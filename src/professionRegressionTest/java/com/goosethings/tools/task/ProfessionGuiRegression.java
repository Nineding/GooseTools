package com.goosethings.tools.task;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.task.profession.*;
import com.goosethings.tools.client.task.ProfessionScreen;
import com.goosethings.tools.client.input.*;
import com.goosethings.tools.client.input.mixin.ClientInputAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec2;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Test-only client: uses rendered hit regions and real handlers, commands and packets. */
public final class ProfessionGuiRegression implements ClientModInitializer {
    private static final TaskType[] TYPES={TaskType.TELECOM,TaskType.NUCLEAR,TaskType.FOODSAFETY,TaskType.CIVIL};
    private final Queue<Step> steps=new ArrayDeque<>();
    private final Map<TaskType,Integer> completions=new ConcurrentHashMap<>();
    private final Map<String,Long> pendingCaptures=new HashMap<>();
    private final Set<String> captures=new HashSet<>();
    private boolean language,languageReady,world,finished,requested,withInvMove;
    private int ticks,scenario,builtStage=-1,cleanup;
    private long since,stepAt,lastSession;
    private Object invMove;
    private java.lang.reflect.Method allows,update;
    private Step running;
    private record Step(Consumer<ProfessionScreen> action,Predicate<ProfessionScreen> done){}
    private static long now(){return System.nanoTime()/1_000_000;}
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("goosetools.professionRegressionTest"))return;
        TaskServer.COMPLETED.register((player,type,time)->completions.merge(type,1,Integer::sum));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            if(!world){
                if(!language){language=true;mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks().whenComplete((v,e)->mc.execute(()->{if(e!=null)finish(mc,"FAIL language "+e);else languageReady=true;}));return;}
                if(!languageReady)return;world=true;mc.options.pauseOnLostFocus=false;mc.getWindow().setWindowed(1400,1100);
                mc.createWorldOpenFlows().createFreshLevel("professions-"+System.currentTimeMillis(),new LevelSettings("Profession regression",net.minecraft.world.level.GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||!mc.player.connection.hasClientLoaded()||mc.getSingleplayerServer()==null||++ticks<40)return;
            if(since==0){since=now();withInvMove=Boolean.getBoolean("goosetools.professionWithInvMove");require(FabricLoader.getInstance().isModLoaded("invmove")==withInvMove,"InvMove fixture mismatch");
                if(withInvMove){Class<?> c=Class.forName("me.pieking1215.invmove.InvMove");invMove=c.getMethod("instance").invoke(null);allows=c.getMethod("allowMovementInScreen",Screen.class);update=c.getMethod("onInputUpdate",net.minecraft.client.player.ClientInput.class);require((boolean)allows.invoke(invMove,new InventoryScreen(mc.player)),"inventory baseline disabled");}}
            require(now()-since<340_000,"timeout scenario="+scenario+" stage="+builtStage+" cleanup="+cleanup);
            if(scenario>=TYPES.length){cleanup(mc);return;}
            if(!requested){requested=true;builtStage=-1;command(mc,"goosetools tasks open "+target(mc)+" "+TYPES[scenario].id);return;}
            if(!(mc.gui.screen() instanceof ProfessionScreen s)||s.taskType()!=TYPES[scenario]||s.currentState()==null)return;
            guard(mc);require(!s.isPauseScreen(),"world paused");require(s.getTitle().getString().contains("专业"),"Chinese title missing");
            if(running!=null){
                require(now()-stepAt<12_000,"operation failed to acknowledge, stage="+s.currentState().stage()+" feedback="+s.currentState().feedback());
                if(running.done.test(s)&&now()-stepAt>=110){running=null;}return;
            }
            if(!steps.isEmpty()){running=steps.remove();stepAt=now();running.action.accept(s);return;}
            if(builtStage==-2){if(s.sessionId()==lastSession)return;
                require(s.currentState().stage()==0&&s.currentState().errors()==0,"replay dirty");key(s,InputConstants.KEY_ESCAPE,0);restore(mc);scenario++;requested=false;builtStage=-1;return;}
            if(s.currentState().stage()!=builtStage){builtStage=s.currentState().stage();GooseTools.LOGGER.info("Profession test type={} stage={}",s.taskType(),builtStage);
                build(mc,s);return;}
            if(builtStage==4){require(completions.getOrDefault(s.taskType(),0)==1,"completion not exactly once");
                lastSession=s.sessionId();click(s,350,253);builtStage=-2;return;}
        }catch(Throwable e){GooseTools.LOGGER.error("Profession regression failed",e);finish(mc,"FAIL "+e);}
    }
    private void build(Minecraft mc,ProfessionScreen s){
        int stage=s.currentState().stage();String tag=s.taskType().id+"-"+stage;
        for(int scale:new int[]{1,2,3}){
            steps.add(new Step(screen->{mc.options.guiScale().set(scale);mc.resizeGui();},screen->capture(mc,tag+"-scale"+scale)));
        }
        steps.add(new Step(screen->{mc.options.guiScale().set(2);mc.resizeGui();},screen->true));
        if(stage==4)return;
        steps.add(new Step(screen->click(screen,600,27),ProfessionScreen::handbookOpen));
        steps.add(new Step(screen->{},screen->capture(mc,tag+"-book")));
        steps.add(new Step(screen->key(screen,InputConstants.KEY_ESCAPE,0),screen->!screen.handbookOpen()));
        if(withInvMove&&stage==0)steps.add(new Step(screen->{try{disabledConfig(mc);}catch(Exception e){throw new RuntimeException(e);}},screen->true));
        switch(s.taskType()){case TELECOM->telecom(s,stage);case NUCLEAR->nuclear(s,stage);case FOODSAFETY->food(s,stage);case CIVIL->civil(s,stage);default->throw new IllegalStateException();}
        int expected=stage+1;steps.add(new Step(screen->key(screen,InputConstants.KEY_RETURN,0),screen->screen.currentState().stage()==expected));
    }
    private void telecom(ProfessionScreen s,int stage){TelecomModel m=(TelecomModel)s.questionModel();switch(stage){
        case 0->{int tx=-1,atten=-1;double rx=0;outer:for(int i=0;i<3;i++)for(int j=0;j<4;j++){double pr=TelecomModel.TX_DBM[i]-.25*m.lengthKm-1-.4-TelecomModel.ATTENUATION_DB[j];if(pr>=-23&&pr<=-13){tx=i;atten=j;rx=pr;break outer;}}
            choose(0,tx);choose(1,atten);numbers(1,1);reject(ProfessionFeedback.CALC_A);numbers(rx,rx+26);}
        case 1->{double capacity=5*Math.log(1+Math.pow(10,m.snrDb/10))/Math.log(2);int mod=-1,fec=-1,rate=-1;double net=0;
            outer:for(int a=0;a<2;a++)for(int b=0;b<2;b++)for(int c=0;c<4;c++){double value=(c+1)*(a==0?2:4)*(b==0?.5:.75)*.9;
                if(m.snrDb>=(a==0?6:14)&&value>=m.demandMbps&&value<=capacity*.8&&(c+1)*1.25<=5){mod=a;fec=b;rate=c;net=value;break outer;}}
            require(mod>=0,"no modulation solution");choose(2,mod);choose(3,fec);choose(4,rate);numbers(capacity,net);}
        case 2->{dial(0,-m.rawPhase-10);steps.add(new Step(screen->{selectDial(screen,0);key(screen,InputConstants.KEY_RIGHT,1);},screen->Math.abs(screen.currentState().dials()[0]+m.rawPhase)<1e-6));
            dial(1,1/m.rawGain);dial(2,-m.rawI);dial(3,-m.rawQ);reject(ProfessionFeedback.HOLD);steps.add(new Step(screen->{},screen->screen.currentState().stable()>=800));}
        case 3->{int[] answer=null;for(int code=0;code<4096;code++){int n=code;int[] p=new int[6];for(int i=0;i<6;i++){p[i]=n%4;n/=4;}boolean valid=true;for(int[] e:m.edges())if(Math.abs(p[e[0]]-p[e[1]])<2){valid=false;break;}if(valid){answer=p;break;}}
            require(answer!=null,"no frequency solution");drag(-1,answer[0],5,-1);for(int i=1;i<6;i++)choose(5+i,answer[i]);}
        default->{}
    }}
    private void nuclear(ProfessionScreen s,int stage){NuclearModel m=(NuclearModel)s.questionModel();switch(stage){
        case 0->{int[] parts=m.components();for(int i=0;i<3;i++)choose(i,parts[i]);choose(3,1);choose(4,0);numbers(1);reject(ProfessionFeedback.CALC_A);numbers(m.neutronsProduced/1000);}
        case 1->{double heat=m.initialMW/Math.pow(1+m.timeMinutes/60,.2),flow=heat*1000/(m.cp*m.deltaT);int pump=0;while(NuclearModel.PUMP_FLOW[pump]<flow*1.1)pump++;choose(5,pump);numbers(heat,flow);}
        case 2->{choose(6,0);choose(7,1);choose(8,0);choose(9,1);choose(10,1);choose(11,1);double flow=Math.ceil(m.requiredFlow()*1.1/5)*5;dial(0,flow);dial(1,flow);}
        case 3->{choose(12,2);choose(13,2);dial(2,m.requiredMinutes);double dose=0;double[] distances={3,3.5,4},weights={.5,.3,.2};for(int i=0;i<3;i++)dose+=m.rateAt1m/(distances[i]*distances[i])*m.requiredMinutes*weights[i]/60*.2;numbers(dose);}
        default->{}
    }}
    private void food(ProfessionScreen s,int stage){FoodSafetyModel m=(FoodSafetyModel)s.questionModel();switch(stage){
        case 0->{int[] kinds=m.hazardKinds(),values={1,2,3,0};for(int i=0;i<4;i++)choose(i,values[kinds[i]]);choose(0,(values[kinds[0]]+1)%4);reject(ProfessionFeedback.HAZARD);drag(0,-1,-1,values[kinds[0]]);}
        case 1->{double hold=6*m.dReference;dial(0,70);dial(1,hold);double f=hold;for(double t:new double[]{50,60,64,68})f+=.5*Math.pow(10,(t-70)/m.z);numbers(f,hold);}
        case 2->{int[] kinds=m.toolKinds(),zones={0,1,2,0};drag(4,-1,-1,zones[kinds[0]]);for(int i=1;i<4;i++)choose(4+i,zones[kinds[i]]);choose(8,1);choose(9,1);}
        case 3->{int[] lots=m.lotMasks(),deviations=m.deviations();for(int i=0;i<6;i++)choose(10+i,deviations[i]==2?2:(lots[i]&(1<<m.badLot))!=0?1:deviations[i]==1?3:0);}
        default->{}
    }}
    private void civil(ProfessionScreen s,int stage){CivilModel m=(CivilModel)s.questionModel();switch(stage){
        case 0->{double[] bs=m.backsights(),fs=m.foresights();double err=0;for(int i=0;i<3;i++)err+=bs[i]-fs[i];double hi=m.initialLevel+bs[0];choose(0,1);numbers(1,1,0);reject(ProfessionFeedback.CALC_A);numbers(hi,hi-fs[0]-err/3,err);}
        case 1->{double r=m.qKNm*m.lengthM/2,mm=m.qKNm*m.lengthM*m.lengthM/8;steps.add(new Step(screen->{
                click(screen,47,230);MouseButtonEvent target=event(screen,47,230-r/600*23);screen.mouseDragged(target,0,-r/600*23);screen.mouseReleased(target);
            },screen->Math.abs(screen.currentState().dials()[1]-r)<.001));dial(2,-r);dial(3,mm);numbers(r,mm);}
        case 2->{int section=-1;double sigma=0,delta=0;for(int i=0;i<4;i++){double a=m.qKNm*m.lengthM*m.lengthM/8*1000/CivilModel.MODULUS[i]/1e6,b=5*m.qKNm*1000*Math.pow(m.lengthM,4)/(384*200e9*CivilModel.INERTIA[i])*1000;if(a<=160&&b<=m.lengthM/250*1000){section=i;sigma=a;delta=b;break;}}require(section>=0,"no section");choose(1,section);numbers(sigma,delta);}
        case 3->{double fine=m.sandSSD/(1+m.sandAbsorption)*(1+m.sandMoisture),coarse=m.stoneSSD/(1+m.stoneAbsorption)*(1+m.stoneMoisture),water=m.binderKg*.45-(fine-m.sandSSD)-(coarse-m.stoneSSD);double[] strength=m.reportStrength(),cure=m.reportCuring();int index=0;while(strength[index]<30||cure[index]<7)index++;choose(2,index);numbers(fine,coarse,water);}
        default->{}
    }}
    private void choose(int slot,int value){require(value>=0,"invalid solution choice");steps.add(new Step(s->{Object h=find(s,"choices",o->integer(o,"slot")==slot&&integer(o,"value")==value);click(s,integer(h,"x")+integer(h,"w")/2.,integer(h,"y")+integer(h,"h")/2.);},s->s.currentState().choices()[slot]==value));}
    private void dial(int slot,double value){final double[] accepted={0};steps.add(new Step(s->{Object h=find(s,"dials",o->integer(o,"slot")==slot);double min=decimal(h,"min"),max=decimal(h,"max"),step=decimal(h,"step");accepted[0]=Math.clamp(Math.rint(value/step)*step,min,max);double x=integer(h,"x")+34+(accepted[0]-min)/(max-min)*(integer(h,"width")-68),y=integer(h,"y")+32;
        click(s,x,y);s.mouseDragged(event(s,x,y),0,0);s.mouseReleased(event(s,x,y));},s->Math.abs(s.currentState().dials()[slot]-accepted[0])<1e-6));}
    private void drag(int sourceSlot,int sourceValue,int targetSlot,int targetValue){steps.add(new Step(s->{Object card=find(s,"cards",o->integer(o,"slot")==sourceSlot&&integer(o,"value")==sourceValue),drop=find(s,"drops",o->integer(o,"slot")==targetSlot&&integer(o,"value")==targetValue);
        click(s,integer(card,"x")+integer(card,"w")/2.,integer(card,"y")+integer(card,"h")/2.);MouseButtonEvent e=event(s,integer(drop,"x")+integer(drop,"w")/2.,integer(drop,"y")+integer(drop,"h")/2.);s.mouseDragged(e,0,0);s.mouseReleased(e);
    },s->s.currentState().choices()[targetSlot>=0?targetSlot:sourceSlot]==(targetValue>=0?targetValue:sourceValue)));}
    private void numbers(double... values){steps.add(new Step(s->{for(int i=0;i<values.length;i++)type(s,i,String.format(Locale.ROOT,"%.4f",values[i]));},s->true));}
    private void reject(ProfessionFeedback expected){steps.add(new Step(s->key(s,InputConstants.KEY_RETURN,0),s->s.currentState().feedback()==expected.ordinal()));}
    private static void selectDial(ProfessionScreen s,int slot){Object h=find(s,"dials",o->integer(o,"slot")==slot);click(s,integer(h,"x")+60,integer(h,"y")+5);}
    private static Object find(ProfessionScreen s,String field,Predicate<Object> predicate){try{var f=ProfessionScreen.class.getDeclaredField(field);f.setAccessible(true);for(Object o:(List<?>)f.get(s))if(predicate.test(o))return o;throw new IllegalStateException("No rendered "+field+" hit region");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
    private static int integer(Object o,String property){return((Number)property(o,property)).intValue();}private static double decimal(Object o,String property){return((Number)property(o,property)).doubleValue();}
    private static Object property(Object o,String name){try{var m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(o);}catch(Exception e){throw new RuntimeException(e);}}
    private static void type(ProfessionScreen s,int index,String value){click(s,80+index*190,406);key(s,InputConstants.KEY_A,64);for(char c:value.toCharArray())s.charTyped(new CharacterEvent(c));
        try{var f=ProfessionScreen.class.getDeclaredField("fields");f.setAccessible(true);require(((EditBox[])f.get(s))[index].getValue().equals(value),"character input rejected "+value);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
    private static MouseButtonEvent event(ProfessionScreen s,double x,double y){var t=s.transform();return new MouseButtonEvent(t.x()+x*t.scale(),t.y()+y*t.scale(),new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static void click(ProfessionScreen s,double x,double y){s.mouseClicked(event(s,x,y),false);}private static void key(ProfessionScreen s,int code,int modifiers){KeyEvent e=new KeyEvent(code,code==InputConstants.KEY_A?'a':0,modifiers);s.keyPressed(e);s.keyReleased(e);}
    private void guard(Minecraft mc)throws Exception{require(GuiMovementGuard.blocks(mc.gui.screen()),"screen input unprotected");
        for(var k:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint))k.setDown(true);
        mc.player.input.tick();require(mc.player.input.keyPresses.equals(Input.EMPTY)&&mc.player.input.getMoveVector().equals(Vec2.ZERO),"vanilla input leak");
        if(withInvMove){require(!(boolean)allows.invoke(invMove,mc.gui.screen()),"InvMove allowed protected screen");mc.player.input.keyPresses=new Input(true,false,true,false,true,true,true);((ClientInputAccessor)mc.player.input).goosetools$setMoveVector(new Vec2(1,1));update.invoke(invMove,mc.player.input);require(mc.player.input.keyPresses.equals(Input.EMPTY)&&mc.player.input.getMoveVector().equals(Vec2.ZERO),"InvMove late overwrite leaked");}
        for(var k:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint))k.setDown(false);}
    private void disabledConfig(Minecraft mc)throws Exception{Object general=Class.forName("me.pieking1215.invmove.InvMoveConfig").getField("GENERAL").get(null),enabled=general.getClass().getField("ENABLED").get(general);var get=enabled.getClass().getMethod("get");var set=enabled.getClass().getMethod("set",Object.class);Object before=get.invoke(enabled);
        try{set.invoke(enabled,false);guard(mc);require(!(boolean)allows.invoke(invMove,new InventoryScreen(mc.player)),"disabled config was enabled");require(Boolean.FALSE.equals(get.invoke(enabled)),"global config changed");}finally{set.invoke(enabled,before);}}
    private void restore(Minecraft mc)throws Exception{InventoryScreen inventory=new InventoryScreen(mc.player);mc.setScreenAndShow(inventory);require(!GuiMovementGuard.blocks(inventory),"inventory guarded");if(withInvMove)require((boolean)allows.invoke(invMove,inventory),"InvMove did not restore");mc.setScreenAndShow(null);}
    private void cleanup(Minecraft mc){String target=target(mc);switch(cleanup){
        case 0->{command(mc,"goosetools tasks open "+target+" telecom");cleanup++;}
        case 1->{if(!(mc.gui.screen() instanceof ProfessionScreen s)||s.currentState()==null)return;guardUnchecked(mc);command(mc,"goosetools tasks close "+target);cleanup++;}
        case 2->{if(mc.gui.screen() instanceof ProfessionScreen)return;command(mc,"goosetools tasks open "+target+" nuclear");cleanup++;}
        case 3->{if(!(mc.gui.screen() instanceof ProfessionScreen s)||s.taskType()!=TaskType.NUCLEAR)return;lastSession=s.sessionId();command(mc,"goosetools tasks open "+target+" foodsafety");cleanup++;}
        case 4->{if(!(mc.gui.screen() instanceof ProfessionScreen s)||s.taskType()!=TaskType.FOODSAFETY)return;require(s.sessionId()!=lastSession,"replacement retained session");command(mc,"scoreboard objectives add ggdSession dummy");command(mc,"tag "+target+" add gamingGGD");command(mc,"scoreboard players set #MeetingPhase ggdSession 1");cleanup++;}
        case 5->{if(mc.gui.screen() instanceof ProfessionScreen)return;command(mc,"scoreboard players set #MeetingPhase ggdSession 0");command(mc,"tag "+target+" remove gamingGGD");command(mc,"goosetools tasks open "+target+" civil");cleanup++;}
        case 6->{if(!(mc.gui.screen() instanceof ProfessionScreen))return;command(mc,"execute in minecraft:the_nether run tp "+target+" 0 100 0");cleanup++;}
        case 7->{if(mc.gui.screen()!=null||!mc.player.level().dimension().equals(Level.NETHER))return;command(mc,"goosetools tasks open "+target+" civil");cleanup++;}
        case 8->{if(!(mc.gui.screen() instanceof ProfessionScreen))return;command(mc,"kill "+target);cleanup++;}
        case 9->{if(mc.gui.screen() instanceof ProfessionScreen)return;for(TaskType type:TYPES)require(completions.getOrDefault(type,0)==1,"duplicate completion after cleanup");finish(mc,"PASS all 16 professional stages through real commands, packets, character input, Ctrl+A, Enter, Shift/arrows, slider/diagram dragging and card placement; wrong calculations/classification and IQ hold; cooling outage simulation; all handbooks/results/replay; Chinese GUI scales 1/2/3; admin/replacement/meeting/dimension/death cleanup; InvMove="+withInvMove+" late overwrite blocked, inventory restores, disabled config unchanged; "+captures.size()+" GPU screenshots");}
    }}
    private void guardUnchecked(Minecraft mc){try{guard(mc);}catch(Exception e){throw new RuntimeException(e);}}
    private boolean capture(Minecraft mc,String name){if(captures.contains(name))return true;if(now()-pendingCaptures.computeIfAbsent(name,k->now())<400)return false;
        captures.add(name);Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot "+e));}});return true;}
    private static String target(Minecraft mc){return mc.player.getName().getString();}
    private static void command(Minecraft mc,String command){mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String result){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception e){GooseTools.LOGGER.error("Profession result",e);}mc.stop();}
}
