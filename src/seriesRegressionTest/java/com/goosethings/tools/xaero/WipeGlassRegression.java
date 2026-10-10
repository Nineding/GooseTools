package com.goosethings.tools.xaero;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.ClientTaskMarkers;
import com.goosethings.tools.client.task.TaskScreen;
import com.goosethings.tools.task.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;

/** Real isolated integrated server and mouse-driven cleaning; no user save is opened. */
public final class WipeGlassRegression implements ClientModInitializer {
    private static final String BASE="ggd:task/eagleton_simplify/wipeglass/";
    private static final String[] NODES={"hut","restaurant","lab"};
    private static final String[] NAMES={"擦玻璃之坍塌小屋","擦玻璃之餐厅","擦玻璃之秘密实验室"};
    private static final int[][] POS={{-1657,72,-524},{-1689,73,-526},{-1651,71,-541}};
    private boolean opened,prepared,finished,busy,ready,screenshot;
    private volatile boolean serverReady,updated,active;
    private volatile Set<String> tags=Set.of();
    private volatile int progress,given,done;
    private int ticks,scenario,frame,assertions;
    private long started=System.currentTimeMillis(),since;
    private TaskLayout layout;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            require(System.currentTimeMillis()-started<240_000,"timeout scenario="+scenario+" tags="+tags);
            if(!opened){opened=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("wipeglass-"+System.currentTimeMillis(),
                    new LevelSettings("Wipe Glass regression",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++ticks<50)return;
            if(!prepared){prepared=true;mc.getSingleplayerServer().execute(()->prepare(mc));return;}
            if(!serverReady)return;
            sample(mc);
            if(!ready){ready=true;since=System.currentTimeMillis();}
            if(scenario==0){
                if(!busy){request(mc,()->setup(mc));return;}
                if(!updated||System.currentTimeMillis()-since<1000)return;
                verifyMarkers(Set.of("hut","restaurant","lab"));
                require(progress==0&&given==4&&done==0,"initial accounting");
                capture(mc,"series-minimap");mc.gui.setScreen(new GradientPreview());next();return;
            }
            if(scenario==1){if(System.currentTimeMillis()-since<800)return;capture(mc,"series-worldmap");mc.gui.setScreen(null);next();return;}
            if(scenario==2||scenario==4||scenario==5){
                int node=scenario==2?0:scenario==4?1:2;
                if(!busy){request(mc,()->accept(mc,node));layout=null;screenshot=false;return;}
                if(!updated)return;
                if(tags.contains("task.wipeglass_"+NODES[node]+".finished")||scenario==5&&done==1){
                    if(scenario!=5){require(progress==0&&given==4&&done==0,"partial node awarded/refilled");
                        verifyMarkers(scenario==2?Set.of("restaurant","lab"):Set.of("lab"));}
                    else{require(progress==2000&&done==1&&given==5,"series did not settle once for +2 / one replacement");
                        require(!tags.contains("task.wipeglass.available"),"parent not consumed");verifyMarkers(Set.of());}
                    next();return;
                }
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.taskType()!=TaskType.CLEANING||s.currentState()==null||!s.currentState().started())return;
                if(!screenshot){capture(mc,"cleaning-"+NODES[node]);screenshot=true;}
                if(layout==null){Field f=TaskScreen.class.getDeclaredField("layout");f.setAccessible(true);layout=(TaskLayout)f.get(s);}
                if(++frame%3==0)wipe(s);return;
            }
            if(scenario==3){
                if(!busy){request(mc,()->accept(mc,2));return;}
                if(!updated)return;
                if(mc.gui.screen() instanceof TaskScreen s&&s.currentState()!=null&&s.currentState().started()){s.onClose();return;}
                if(!active&&System.currentTimeMillis()-since>700){require(tags.contains("task.wipeglass_hut.finished")&&tags.contains("task.wipeglass_lab.available")&&!tags.contains("task.wipeglass_lab.finished"),"early close lost completed node or completed lab");require(progress==0&&given==4,"early close awarded");next();}return;
            }
            if(scenario==6){
                if(!busy){request(mc,()->serverChecks(mc));return;}
                if(!updated)return;
                finish(mc,"PASS "+assertions+" assertions: real cleaning inputs at all three coordinates, early close/retry, partial +0 and final +2000, single refill, menu/navigation, private Xaero lists and gradient GPU preview; server role/cleanup/binding checks");
            }
        }catch(Throwable e){GooseTools.LOGGER.error("Wipe Glass regression",e);finish(mc,"FAIL "+e);}
    }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private void request(Minecraft mc,CheckedAction r){busy=true;updated=false;since=System.currentTimeMillis();mc.getSingleplayerServer().execute(()->{try{r.run();updated=true;}catch(Throwable e){mc.execute(()->finish(mc,"FAIL server "+e));}});}
    private void next(){scenario++;busy=false;updated=false;frame=0;since=System.currentTimeMillis();}
    private void prepare(Minecraft mc){try{
        var server=mc.getSingleplayerServer();Path root=Path.of(System.getProperty("series.fixtureRoot"));
        Path source=root.resolve("saves/Whoiskiller/datapacks/Whoiskiller");Path dest=server.getWorldPath(LevelResource.ROOT).resolve("datapacks/series-test");
        try(var stream=Files.walk(source)){for(Path p:stream.filter(Files::isRegularFile).toList()){
            Path q=dest.resolve(source.relativize(p));Files.createDirectories(q.getParent());Files.copy(p,q);}}
        Path functions=dest.resolve("data/minecraft/tags/function");
        Files.writeString(functions.resolve("load.json"),"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\"]}");
        Files.writeString(functions.resolve("tick.json"),"{\"values\":[\"series_check:tick\"]}");
        Path tick=dest.resolve("data/series_check/function/tick.mcfunction");Files.createDirectories(tick.getParent());
        StringBuilder t=new StringBuilder();for(String slug:NODES)t.append("execute as @a[tag=inTaskWipeGlass_").append(slug).append("] at @s run function ").append(BASE).append(slug).append("/checking\n");
        t.append("execute as @a[tag=task.wipeglass.finished] at @s run function ").append(BASE).append("process_finished\n");
        t.append("execute as @a[tag=gamingGGD] at @s run function ggd:task/map_marker_sync_player\n");Files.writeString(tick,t);
        server.getPackRepository().reload();var packs=new ArrayList<>(server.getPackRepository().getSelectedIds());packs.add("file/series-test");
        server.reloadResources(packs).whenComplete((v,error)->server.execute(()->{try{
            if(error!=null)throw new IllegalStateException(error);
            for(String name:List.of("gamesetting","ggdSession","ggdadv","ggdObjective","ggdMath","ggdTaskGiven","ggdTasksDone","ggdTaskRoll","TaskCount","TaskLimit","achTasksDone","ggdId","ggd_temp"))exec(server,"scoreboard objectives add "+name+" dummy");
            exec(server,"scoreboard players set map gamesetting 11");exec(server,"scoreboard players set #MeetingPhase ggdSession 0");
            exec(server,"scoreboard players set HowManyNeedDone ggdObjective 100000");exec(server,"scoreboard players set HalfLevel ggdMath 50000");
            var player=server.getPlayerList().getPlayer(mc.player.getUUID());player.addTag("players");player.addTag("gamingGGD");player.addTag("inTutorial");player.addTag("good");
            for(int cx=(-1700>>4);cx<=(-1640>>4);cx++)for(int cz=(-550>>4);cz<=(-515>>4);cz++)player.level().getChunk(cx,cz);
            String load=Files.readString(source.resolve("data/mainsys/function/map/eagleton_simplify/loadggdtask.mcfunction"));
            for(String line:load.split("\\R"))if(line.startsWith("summon marker ")&&line.contains("\"wipeglass\""))exec(server,line);
            for(int[] c:POS)exec(server,"setblock "+c[0]+" "+(c[1]-1)+" "+c[2]+" stone");
            exec(server,"tp "+name(mc)+" -1657 72 -524");exec(server,"goosetools markers reload");serverReady=true;
        }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}));
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}
    private void setup(Minecraft mc)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);run(mc,BASE+"give");run(mc,BASE+"give");
        require(get(s,p,"ggdTaskGiven")==1,"series dealt more than once");
        for(String id:List.of("reading","musician","derust"))p.addTag("task."+id+".available");
        exec(s,"scoreboard players set "+name(mc)+" ggdTaskGiven 4");exec(s,"scoreboard players set HowManyDone ggdObjective 0");
        run(mc,"ggd:task/roll_new_eagleton_simplify");require(get(s,p,"TaskCount")==4,"series counted as multiple slots");
        exec(s,"scoreboard players set #task_count ggd_temp 1");run(mc,"ggd:invrepevent/taskmenugui");
        Set<String> targets=new HashSet<>();for(var stack:p.getInventory().getNonEquipmentItems()){
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);if(data!=null){String target=data.copyTag().getString("NavToTask").orElse("");if(target.startsWith("wipeglass_"))targets.add(target);}}
        require(targets.equals(Set.of("wipeglass_hut","wipeglass_restaurant","wipeglass_lab")),"node menu compasses missing/duplicated: "+targets);
        exec(s,"clear "+name(mc));
        for(String slug:NODES){exec(s,"execute as "+name(mc)+" at @s run function ggd:task/nav_start {task:\"wipeglass_"+slug+"\",name_key:\"item.task.wipeglass_"+slug+".available\"}");require(p.entityTags().contains("navigating_to_wipeglass_"+slug),"nav absent "+slug);}
        run(mc,"ggd:canceltask");require(p.entityTags().contains("task.wipeglass.available"),"cancel cleared uncompleted parent");
    }
    private void accept(Minecraft mc,int node){var s=mc.getSingleplayerServer();int[] c=POS[node];exec(s,"tp "+name(mc)+" "+c[0]+" "+c[1]+" "+c[2]);run(mc,BASE+NODES[node]+"/taskgui");run(mc,BASE+NODES[node]+"/accept");require(player(mc).entityTags().contains("inTaskWipeGlass_"+NODES[node]),"accept failed "+NODES[node]);}
    private void serverChecks(Minecraft mc)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);int before=get(s,p,"ggdTaskGiven");run(mc,BASE+"process_finished");require(get(s,p,"ggdTasksDone")==1&&get(s,p,"ggdTaskGiven")==before,"duplicate completion");
        require(get(s,"HowManyDone","ggdObjective")==2000,"duplicate progress");
        for(String role:List.of("evil","neutral")){
            run(mc,"ggd:canceltaskfinished");p.removeTag("good");p.removeTag("evil");p.removeTag("neutral");p.addTag(role);
            exec(s,"scoreboard players set "+name(mc)+" ggdTaskGiven "+(role.equals("neutral")?5:0));run(mc,BASE+"give");
            if(role.equals("evil"))p.addTag("task.reading.available");
            for(String slug:NODES){p.addTag("inTaskWipeGlass_"+slug);p.addTag("inTaskGooseGui");exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 2");run(mc,BASE+slug+"/finish");}
            run(mc,BASE+"process_finished");require(get(s,"HowManyDone","ggdObjective")==2000,"fake series added goose progress");
            if(role.equals("neutral"))require(get(s,p,"ggdTaskGiven")==6,"bird refilled beyond six");
            else require(get(s,p,"ggdTaskGiven")==2,"duck replacement count");
        }
        p.removeTag("neutral");p.addTag("good");run(mc,"ggd:canceltaskfinished");run(mc,BASE+"give");
        p.addTag("task.wipeglass_hut.finished");p.removeTag("task.wipeglass_hut.available");run(mc,"ggd:task/meeting_cancel_normal_task");require(p.entityTags().contains("task.wipeglass_hut.finished"),"meeting forgot completed node");
        exec(s,"scoreboard players set map gamesetting 10");run(mc,"ggd:task/roll_new");require(p.entityTags().stream().noneMatch(t->t.contains("wipeglass")),"offmap state leaked");
        exec(s,"scoreboard players set map gamesetting 11");run(mc,BASE+"give");run(mc,"ggd:dlc/ghost/clear_normal_tasks");require(p.entityTags().stream().noneMatch(t->t.contains("wipeglass")),"ghost state leaked");
        p.addTag("spectator");p.addTag("task.wipeglass.finished");p.addTag("task.wipeglass.available");exec(s,"scoreboard players set HowManyDone ggdObjective 99000");run(mc,BASE+"process_finished");require(get(s,"HowManyDone","ggdObjective")==99000,"spectator pushed full weight over limit");p.removeTag("spectator");
        // Controlled bindings verify UUID/session isolation without faking GUI success in the real client cases.
        Field bf=GuiTaskBridge.class.getDeclaredField("bindings");bf.setAccessible(true);@SuppressWarnings("unchecked") Map<UUID,Object> bindings=(Map<UUID,Object>)bf.get(null);
        Class<?> bc=Class.forName("com.goosethings.tools.task.GuiTaskBridge$Binding");var ctor=bc.getDeclaredConstructor(String.class,long.class);ctor.setAccessible(true);
        p.addTag("inTaskGooseGui");bindings.put(p.getUUID(),ctor.newInstance("cleaning",901L));exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 1");
        GuiTaskBridge.taskProgress(p,900,true);require(get(s,p,"ggdGuiState")==1,"stale cleaning result accepted");
        GuiTaskBridge.taskClosed(p,900);require(get(s,p,"ggdGuiState")==1,"stale close accepted");
        GuiTaskBridge.taskProgress(p,901,true);require(get(s,p,"ggdGuiState")==2,"cleaning bound result missing");GuiTaskBridge.taskClosed(p,901);require(get(s,p,"ggdGuiState")==2,"close undid cleaning success");
        bindings.put(p.getUUID(),ctor.newInstance("cleaning",902L));exec(s,"scoreboard players set "+name(mc)+" ggdGuiState 1");GuiTaskBridge.taskClosed(p,902);require(get(s,p,"ggdGuiState")==3,"cleaning early close did not fail");GuiTaskBridge.clear(p);p.removeTag("inTaskGooseGui");
    }
    private void sample(Minecraft mc){mc.getSingleplayerServer().execute(()->{var p=player(mc);tags=Set.copyOf(p.entityTags());active=tags.contains("inTaskGooseGui");var s=mc.getSingleplayerServer();progress=get(s,"HowManyDone","ggdObjective");given=get(s,p,"ggdTaskGiven");done=get(s,p,"ggdTasksDone");});}
    private void verifyMarkers(Set<String> slugs)throws Exception{
        Set<String> expected=new HashSet<>();for(String s:slugs)expected.add("wipeglass_"+s);
        for(var loc:List.of(MinimapElementRenderLocation.OVER_MINIMAP,MinimapElementRenderLocation.WORLD_MAP)){
            var context=new GgdMapElementRenderer.Context();var method=context.getClass().getDeclaredMethod("rebuild",MinimapElementRenderLocation.class);method.setAccessible(true);method.invoke(context,loc);
            var field=context.getClass().getDeclaredField("frameElements");field.setAccessible(true);Set<String> found=new HashSet<>();
            for(Object obj:(List<?>)field.get(context)){var e=(GgdMapElementRenderer.Element)obj;if(!e.textKey().startsWith("wipeglass_"))continue;found.add(e.textKey());int i=Arrays.asList(NODES).indexOf(e.textKey().substring(10));
                require(e.x()==POS[i][0]+.5&&e.y()==POS[i][1]&&e.z()==POS[i][2]+.5,"marker coordinate "+e.textKey());
                var config=ClientTaskMarkers.current();var task=config.task(e.textKey());require(Component.translatableWithFallback(task.translationKey(),task.fallback()).getString().equals(NAMES[i]),"node translation");
                require(config.backgroundAt(e.textKey(),"normal",0)==0xD02879C8&&config.backgroundAt(e.textKey(),"normal",1)==0xD026A66A,"gradient missing");}
            require(found.equals(expected),"marker list "+loc+" expected="+expected+" got="+found);
        }
    }
    private void wipe(TaskScreen s){
        long[] bits=s.currentState().cleaned();for(int cell=0;cell<TaskExtraLayout.CLEAN_CELLS;cell++)if(layout.extra.stains[cell]>=0&&!TaskExtraLayout.cleaned(bits,cell)){
            double x=44+(cell%24+.5)*12,y=96+(cell/24+.5)*12;var e=mouse(s,x,y);s.mouseClicked(e,false);s.mouseDragged(mouse(s,x+10,y),0,0);s.mouseReleased(mouse(s,x+10,y));return;}
    }
    private static MouseButtonEvent mouse(TaskScreen s,double x,double y){double z=Math.max(.1,Math.min(1.5,Math.min((s.width-24.0)/420,(s.height-24.0)/320)));return new MouseButtonEvent((s.width-420*z)/2+x*z,(s.height-320*z)/2+y*z,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private void require(boolean ok,String reason){assertions++;if(!ok)throw new IllegalStateException(reason);}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static void run(Minecraft mc,String fn){exec(mc.getSingleplayerServer(),"execute as "+name(mc)+" at @s run function "+fn);}
    private static void exec(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),command);}
    private static int get(MinecraftServer s,net.minecraft.world.scores.ScoreHolder p,String key){var o=s.getScoreboard().getObjective(key);var v=o==null?null:s.getScoreboard().getPlayerScoreInfo(p,o);return v==null?0:v.value();}
    private static int get(MinecraftServer s,String p,String key){return get(s,net.minecraft.world.scores.ScoreHolder.forNameOnly(p),key);}
    private static void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){GooseTools.LOGGER.error("Screenshot",e);}});}
    private void finish(Minecraft mc,String text){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception e){GooseTools.LOGGER.error("Result",e);}mc.stop();}
    private static final class GradientPreview extends Screen {
        GradientPreview(){super(Component.literal("Series markers"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){g.fill(0,0,width,height,0xFF20333A);GgdMapElementRenderer.renderWorldMapOverlay(g,-1666,-530,5,0,0,width,height);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
