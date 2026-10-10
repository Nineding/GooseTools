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
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;

/** A real client validates gestures and binding; isolated command checks validate the two timer phases. */
public final class BroadcastRegression implements ClientModInitializer {
    private static final String CUT="ggd:task/eagleton_simplify/cutwires/", BC="ggd:eagleton_simplify/broadcast/";
    private boolean opened,prepared,finished,busy,guiShot,screenShot;
    private volatile boolean ready,updated,unlocked;
    private int ticks,scenario,assertions,wire,wait,readyTicks;
    private long started=System.currentTimeMillis(),lastCut;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            require(System.currentTimeMillis()-started<240_000,"timeout scenario="+scenario);
            if(!opened){opened=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("broadcast-"+System.currentTimeMillis(),
                    new LevelSettings("Broadcast regression",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++ticks<50)return;
            if(!prepared){prepared=true;mc.getSingleplayerServer().execute(()->prepare(mc));return;}
            if(!ready||++readyTicks<30)return;
            mc.getSingleplayerServer().execute(()->{unlocked=player(mc).entityTags().contains("task.cutwires.finished");});
            if(scenario==0){
                if(!busy){request(mc,()->accept(mc));return;}
                if(updated&&mc.gui.screen() instanceof TaskScreen s&&s.currentState()!=null&&s.currentState().started()){
                    require(s.taskType()==TaskType.CUTWIRES,"wrong GUI");
                    if(++wait<12)return;capture(mc,"cutwires-empty");s.onClose();next();}return;
            }
            if(scenario==1&&unlocked){next();return;}
            if(scenario==1){
                if(!busy){request(mc,()->{require(!player(mc).entityTags().contains("task.cutwires.finished"),"closing unlocked sabotage");require(!player(mc).entityTags().contains("inTaskCutWires"),"closing left active task");accept(mc);});return;}
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.currentState()==null||!s.currentState().started())return;
                if(System.currentTimeMillis()-lastCut<140)return;
                int progress=s.currentState().progress();
                if(progress>=2&&!guiShot){guiShot=true;capture(mc,"cutwires-half");}
                if(progress==4)return;
                // Actual mouse events and ordinary network packets, no client progress injection.
                double x=210,y=CutWiresLayout.y(progress);s.mouseClicked(mouse(s,x,y-10),false);s.mouseDragged(mouse(s,x,y+10),0,20);s.mouseReleased(mouse(s,x,y+10));lastCut=System.currentTimeMillis();
                mc.getSingleplayerServer().execute(()->{unlocked=player(mc).entityTags().contains("task.cutwires.finished");});
            }
            if(scenario==1&&unlocked){next();return;}
            if(scenario==2){
                if(!busy){request(mc,()->serverChecks(mc));return;}
                if(updated){if(++wait<30)return;mapChecks(mc);capture(mc,"broadcast-ready");next();}return;
            }
            if(scenario==3){
                if(!busy){request(mc,()->{run(mc,CUT+"trigger");exec(mc.getSingleplayerServer(),"tp "+name(mc)+" -1657 72 -544 0 22");});return;}
                if(updated&&++wait>20){capture(mc,"broadcast-disabled");next();}return;
            }
            if(scenario==4){
                if(!busy){request(mc,()->{run(mc,BC+"cleanup");require(get(mc,"#EagletonLive")==0&&count(mc,"eagleton_broadcast_display")==0,"cleanup left display");require(!player(mc).entityTags().contains("task.cutwires.finished"),"cleanup left unlock");require(!has(player(mc),"BroadcastSabotageDisplay"),"cleanup left sabotage item");});return;}
                if(updated)finish(mc,"PASS "+assertions+" assertions: real cut GUI close/retry and bound unlock, four authoritative wires, 600+900 ticks, shared states, interrupt/restore, DLC and meeting gates, menu/navigation, Xaero broadcast icon, display orientation and cleanup");
            }
        }catch(Throwable e){GooseTools.LOGGER.error("Broadcast regression",e);finish(mc,"FAIL "+e);}
    }
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private void request(Minecraft mc,Action a){busy=true;updated=false;mc.getSingleplayerServer().execute(()->{try{a.run();updated=true;}catch(Throwable e){GooseTools.LOGGER.error("Broadcast server check",e);mc.execute(()->finish(mc,"FAIL server "+e));}});}
    private void next(){scenario++;busy=false;updated=false;wait=0;}
    private void prepare(Minecraft mc){try{
        var s=mc.getSingleplayerServer();Path root=Path.of(System.getProperty("broadcast.fixtureRoot"));Path source=root.resolve("GooseTools-publish/.cache-unused/build/broadcast-smoke/world/datapacks/Whoiskiller");Path dest=s.getWorldPath(LevelResource.ROOT).resolve("datapacks/broadcast-test");
        try(var stream=Files.walk(source)){for(Path p:stream.filter(Files::isRegularFile).toList()){Path q=dest.resolve(source.relativize(p));Files.createDirectories(q.getParent());Files.copy(p,q);}}
        Path tags=dest.resolve("data/minecraft/tags/function");Files.writeString(tags.resolve("load.json"),"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\"]}");
        Files.writeString(tags.resolve("tick.json"),"{\"values\":[\"ggd:eagleton_simplify/broadcast/tick\"]}");
        s.getPackRepository().reload();var packs=new ArrayList<>(s.getPackRepository().getSelectedIds());packs.add("file/broadcast-test");
        s.reloadResources(packs).whenComplete((v,error)->s.execute(()->{try{
            if(error!=null)throw new IllegalStateException(error);
            for(String o:List.of("gamesetting","ggdSession","ggdadv","ggdId","ggd_temp","ggdObjective","ggdMath","ggdTaskGiven","ggdTasksDone","TaskLimit","carrotrightclick","ggdTruck"))exec(s,"scoreboard objectives add "+o+" dummy");
            exec(s,"scoreboard players set map gamesetting 11");exec(s,"scoreboard players set FullBloodDLC ggdadv 1");exec(s,"scoreboard players set #MeetingPhase ggdSession 0");
            var p=player(mc);p.addTag("players");p.addTag("gamingGGD");p.addTag("evil");p.addTag("inTutorial");
            exec(s,"scoreboard players set "+name(mc)+" ggdId 101");exec(s,"forceload add -1664 -544 -1616 -528");
            for(int x=-1664;x<=-1616;x+=16)for(int z=-544;z<=-528;z+=16)p.level().getChunk(x>>4,z>>4);
            exec(s,"fill -1660 71 -545 -1654 71 -538 stone");exec(s,"fill -1623 71 -530 -1619 71 -526 stone");
            run(mc,BC+"init");exec(s,"scoreboard players set #Cooldown ggdTruck 0");exec(s,"scoreboard players set #Active ggdTruck 0");
            exec(s,"tp "+name(mc)+" -1621 72 -528");ready=true;
        }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}));
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}
    private void accept(Minecraft mc){run(mc,CUT+"taskgui");run(mc,CUT+"accept");require(player(mc).entityTags().contains("inTaskCutWires"),"accept failed "+player(mc).entityTags()+" pos="+player(mc).position()+" live="+get(mc,"#EagletonLive")+" markers="+count(mc,"eagleton_cutwires")+" scores="+mc.getSingleplayerServer().getScoreboard().getTrackedPlayers());}
    private void serverChecks(Minecraft mc)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);
        require(p.entityTags().contains("task.cutwires.finished")&&!p.entityTags().contains("inTaskCutWires"),"bound GUI did not unlock");require(has(p,"broadcastDisable"),"unlock item missing");
        p.addTag("task.keepgreen.finished");run(mc,"ggd:task/eagleton_simplify/keepgreen/inventory");require(p.getInventory().getItem(20).getItem()==net.minecraft.world.item.Items.CARROT_ON_A_STICK,"truck slot changed");require(p.getInventory().getItem(21).getItem()==net.minecraft.world.item.Items.CARROT_ON_A_STICK,"broadcast slot not next to truck");
        exec(s,"tp "+name(mc)+" -1656.5 72 -541.5");run(mc,BC+"start");require(get(mc,"#Active")==1&&p.entityTags().contains("ggdBroadcasting"),"default broadcast unusable");
        run(mc,CUT+"trigger");require(get(mc,"#Disabled")==600&&get(mc,"#SabotageCD")==0,"disable did not start 600 ticks");require(!p.entityTags().contains("ggdBroadcasting")&&get(mc,"#Active")==0,"disable did not interrupt broadcast");require(!has(p,"broadcastDisable")&&!has(p,"BroadcastItem"),"active items left during disable");run(mc,BC+"start");require(get(mc,"#Active")==0,"disabled broadcast started");run(mc,CUT+"trigger");require(get(mc,"#Disabled")==600,"duplicate sabotage changed timer");
        for(int i=0;i<599;i++)run(mc,BC+"world_tick");require(get(mc,"#Disabled")==1&&get(mc,"#SabotageCD")==0,"early restore/cooldown");run(mc,BC+"world_tick");require(get(mc,"#Disabled")==0&&get(mc,"#SabotageCD")==900&&get(mc,"#CD")==0,"restore did not start separate 900 ticks");
        run(mc,BC+"start");require(get(mc,"#Active")==1,"restored broadcast not immediately usable");run(mc,BC+"stop");run(mc,CUT+"trigger");require(get(mc,"#Disabled")==0,"cooldown did not reject sabotage");
        p.removeTag("task.cutwires.finished");p.addTag("inTaskCutWires");p.addTag("inTaskGooseGui");exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 2");run(mc,CUT+"finish");require(get(mc,"#SabotageCD")==900,"late unlock reset shared cooldown");
        exec(s,"scoreboard players set #MeetingPhase ggdSession 1");run(mc,BC+"world_tick");require(get(mc,"#SabotageCD")==899,"meeting did not advance cooldown");run(mc,CUT+"trigger");require(get(mc,"#Disabled")==0,"meeting allowed sabotage");exec(s,"scoreboard players set #MeetingPhase ggdSession 0");
        for(int i=0;i<898;i++)run(mc,BC+"world_tick");require(get(mc,"#SabotageCD")==1&&!has(p,"broadcastDisable"),"early sabotage ready");run(mc,BC+"world_tick");require(get(mc,"#SabotageCD")==0&&has(p,"broadcastDisable"),"900 tick cooldown did not expire");
        exec(s,"scoreboard players set FullBloodDLC ggdadv 0");run(mc,CUT+"trigger");require(get(mc,"#Disabled")==0,"DLC gate bypassed");exec(s,"scoreboard players set FullBloodDLC ggdadv 1");
        exec(s,"tp "+name(mc)+" -1656.5 72 -541.5");run(mc,BC+"start");require(get(mc,"#Active")==1,"broadcast start again failed");p.removeTag("ggdBroadcasting");run(mc,BC+"world_tick");require(get(mc,"#Active")==0&&get(mc,"#CD")==900,"missing speaker left global Active stuck");
        exec(s,"scoreboard players set #CD ggdBroadcast 0");run(mc,BC+"start");p.addTag("spectator");run(mc,BC+"world_tick");require(get(mc,"#Active")==0&&!p.entityTags().contains("ggdBroadcasting"),"death left broadcast active");p.removeTag("spectator");
        var display=p.level().getAllEntities().iterator();boolean facing=false;while(display.hasNext()){var e=display.next();if(e.entityTags().contains("eagleton_broadcast_display"))facing=Math.abs(Math.abs(e.getYRot())-180)<.01&&Math.abs(e.getX()+1657)<.01;}require(facing,"display not centered/facing -Z");
        p.removeTag("task.cutwires.finished");exec(s,"scoreboard players set #task_count ggd_temp 1");run(mc,"ggd:invrepevent/taskmenugui");require(p.getInventory().getNonEquipmentItems().stream().anyMatch(i->{var d=i.get(DataComponents.CUSTOM_DATA);return d!=null&&d.copyTag().getString("NavToTask").orElse("").equals("cutwires");}),"unfinished task menu missing");
        exec(s,"execute as "+name(mc)+" at @s run function ggd:task/nav_start {task:\"cutwires\",name_key:\"item.task.cutwires.available\"}");require(p.entityTags().contains("navigating_to_cutwires"),"navigation missing");
        p.removeTag("inTaskMenuGUI");p.removeTag("inTaskMenuGUI_Drawn");p.removeTag("inTask");p.addTag("task.cutwires.finished");p.removeTag("navigating_to_cutwires");run(mc,CUT+"inventory");
        run(mc,"ggd:task/map_marker_sync_player");exec(s,"scoreboard players set #CD ggdBroadcast 0");exec(s,"tp "+name(mc)+" -1657 72 -544 0 22");run(mc,BC+"update_displays");
    }
    private void mapChecks(Minecraft mc)throws Exception{
        require(GgdMapState.isGameActive(mc),"map game-presence missing");
        for (var location : List.of(MinimapElementRenderLocation.OVER_MINIMAP,MinimapElementRenderLocation.WORLD_MAP)) {
        var context=new GgdMapElementRenderer.Context();var method=context.getClass().getDeclaredMethod("rebuild",MinimapElementRenderLocation.class);method.setAccessible(true);method.invoke(context,location);
        var field=context.getClass().getDeclaredField("frameElements");field.setAccessible(true);
        boolean found=false;for(Object o:(List<?>)field.get(context)){var e=(GgdMapElementRenderer.Element)o;if(e.kind()==GgdMapElementRenderer.ElementKind.BROADCAST){found=true;require(Math.floor(e.x())==-1657&&Math.floor(e.z())==-542,"broadcast icon wrong block: "+e.x()+","+e.z());}}require(found,"Xaero broadcast icon missing "+location);
        }
    }
    private static boolean has(ServerPlayer p,String key){return p.getInventory().getNonEquipmentItems().stream().anyMatch(i->{var d=i.get(DataComponents.CUSTOM_DATA);return d!=null&&d.copyTag().getBoolean(key).orElse(false);});}
    private static MouseButtonEvent mouse(TaskScreen s,double x,double y){double z=Math.max(.1,Math.min(1.5,Math.min((s.width-24.)/420,(s.height-24.)/320)));return new MouseButtonEvent((s.width-420*z)/2+x*z,(s.height-320*z)/2+y*z,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private void require(boolean b,String reason){assertions++;if(!b)throw new IllegalStateException(reason);}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static int count(Minecraft mc,String tag){int n=0;for(var e:player(mc).level().getAllEntities())if(e.entityTags().contains(tag))n++;return n;}
    private static void exec(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),command);}
    private static void run(Minecraft mc,String fn){exec(mc.getSingleplayerServer(),"execute as "+name(mc)+" at @s run function "+fn);}
    private static int get(Minecraft mc,String holder){var sb=mc.getSingleplayerServer().getScoreboard();var o=sb.getObjective("ggdBroadcast");var v=o==null?null:sb.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(holder),o);return v==null?0:v.value();}
    private static void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){GooseTools.LOGGER.error("Screenshot",e);}});}
    private void finish(Minecraft mc,String text){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception e){GooseTools.LOGGER.error("Result",e);}mc.stop();}
}
