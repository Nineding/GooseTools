package com.goosethings.tools.xaero;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.task.TaskScreen;
import com.goosethings.tools.task.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Isolated real client: actual brush input, network binding and a six-second truck drive. */
public final class KeepGreenRegression implements ClientModInitializer {
    private static final String BASE="ggd:task/eagleton_simplify/keepgreen/";
    private boolean opened,prepared,finished,busy,shot;
    private volatile boolean ready,updated,unlocked,active;
    private volatile int frame,parts,cooldown;
    private int ticks,scenario,assertions,paintTicks,lastShot,readyTicks;
    private long started=System.currentTimeMillis(),since;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            require(System.currentTimeMillis()-started<240_000,"timeout scenario="+scenario);
            if(!opened){opened=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("keepgreen-"+System.currentTimeMillis(),
                    new LevelSettings("Keep Green regression",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++ticks<50)return;
            if(!prepared){prepared=true;mc.getSingleplayerServer().execute(()->prepare(mc));return;}
            if(!ready||++readyTicks<20)return;
            sample(mc);
            if(scenario==0){
                if(!busy){request(mc,()->accept(mc));return;}
                if(updated&&mc.gui.screen() instanceof TaskScreen s&&s.currentState()!=null&&s.currentState().started()){
                    require(s.taskType()==TaskType.KEEPGREEN,"wrong GUI");capture(mc,"keepgreen-empty");s.onClose();next();}return;
            }
            if(scenario==1){
                if(!busy){request(mc,()->{require(!player(mc).entityTags().contains("task.keepgreen.finished"),"closing unlocked sabotage");accept(mc);});return;}
                if(!updated)return;
                if(unlocked){require(!active,"completed GUI still active");next();return;}
                if(mc.gui.screen() instanceof TaskScreen s&&s.currentState()!=null&&s.currentState().started()){
                    if(s.currentState().progress()==1&&!shot){capture(mc,"keepgreen-painted");shot=true;}
                    if(++paintTicks%3==0)paint(s);
                }return;
            }
            if(scenario==2){
                if(!busy){request(mc,()->startTruck(mc));return;}
                if(!updated)return;
                if(frame>=lastShot+25&&frame<=115){lastShot=frame;capture(mc,"truck-frame-"+frame);}
                if(parts==0&&cooldown>0){require(cooldown<=780,"truck did not last exactly 120 ticks");next();}return;
            }
            if(scenario==3){
                if(!busy){request(mc,()->serverChecks(mc));return;}
                if(updated)finish(mc,"PASS "+assertions+" assertions: real brush GUI and close/retry, bound unlock, 311 rigid display parts, 120-tick animation, shared cooldown, real fake-player collision and reportable corpse, road-side rejection, duplicate death, meeting/reset cleanup and navigation");
            }
        }catch(Throwable e){GooseTools.LOGGER.error("Keep Green regression",e);finish(mc,"FAIL "+e);}
    }
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private void request(Minecraft mc,Action a){busy=true;updated=false;since=System.currentTimeMillis();mc.getSingleplayerServer().execute(()->{try{a.run();updated=true;}catch(Throwable e){mc.execute(()->finish(mc,"FAIL server "+e));}});}
    private void next(){scenario++;busy=false;updated=false;since=System.currentTimeMillis();}
    private void prepare(Minecraft mc){try{
        var s=mc.getSingleplayerServer();Path root=Path.of(System.getProperty("keepgreen.fixtureRoot"));Path source=root.resolve("saves/Whoiskiller/datapacks/Whoiskiller");Path dest=s.getWorldPath(LevelResource.ROOT).resolve("datapacks/keepgreen-test");
        try(var stream=Files.walk(source)){for(Path p:stream.filter(Files::isRegularFile).toList()){Path q=dest.resolve(source.relativize(p));Files.createDirectories(q.getParent());Files.copy(p,q);}}
        Path tags=dest.resolve("data/minecraft/tags/function");Files.writeString(tags.resolve("load.json"),"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\"]}");
        Files.writeString(tags.resolve("tick.json"),"{\"values\":[\"keepgreen_check:tick\"]}");Path tick=dest.resolve("data/keepgreen_check/function/tick.mcfunction");Files.createDirectories(tick.getParent());Files.writeString(tick,"execute in minecraft:overworld run function "+BASE+"world_tick\n");
        s.getPackRepository().reload();var packs=new ArrayList<>(s.getPackRepository().getSelectedIds());packs.add("file/keepgreen-test");
        s.reloadResources(packs).whenComplete((v,error)->s.execute(()->{try{
            if(error!=null)throw new IllegalStateException(error);
            for(String o:List.of("gamesetting","ggdSession","ggdadv","ggdId","ggd_temp","ggdObjective","ggdMath","achDuckGooseKills","ggdTaskGiven","ggdTasksDone","TaskLimit"))exec(s,"scoreboard objectives add "+o+" dummy");
            exec(s,"scoreboard players set map gamesetting 11");exec(s,"scoreboard players set FullBloodDLC ggdadv 0");exec(s,"scoreboard players set #MeetingPhase ggdSession 0");
            run(mc,BASE+"reset");var p=player(mc);p.addTag("players");p.addTag("gamingGGD");p.addTag("evil");p.addTag("inTutorial");
            exec(s,"scoreboard players set "+name(mc)+" ggdId 101");exec(s,"forceload add -1728 -576 -1584 -496");
            for(int x=-1728;x<=-1584;x+=16)for(int z=-576;z<=-496;z+=16)p.level().getChunk(x>>4,z>>4);
            exec(s,"fill -1715 71 -524 -1603 71 -510 black_concrete");exec(s,"fill -1615 71 -568 -1602 71 -511 black_concrete");exec(s,"setblock -1604 72 -526 stone");
            exec(s,"summon marker -1604 73 -526 {Tags:[\"task\",\"keepgreen\",\"evil\"]}");exec(s,"tp "+name(mc)+" -1604 73 -526");
            exec(s,"player TruckVictim spawn at -1660 72 -517");ready=true;
        }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}));
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}
    private void accept(Minecraft mc){run(mc,BASE+"taskgui");run(mc,BASE+"accept");require(player(mc).entityTags().contains("inTaskKeepGreen"),"accept failed at "+player(mc).position()+" tags="+player(mc).entityTags()+" gui="+get(mc.getSingleplayerServer(),name(mc),"ggdGuiState"));}
    private void startTruck(Minecraft mc){
        var s=mc.getSingleplayerServer();require(get(s,"#Cooldown","ggdTruck")==0,"initial unlock cooldown");
        var victim=s.getPlayerList().getPlayerByName("TruckVictim");require(victim!=null,"road victim not ready");
        victim.addTag("players");victim.addTag("good");victim.addTag("inTutorial");
        exec(s,"scoreboard players set TruckVictim ggdId 102");
        exec(s,"time set 6000");
        exec(s,"scoreboard players set #task_count ggd_temp 1");run(mc,"ggd:invrepevent/taskmenugui");
        // Finished prerequisite should have no unfinished menu compass.
        require(player(mc).getInventory().getNonEquipmentItems().stream().noneMatch(i->{var d=i.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);return d!=null&&d.copyTag().getString("NavToTask").orElse("").equals("keepgreen");}),"completed prerequisite still in menu");
        run(mc,BASE+"trigger");require(count(mc,"eagleton_truck_part")==311,"display model missing blocks");require(get(s,"#Cooldown","ggdTruck")==900,"use did not start 45s cooldown");
        run(mc,BASE+"trigger");require(count(mc,"eagleton_truck_part")==311,"repeated use spawned duplicate truck");
        exec(s,"gamemode spectator "+name(mc));exec(s,"title "+name(mc)+" clear");
        exec(s,"tp "+name(mc)+" -1635 118 -490 180 65");player(mc).addTag("spectator");
    }
    private void serverChecks(Minecraft mc)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);p.removeTag("spectator");p.removeTag("inTask");exec(s,"gamemode creative "+name(mc));
        require(get(s,"#Active","ggdTruck")==0&&count(mc,"eagleton_truck")==0,"end of route not cleaned");
        var roadVictim=s.getPlayerList().getPlayerByName("TruckVictim");require(roadVictim!=null&&roadVictim.entityTags().contains("spectator"),"moving truck did not kill road victim");
        require(count(mc,"deadbody")==1&&get(s,name(mc),"achDuckGooseKills")==1,"moving truck corpse or credit missing");
        roadVictim.removeTag("spectator");roadVictim.removeTag("deadInMap");exec(s,"gamemode creative TruckVictim");
        int cd=get(s,"#Cooldown","ggdTruck");p.addTag("inTaskKeepGreen");p.addTag("inTaskGooseGui");exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 2");run(mc,BASE+"finish");require(get(s,"#Cooldown","ggdTruck")==cd,"duplicate unlock reset shared cooldown");run(mc,BASE+"fail");
        p.removeTag("task.keepgreen.finished");p.addTag("inTaskKeepGreen");p.addTag("inTaskGooseGui");exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 2");run(mc,BASE+"finish");require(get(s,"#Cooldown","ggdTruck")==cd,"late unlock reset shared cooldown");
        exec(s,"scoreboard players set #Cooldown ggdTruck 1");run(mc,BASE+"world_tick");require(get(s,"#Cooldown","ggdTruck")==0,"cooldown expiry failed");
        var victim=s.getPlayerList().getPlayerByName("TruckVictim");require(victim!=null,"fake player missing");victim.addTag("players");victim.addTag("good");victim.addTag("inTutorial");
        exec(s,"tp TruckVictim -1660 72 -517");exec(s,"scoreboard players set TruckVictim ggdId 102");exec(s,"scoreboard players set #Owner ggdTruck 101");
        int before=count(mc,"deadbody");exec(s,"execute as TruckVictim at @s run function "+BASE+"collision {cx:-1660000,cz:-517000,cos:10000,sin:0}");
        require(victim.entityTags().contains("spectator"),"collision did not kill at "+victim.position()+" tags="+victim.entityTags()+" math="+get(s,"#LX","ggdTruckMath")+","+get(s,"#LZ","ggdTruckMath")+","+get(s,"#Y","ggdTruckMath"));require(count(mc,"deadbody")==before+1,"collision corpse missing");
        require(get(s,name(mc),"achDuckGooseKills")==2,"duck-goose achievement not credited once");
        var corpses=new ArrayList<net.minecraft.world.entity.Entity>();for(var e:p.level().getAllEntities())if(e.entityTags().contains("deadbody"))corpses.add(e);var body=corpses.get(corpses.size()-1);
        require(Math.abs(body.getX()+1660)<1&&Math.abs(body.getZ()+517)<1,"corpse not at impact");require(!body.entityTags().contains("cargoDoorUnreportable"),"truck corpse unreportable");
        exec(s,"execute as TruckVictim at @s run function "+BASE+"hit");require(count(mc,"deadbody")==before+1&&get(s,name(mc),"achDuckGooseKills")==2,"duplicate death credited");
        victim.removeTag("spectator");victim.removeTag("deadInMap");exec(s,"tp TruckVictim -1660 72 -510");exec(s,"execute as TruckVictim at @s run function "+BASE+"collision {cx:-1660000,cz:-517000,cos:10000,sin:0}");require(!victim.entityTags().contains("spectator"),"roadside false positive");
        run(mc,BASE+"trigger");require(count(mc,"eagleton_truck_part")==311,"second launch missing");exec(s,"scoreboard players set #MeetingPhase ggdSession 1");run(mc,BASE+"world_tick");require(count(mc,"eagleton_truck")==0,"meeting left truck parts");require(p.entityTags().contains("task.keepgreen.finished"),"meeting cleared unlock");
        exec(s,"scoreboard players set #MeetingPhase ggdSession 0");run(mc,BASE+"reset");require(get(s,"#Cooldown","ggdTruck")==0&&!p.entityTags().contains("task.keepgreen.finished"),"reset leaked state");
        exec(s,"tp "+name(mc)+" -1604 73 -526");exec(s,"scoreboard players set #task_count ggd_temp 1");run(mc,"ggd:invrepevent/taskmenugui");require(p.getInventory().getNonEquipmentItems().stream().anyMatch(i->{var d=i.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);return d!=null&&d.copyTag().getString("NavToTask").orElse("").equals("keepgreen");}),"unfinished menu compass missing");
        exec(s,"execute as "+name(mc)+" at @s run function ggd:task/nav_start {task:\"keepgreen\",name_key:\"item.task.keepgreen.available\"}");require(p.entityTags().contains("navigating_to_keepgreen"),"navigation tag missing");
    }
    private void sample(Minecraft mc){mc.getSingleplayerServer().execute(()->{unlocked=player(mc).entityTags().contains("task.keepgreen.finished");active=player(mc).entityTags().contains("inTaskKeepGreen");frame=get(mc.getSingleplayerServer(),"#Frame","ggdTruck");parts=count(mc,"eagleton_truck_part");cooldown=get(mc.getSingleplayerServer(),"#Cooldown","ggdTruck");});}
    private void paint(TaskScreen s){long[] bits=s.currentState().cleaned();for(int cell=0;cell<KeepGreenLayout.CELLS;cell++)if(KeepGreenLayout.target(cell)&&!TaskExtraLayout.cleaned(bits,cell)){
        double x=KeepGreenLayout.cellX(cell),y=KeepGreenLayout.cellY(cell);s.mouseClicked(mouse(s,x,y),false);s.mouseDragged(mouse(s,x+3,y),3,0);s.mouseReleased(mouse(s,x+3,y));return;}}
    private static MouseButtonEvent mouse(TaskScreen s,double x,double y){double z=Math.max(.1,Math.min(1.5,Math.min((s.width-24.)/420,(s.height-24.)/320)));return new MouseButtonEvent((s.width-420*z)/2+x*z,(s.height-320*z)/2+y*z,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private void require(boolean b,String reason){assertions++;if(!b)throw new IllegalStateException(reason);}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static int count(Minecraft mc,String tag){int n=0;for(var e:player(mc).level().getAllEntities())if(e.entityTags().contains(tag))n++;return n;}
    private static void exec(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),command);}
    private static void run(Minecraft mc,String fn){exec(mc.getSingleplayerServer(),"execute as "+name(mc)+" at @s run function "+fn);}
    private static int get(MinecraftServer s,String holder,String obj){var o=s.getScoreboard().getObjective(obj);var v=o==null?null:s.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(holder),o);return v==null?0:v.value();}
    private static void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){GooseTools.LOGGER.error("Screenshot",e);}});}
    private void finish(Minecraft mc,String text){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception e){GooseTools.LOGGER.error("Result",e);}mc.stop();}
}
