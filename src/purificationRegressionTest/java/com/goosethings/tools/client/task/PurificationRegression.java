package com.goosethings.tools.client.task;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.task.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Isolated world: real block packets, Screens and bound results; actual datapack functions for lifecycle checks. */
public final class PurificationRegression implements ClientModInitializer {
    private static final String BASE="ggd:eagleton_simplify/purification/", TASK="ggd:task/eagleton_simplify/purificationlaser/";
    private boolean opened, prepared, finished, requested, shot, verifiedCharge;
    private volatile boolean ready, done;
    private int scenario, wait, assertions, digit;
    private long last, start=System.currentTimeMillis();
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            require(System.currentTimeMillis()-start<240_000,"timeout stage="+scenario);
            if(!opened){opened=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("purification-"+System.currentTimeMillis(),new LevelSettings("Purification regression",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded())return;
            if(!prepared){prepared=true;server(mc,()->prepare(mc));return;}
            if(!ready)return;
            if(scenario==0||scenario==1){
                if(!requested){requested=true;useBlock(mc,scenario==0?-1650:-1649);return;}
                if(mc.gui.screen() instanceof TaskScreen s && s.currentState()!=null && s.currentState().started()){
                    require(s.taskType()==TaskType.PURIFICATION,"exact block did not open console");
                    if(++wait<15)return;capture(mc,"console-"+scenario);s.onClose();next();
                }return;
            }
            if(scenario==2){
                if(!requested){requested=true;server(mc,()->{run(mc,TASK+"accept");require(player(mc).entityTags().contains("inTaskPurificationLaser"),"laser accept failed");});return;}
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.currentState()==null||!s.currentState().started())return;
                if(++wait<15)return;
                if(!shot){shot=true;capture(mc,"laser");s.onClose();next();}return;
            }
            if(scenario==3){
                if(requested)server(mc,()->{if(player(mc).entityTags().contains("task.purificationlaser.finished"))done=true;});
                if(done){next();return;}
                if(!requested){requested=true;server(mc,()->{require(!player(mc).entityTags().contains("task.purificationlaser.finished"),"close awarded unlock");run(mc,TASK+"accept");});return;}
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.currentState()==null||!s.currentState().started())return;
                if(System.currentTimeMillis()-last<180)return;
                var layout=new PurificationLaserLayout(screenSeed(s));int bits=s.currentState().pipeBits(),solution=0;while(!layout.trace(solution).returned())solution++;
                for(int i=0;i<3;i++)if(((bits^solution)&(1<<i))!=0){var p=layout.mirrors[i];click(s,p.x(),p.y());last=System.currentTimeMillis();return;}
                server(mc,()->{if(player(mc).entityTags().contains("task.purificationlaser.finished"))done=true;});
                if(done){next();}return;
            }
            if(scenario==4){
                if(!requested){requested=true;server(mc,()->{checks(mc);done=true;});return;}
                if(done){next();}return;
            }
            if(scenario==5){
                if(requested&&wait>0)server(mc,()->{if(!player(mc).entityTags().contains("inTaskPurificationConsole")){require(shot&&digit==4&&wait>30,"console activation sequence skipped");verifiedCharge=true;done=true;}});
                if(!requested){requested=true;useBlock(mc,-1650);return;}
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.taskType()!=TaskType.PURIFICATION||s.currentState()==null||!s.currentState().started()){
                    if(done){next();}return;
                }
                if(System.currentTimeMillis()-last<160)return;
                wait++;
                var state=s.currentState();
                if(state.stage()==0){
                    if(digit<4){String code=String.format(Locale.ROOT,"%04d",PurificationLayout.password(screenSeed(s)));int key=48+code.charAt(digit++)-'0';s.keyPressed(new KeyEvent(key,key,0));}
                    else{var box=PurificationLayout.key(11);click(s,box.x()+5,box.y()+5);}
                }else{var b=PurificationLayout.ACTIVATE;click(s,b.x()+5,b.y()+5);if(!shot&&state.phaseAt()>2000){shot=true;capture(mc,"console-charging");}}
                last=System.currentTimeMillis();return;
            }
            if(scenario==6){
                if(!requested){requested=true;server(mc,()->{require(verifiedCharge,"console activation sequence skipped");require(!player(mc).entityTags().contains("inTaskPurificationConsole"),"successful console not closed");run(mc,BASE+"cleanup");require(get(mc,"#Live")==0&&!player(mc).entityTags().contains("task.purificationlaser.finished"),"cleanup left state");done=true;});return;}
                if(done)finish(mc,"PASS "+assertions+" checks: exact two block packets, real console/laser mouse and keyboard input, close/retry and bound unlock, 1200+1200 tick phases, meeting gates, bomb/Cupid cleanup, exact chamber feet bounds, navigation, inventory.14, successful activation and cleanup; four GPU screenshots.");
            }
        }catch(Throwable e){GooseTools.LOGGER.error("Purification regression",e);finish(mc,"FAIL "+e);}
    }
    private void next(){scenario++;requested=false;done=false;shot=false;wait=0;}
    private void prepare(Minecraft mc)throws Exception{
        var server=mc.getSingleplayerServer();Path root=Path.of(System.getProperty("purification.fixtureRoot"));Path source=root.resolve("saves/Whoiskiller/datapacks/Whoiskiller"),dest=server.getWorldPath(LevelResource.ROOT).resolve("datapacks/purification-test");
        try(var stream=Files.walk(source)){for(Path p:stream.filter(Files::isRegularFile).toList()){Path q=dest.resolve(source.relativize(p));Files.createDirectories(q.getParent());Files.copy(p,q);}}
        Path tags=dest.resolve("data/minecraft/tags/function");Files.writeString(tags.resolve("load.json"),"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\",\"ggd:eagleton_simplify/purification/load\"]}");Files.writeString(tags.resolve("tick.json"),"{\"values\":[\"ggd:eagleton_simplify/purification/tick\"]}");
        server.getPackRepository().reload();var packs=new ArrayList<>(server.getPackRepository().getSelectedIds());packs.add("file/purification-test");
        server.reloadResources(packs).whenComplete((v,e)->server.execute(()->{try{
            if(e!=null)throw new IllegalStateException(e);
            for(String o:List.of("gamesetting","ggdSession","ggdadv","ggdId","ggdRoleSetting","ggd_temp","ggdObjective","ggdMath","ggdTaskGiven","ggdTasksDone","TaskLimit","carrotrightclick","ggdBombPassCD","ggdCupidCD","ggdCupidDelayLeft","ggdCupidLinkTimer"))exec(server,"scoreboard objectives add "+o+" dummy");
            exec(server,"scoreboard players set map gamesetting 11");exec(server,"scoreboard players set FullBloodDLC ggdadv 1");exec(server,"scoreboard players set #MeetingPhase ggdSession 0");
            var p=player(mc);p.addTag("players");p.addTag("gamingGGD");p.addTag("evil");p.addTag("inTutorial");p.addTag("ggdSkipInvChanged");
            exec(server,"scoreboard players set "+name(mc)+" ggdId 101");exec(server,"forceload add -1664 -576 -1632 -544");
            for(int x=-1664;x<=-1632;x+=16)for(int z=-576;z<=-544;z+=16)p.level().getChunk(x>>4,z>>4);
            exec(server,"fill -1653 70 -561 -1646 70 -544 stone");exec(server,"setblock -1650 72 -547 stone");exec(server,"setblock -1649 72 -547 stone");
            exec(server,"tp "+name(mc)+" -1649.5 71 -545");run(mc,BASE+"init");ready=true;
        }catch(Throwable f){mc.execute(()->finish(mc,"FAIL prepare "+f));}}));
    }
    private void checks(Minecraft mc){
        var p=player(mc);var s=mc.getSingleplayerServer();require(p.entityTags().contains("task.purificationlaser.finished"),"real laser not bound");require(has(p,"purificationDisable"),"unlock missing");require(p.getInventory().getItem(23).getItem()==net.minecraft.world.item.Items.CARROT_ON_A_STICK,"inventory.14 missing");
        run(mc,TASK+"trigger");require(get(mc,"#Disabled")==1200&&get(mc,"#CD")==0,"disable not 1200");run(mc,TASK+"trigger");require(get(mc,"#Disabled")==1200,"duplicate reset");
        for(int i=0;i<1199;i++)run(mc,BASE+"world_tick");require(get(mc,"#Disabled")==1&&get(mc,"#CD")==0,"early restore");run(mc,BASE+"world_tick");require(get(mc,"#Disabled")==0&&get(mc,"#CD")==1200,"serial cooldown start");
        run(mc,TASK+"trigger");require(get(mc,"#Disabled")==0,"cooldown bypass");exec(s,"scoreboard players set #MeetingPhase ggdSession 1");run(mc,BASE+"world_tick");require(get(mc,"#CD")==1199,"meeting clock stopped");run(mc,TASK+"trigger");require(get(mc,"#Disabled")==0,"meeting sabotage");exec(s,"scoreboard players set #MeetingPhase ggdSession 0");
        for(int i=0;i<1198;i++)run(mc,BASE+"world_tick");require(get(mc,"#CD")==1&&!has(p,"purificationDisable"),"early ready");run(mc,BASE+"world_tick");require(get(mc,"#CD")==0&&has(p,"purificationDisable"),"cooldown did not finish");
        p.addTag("gotBomb");p.addTag("bombPassLocked");p.addTag("cupidMarked");p.addTag("cupidLinking");p.addTag("cupidLinkPending");run(mc,BASE+"clean_status");require(!p.entityTags().contains("gotBomb")&&!p.entityTags().contains("bombPassLocked"),"bomb not removed");require(!p.entityTags().contains("cupidMarked")&&!p.entityTags().contains("cupidLinking")&&!p.entityTags().contains("cupidLinkPending"),"chain not removed");
        double[][] points={{-1650,71,-560},{-1648.001,76.999,-558.001},{-1648,72,-559},{-1649,77,-559},{-1649,72,-558},{-1650.001,72,-559},{-1649,70.999,-559},{-1649,72,-560.001}};
        for(int i=0;i<points.length;i++){double[] xyz=points[i];exec(s,"tp "+name(mc)+" "+xyz[0]+" "+xyz[1]+" "+xyz[2]);exec(s,"execute as "+name(mc)+" at @s store success score #Boundary ggdPurify if predicate ggd:purification_chamber");require((get(mc,"#Boundary")==1)==(i<2),"chamber boundary "+i);}
        exec(s,"tp "+name(mc)+" -1649.5 71 -545");exec(s,"execute as "+name(mc)+" at @s run function ggd:task/nav_start {task:\"purificationlaser\",name_key:\"item.task.purificationlaser.available\"}");require(p.entityTags().contains("navigating_to_purificationlaser"),"navigation cleared itself");p.removeTag("navigating_to_purificationlaser");p.removeTag("inTask");
    }
    private static long screenSeed(TaskScreen s)throws Exception{var f=TaskScreen.class.getDeclaredField("open");f.setAccessible(true);return ((TaskPackets.Open)f.get(s)).seed();}
    private static void useBlock(Minecraft mc,int x){var p=new BlockPos(x,72,-547);mc.player.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.SOUTH,p,false),0));}
    private static MouseButtonEvent mouse(TaskScreen s,double x,double y){double z=Math.max(.1,Math.min(1.5,Math.min((s.width-24.)/420,(s.height-24.)/320)));return new MouseButtonEvent((s.width-420*z)/2+x*z,(s.height-320*z)/2+y*z,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static void click(TaskScreen s,double x,double y){s.mouseClicked(mouse(s,x,y),false);s.mouseReleased(mouse(s,x,y));}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static void exec(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),command);}
    private static void run(Minecraft mc,String fn){exec(mc.getSingleplayerServer(),"execute as "+name(mc)+" at @s run function "+fn);}
    private static int get(Minecraft mc,String holder){var sb=mc.getSingleplayerServer().getScoreboard();var o=sb.getObjective("ggdPurify");var v=o==null?null:sb.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(holder),o);return v==null?0:v.value();}
    private static boolean has(ServerPlayer p,String key){return p.getInventory().getNonEquipmentItems().stream().anyMatch(i->{var d=i.get(DataComponents.CUSTOM_DATA);return d!=null&&d.copyTag().getBoolean(key).orElse(false);});}
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private void server(Minecraft mc,Action a){int stage=scenario;mc.getSingleplayerServer().execute(()->{if(scenario!=stage)return;try{a.run();}catch(Throwable e){GooseTools.LOGGER.error("Purification server checks",e);mc.execute(()->finish(mc,"FAIL server "+e));}});}
    private void require(boolean result,String message){assertions++;if(!result)throw new IllegalStateException(message);}
    private static void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){GooseTools.LOGGER.error("Screenshot",e);}});}
    private void finish(Minecraft mc,String result){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception e){GooseTools.LOGGER.error("Result",e);}mc.stop();}
}
