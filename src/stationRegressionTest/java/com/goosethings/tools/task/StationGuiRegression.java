package com.goosethings.tools.task;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.task.*;
import com.goosethings.tools.client.game.GameScreen;
import com.goosethings.tools.client.input.*;
import com.goosethings.tools.client.input.mixin.ClientInputAccessor;
import com.goosethings.tools.game.GameType;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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

/** Exercises real client handlers and packets in an isolated save; not included in the runtime JAR. */
public final class StationGuiRegression implements ClientModInitializer {
    private boolean world,language,languageReady,finished,requested,baseline,expectInvMove;
    private int ticks,probe,step,stageTicks,captures,phaseKnob,compMask,assignIndex,loggedStep=-1;
    private long since,lastAction,lastEvent=-1;
    private int[] solution;
    private Object invMove;
    private java.lang.reflect.Method allows,update;
    private final Map<String,Long> pending=new HashMap<>();
    private final Set<String> captured=new HashSet<>();
    @Override public void onInitializeClient(){if(Boolean.getBoolean("goosetools.stationRegressionTest"))ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private static long now(){return System.nanoTime()/1_000_000;}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            if(!world){
                if(!language){language=true;mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks().whenComplete((v,e)->mc.execute(()->{if(e!=null)finish(mc,"FAIL language "+e);else languageReady=true;}));return;}
                if(!languageReady)return;world=true;mc.options.pauseOnLostFocus=false;mc.getWindow().setWindowed(1280,1000);
                mc.createWorldOpenFlows().createFreshLevel("station-"+System.currentTimeMillis(),new LevelSettings("Station regression",net.minecraft.world.level.GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||!mc.player.connection.hasClientLoaded()||mc.getSingleplayerServer()==null||++ticks<40)return;
            if(since==0){since=now();expectInvMove=Boolean.getBoolean("goosetools.stationWithInvMove");require(FabricLoader.getInstance().isModLoaded("invmove")==expectInvMove,"InvMove fixture absent/present mismatch");
                if(expectInvMove){Class<?> c=Class.forName("me.pieking1215.invmove.InvMove");invMove=c.getMethod("instance").invoke(null);allows=c.getMethod("allowMovementInScreen",Screen.class);update=c.getMethod("onInputUpdate",net.minecraft.client.player.ClientInput.class);baseline=(boolean)allows.invoke(invMove,new InventoryScreen(mc.player));require(baseline,"InvMove inventory baseline disabled");}}
            require(now()-since<200_000,"timeout probe="+probe+" step="+step);
            int all=TaskType.values().length+GameType.values().length;
            if(probe<all){
                if(!requested){requested=true;stageTicks=0;String id=probe<TaskType.values().length?TaskType.values()[probe].id:GameType.values()[probe-TaskType.values().length].id;command(mc,"goosetools "+(probe<TaskType.values().length?"tasks":"games")+" open "+mc.player.getName().getString()+" "+id);return;}
                Screen gui=mc.gui.screen();boolean matching=probe<TaskType.values().length?(probe==TaskType.POWERSTATION.ordinal()?gui instanceof PowerStationScreen:gui instanceof TaskScreen t&&t.taskType()==TaskType.values()[probe]):gui instanceof GameScreen g&&g.gameType()==GameType.values()[probe-TaskType.values().length];
                if(!matching||++stageTicks<8)return;guard(mc);gui.onClose();restore(mc);probe++;requested=false;return;
            }
            if(!requested){requested=true;mc.options.guiScale().set(2);mc.resizeGui();command(mc,"goosetools tasks open "+mc.player.getName().getString()+" powerstation");return;}
            if(!(mc.gui.screen() instanceof PowerStationScreen s)||s.currentState()==null)return;
            require(!s.isPauseScreen(),"station paused world");guard(mc);
            PowerStationSnapshot state=s.currentState();
            if(loggedStep!=step){loggedStep=step;GooseTools.LOGGER.info("Station test step={} stage={} feedback={} event={}",step,state.stage(),state.feedback(),state.event());}
            if(step==0){if(!capture(mc,"calculate"))return;click(s,512,24);step=1;return;}
            if(step==1){require(s.handbookOpen(),"handbook did not open");if(!capture(mc,"handbook"))return;if(expectInvMove)disabledConfig(mc);key(s,InputConstants.KEY_ESCAPE,0);step=2;return;}
            if(step==2){require(!s.handbookOpen(),"Escape closed whole station");type(s,0,"1");type(s,1,"1");key(s,InputConstants.KEY_RETURN,0);step=3;return;}
            if(step==3){if(state.feedback()!=PowerStationSession.WRONG_CURRENT){capture(mc,"input-pending");return;}require(state.errors()==1&&state.stage()==0,"wrong calculation advanced");
                double apparent=state.power()/state.initialPf(),current=apparent*1000/(Math.sqrt(3)*state.voltage());type(s,0,String.format(Locale.ROOT,"%.3f",current));type(s,1,String.format(Locale.ROOT,"%.3f",apparent));int cap=0;while(PowerStationSession.CAPACITIES[cap]<apparent*1.15)cap++;click(s,258+67*cap,275);key(s,InputConstants.KEY_RETURN,0);step=4;return;}
            if(step==4){if(state.stage()!=1){capture(mc,"calculation-rejected");return;}if(!capture(mc,"compensate"))return;compMask=0;while(!validComp(state,compMask))compMask++;step=5;return;}
            if(step==5){if(state.capacitors()!=compMask){if(!canAct(state))return;int bit=Integer.numberOfTrailingZeros(state.capacitors()^compMask);click(s,267+82*bit,180);return;}key(s,InputConstants.KEY_RETURN,0);step=6;return;}
            if(step==6){if(state.stage()!=2)return;key(s,InputConstants.KEY_RETURN,0);step=7;return;}
            if(step==7){if(state.feedback()!=PowerStationSession.WRONG_SEQUENCE)return;if(!capture(mc,"interlock"))return;click(s,292,320);step=8;return;}
            if(step==8){if(state.phaseSequence()!=0)return;if(Math.abs(state.generatorVoltage()-state.voltage())>1){if(!canAct(state))return;click(s,302,199);key(s,state.generatorVoltage()<state.voltage()?InputConstants.KEY_RIGHT:InputConstants.KEY_LEFT,Math.abs(state.generatorVoltage()-state.voltage())>=5?1:0);return;}
                click(s,488,199);step=9;lastEvent=-1;return;}
            if(step==9){if(Math.abs(state.frequency()-50.1)>.005){if(!canAct(state))return;key(s,state.frequency()<50.1?InputConstants.KEY_RIGHT:InputConstants.KEY_LEFT,Math.abs(state.frequency()-50.1)>=.095?1:0);return;}step=10;return;}
            if(step==10){if(!capture(mc,"synchronize"))return;if(Math.abs(state.phase())<3){key(s,InputConstants.KEY_LEFT,1);step=11;}return;}
            if(step==11){if(Math.abs(state.frequency()-50)>.005){if(!canAct(state))return;key(s,state.frequency()>50?InputConstants.KEY_LEFT:InputConstants.KEY_RIGHT,0);return;}if(state.stable()<400)return;key(s,InputConstants.KEY_RETURN,0);step=12;return;}
            if(step==12){if(state.stage()!=3)return;if(!capture(mc,"distribute"))return;key(s,InputConstants.KEY_RETURN,0);step=13;return;}
            if(step==13){if(state.feedback()!=PowerStationSession.NOT_CONNECTED)return;solution=solve(state);require(solution!=null,"visible loads have no solution");assignIndex=0;step=14;return;}
            if(step==14){if(assignIndex==6){key(s,InputConstants.KEY_RETURN,0);step=15;return;}if(state.assignments()[assignIndex]==solution[assignIndex]){assignIndex++;return;}if(!canAct(state))return;
                int x=228+assignIndex%2*170,y=103+assignIndex/2*64;
                if(assignIndex==0){click(s,x+60,y+12);s.mouseReleased(event(s,100,152+solution[assignIndex]*58));}else click(s,x+28+49*solution[assignIndex],y+39);return;}
            if(step==15){if(state.stage()!=4)return;require(state.errors()==3,"expected calculation/sequence/missing-load errors only, actual="+state.errors());if(!capture(mc,"complete"))return;
                mc.options.guiScale().set(3);mc.resizeGui();step=16;return;}
            if(step==16){if(!capture(mc,"scale3"))return;mc.options.guiScale().set(1);mc.resizeGui();step=17;return;}
            if(step==17){if(!capture(mc,"scale1"))return;click(s,300,244);step=18;return;}
            if(step==18){if(s.currentState().stage()!=0)return;require(s.currentState().errors()==0,"replay retained errors");key(s,InputConstants.KEY_ESCAPE,0);restore(mc);finish(mc,"PASS four station stages via real commands, packets, character input, Enter, arrows, Shift, buttons and card drag; wrong calculation/sequence/missing load interlocks; continuous synchronization and stable supply; handbook; replay; Chinese GUI scales 1/2/3; all 17 task/game screens block movement; InvMove="+expectInvMove+"; inventory restoration; "+captures+" GPU screenshots");}
        }catch(Throwable e){GooseTools.LOGGER.error("Station regression failed",e);finish(mc,"FAIL "+e);}
    }
    private boolean canAct(PowerStationSnapshot s){if(now()-lastAction<85||lastEvent==s.event())return false;lastAction=now();lastEvent=s.event();return true;}
    private void guard(Minecraft mc)throws Exception{
        require(GuiMovementGuard.blocks(mc.gui.screen()),"missing protected screen");
        for(var k:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint))k.setDown(true);
        mc.player.input.tick();require(mc.player.input.keyPresses.equals(Input.EMPTY),"vanilla movement leaked");
        require(mc.player.input.getMoveVector().equals(Vec2.ZERO),"vanilla movement vector leaked");
        if(expectInvMove){require(!(boolean)allows.invoke(invMove,mc.gui.screen()),"InvMove still allowed");mc.player.input.keyPresses=new Input(true,false,true,false,true,true,true);((ClientInputAccessor)mc.player.input).goosetools$setMoveVector(new Vec2(1,1));update.invoke(invMove,mc.player.input);require(mc.player.input.keyPresses.equals(Input.EMPTY)&&mc.player.input.getMoveVector().equals(Vec2.ZERO),"InvMove late overwrite leaked");}
        for(var k:List.of(mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyJump,mc.options.keyShift,mc.options.keySprint))k.setDown(false);
    }
    private void restore(Minecraft mc)throws Exception{InventoryScreen inventory=new InventoryScreen(mc.player);mc.setScreenAndShow(inventory);require(!GuiMovementGuard.blocks(inventory),"inventory remained guarded");if(expectInvMove)require((boolean)allows.invoke(invMove,inventory)==baseline,"InvMove did not restore");mc.setScreenAndShow(null);}
    private void disabledConfig(Minecraft mc)throws Exception{
        Object general=Class.forName("me.pieking1215.invmove.InvMoveConfig").getField("GENERAL").get(null);
        Object enabled=general.getClass().getField("ENABLED").get(general);var get=enabled.getClass().getMethod("get");var set=enabled.getClass().getMethod("set",Object.class);Object original=get.invoke(enabled);
        try{set.invoke(enabled,false);guard(mc);require(!(boolean)allows.invoke(invMove,new InventoryScreen(mc.player)),"disabled InvMove became enabled");require(Boolean.FALSE.equals(get.invoke(enabled)),"guard changed global config");}finally{set.invoke(enabled,original);}
    }
    private static boolean validComp(PowerStationSnapshot s,int mask){double q=s.power()*PowerStationSession.tan(s.initialPf())-mask*s.capacitorUnit(),pf=s.power()/Math.hypot(s.power(),q);return q>=0&&pf>=.95&&pf<=.985;}
    private static int[] solve(PowerStationSnapshot s){double[] ps=s.loadP(),qs=s.loadQ();for(int code=0;code<729;code++){int n=code;int[] as=new int[6];double[] p=new double[3],q=new double[3],a=new double[3];for(int i=0;i<6;i++){as[i]=n%3;n/=3;p[as[i]]+=ps[i];q[as[i]]+=qs[i];}for(int i=0;i<3;i++)a[i]=Math.hypot(p[i],q[i]-s.capacitors()*s.capacitorUnit()/3)*1000/(s.voltage()/Math.sqrt(3));double mean=Arrays.stream(a).average().orElse(0);if(Arrays.stream(a).allMatch(v->v<=s.phaseLimit()&&Math.abs(v-mean)<=mean*.10))return as;}return null;}
    private static void type(PowerStationScreen s,int field,String value)throws Exception{click(s,300,field==0?170:220);key(s,InputConstants.KEY_A,64);for(char c:value.toCharArray())s.charTyped(new CharacterEvent(c));var f=PowerStationScreen.class.getDeclaredField(field==0?"current":"apparent");f.setAccessible(true);var box=(net.minecraft.client.gui.components.EditBox)f.get(s);require(box.getValue().equals(value),"numeric field rejected characters: focused="+box.isFocused()+" actual="+box.getValue()+" expected="+value);}
    private static MouseButtonEvent event(PowerStationScreen s,double x,double y){var t=s.transform();return new MouseButtonEvent(t.x()+x*t.scale(),t.y()+y*t.scale(),new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static void click(PowerStationScreen s,double x,double y){s.mouseClicked(event(s,x,y),false);}
    private static void key(PowerStationScreen s,int key,int modifiers){KeyEvent e=new KeyEvent(key,key==InputConstants.KEY_A?'a':0,modifiers);s.keyPressed(e);s.keyReleased(e);}
    private static void command(Minecraft mc,String cmd){mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),cmd);});}
    private boolean capture(Minecraft mc,String name){if(captured.contains(name))return true;if(now()-pending.computeIfAbsent(name,k->now())<350)return false;captured.add(name);Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve("station-"+name+".png"));captures++;}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot "+e));}});return true;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception e){GooseTools.LOGGER.error("Station result",e);}mc.stop();}
}
