package com.goosethings.tools.task;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.ClientTaskMarkers;
import com.goosethings.tools.client.task.ProfessionScreen;
import com.goosethings.tools.client.task.PowerStationScreen;
import com.goosethings.tools.map.TaskMarkerConfig;
import com.goosethings.tools.xaero.GgdMapElementRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.*;

/** One isolated real client. Solvers operate on the real server models, never on completion scores. */
public final class ArtisanRegression implements ClientModInitializer {
    private static final String BASE="ggd:task/eagleton_simplify/artisan/";
    private static final String[] KINDS={"civil","foodsafety","nuclear","powerstation","telecom","civil","civil"};
    private static final int[][] POS={{-1597,73,-492},{-1687,73,-533},{-1652,72,-491},{-1645,72,-505},{-1626,72,-535},{-1597,73,-492},{-1597,73,-492}};
    private boolean opened,prepared,finished;
    private volatile boolean serverReady;
    private int warmup,scenario=Integer.getInteger("artisan.startScenario",0),assertions,captures;
    private long start=System.currentTimeMillis(),since;
    private CompletableFuture<Void> pending;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            require(System.currentTimeMillis()-start<300000,"timeout scenario "+scenario);
            if(!opened){opened=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);
                mc.createWorldOpenFlows().createFreshLevel("artisan-"+System.currentTimeMillis(),
                    new LevelSettings("Artisan regression",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false),p->p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;}
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++warmup<50)return;
            if(!prepared){prepared=true;prepare(mc);return;}
            if(!serverReady)return;
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;since=System.currentTimeMillis();}
            if(scenario==0){scenario++;request(mc,()->initial(mc));return;}
            if(scenario==1){if(System.currentTimeMillis()-since<1200)return;capture(mc,"artisan-npc");mc.gui.setScreen(new GradientPreview());scenario++;since=System.currentTimeMillis();return;}
            if(scenario==2){if(System.currentTimeMillis()-since<500)return;capture(mc,"artisan-gradient");mc.gui.setScreen(null);scenario++;request(mc,()->setup(mc,0));return;}
            if(scenario==3){if(!screenStarted(mc))return;scenario++;since=System.currentTimeMillis();return;}
            if(scenario==4){if(System.currentTimeMillis()-since<700)return;mc.gui.screen().onClose();scenario++;since=System.currentTimeMillis();return;}
            if(scenario==5){if(System.currentTimeMillis()-since<1200)return;scenario++;request(mc,()->cancellation(mc));return;}
            if(scenario>=6&&scenario<27){
                int index=(scenario-6)/3,phase=(scenario-6)%3;
                if(phase==0){scenario++;request(mc,()->setup(mc,index));return;}
                if(phase==1){if(!screenStarted(mc)||System.currentTimeMillis()-since<500)return;capture(mc,"artisan-"+KINDS[index]+"-"+index);scenario++;request(mc,()->solveCurrent(mc));return;}
                if(System.currentTimeMillis()-since<800)return;scenario++;request(mc,()->settled(mc,index));return;
            }
            if(scenario==27){scenario++;request(mc,()->serverChecks(mc));return;}
            if(scenario==28){if(System.currentTimeMillis()-since<1000)return;String coverage=Integer.getInteger("artisan.startScenario",0)==0
                    ? "all five goose GUI panels (+4), duck/neutral GUI panels (+0), refill, early-close retry, NPC accept/giveup and gradient"
                    : "focused remaining duck/neutral GUI panels (+0), refill and neutral no-refill";
                finish(mc,"PASS "+assertions+" assertions; "+coverage+", session isolation, menu/achievement/title/reset and 249/250/500 breakthrough boundaries; "+captures+" GPU screenshots");}
        }catch(Throwable e){GooseTools.LOGGER.error("Artisan regression",e);finish(mc,"FAIL "+e);}
    }
    @FunctionalInterface private interface Work{void run()throws Exception;}
    private void request(Minecraft mc,Work work){pending=new CompletableFuture<>();var next=pending;mc.getSingleplayerServer().execute(()->{try{work.run();next.complete(null);}catch(Throwable e){next.completeExceptionally(e);}});}
    private void prepare(Minecraft mc){mc.getSingleplayerServer().execute(()->{try{
        var server=mc.getSingleplayerServer();Path source=Path.of(System.getProperty("artisan.fixtureRoot")).resolve("saves/Whoiskiller/datapacks/Whoiskiller");
        Path dest=server.getWorldPath(LevelResource.ROOT).resolve("datapacks/artisan-test");
        try(var files=Files.walk(source)){for(Path p:files.filter(Files::isRegularFile).toList()){Path q=dest.resolve(source.relativize(p));Files.createDirectories(q.getParent());Files.copy(p,q);}}
        Path functions=dest.resolve("data/minecraft/tags/function");
        Files.writeString(functions.resolve("load.json"),"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\",\"ggd:task/eagleton_simplify/artisan/load\"]}");
        Files.writeString(functions.resolve("tick.json"),"{\"values\":[\"artisan_check:tick\"]}");
        Path tick=dest.resolve("data/artisan_check/function/tick.mcfunction");Files.createDirectories(tick.getParent());
        Files.writeString(tick,"function "+BASE+"tick\nexecute as @a[tag=task.artisan.finished] at @s run function "+BASE+"process_finished\nexecute as @a[tag=gamingGGD] at @s run function ggd:task/map_marker_sync_player\n");
        server.getPackRepository().reload();var packs=new ArrayList<>(server.getPackRepository().getSelectedIds());packs.add("file/artisan-test");
        server.reloadResources(packs).whenComplete((v,error)->server.execute(()->{try{
            if(error!=null)throw new IllegalStateException(error);
            var objectives=new LinkedHashMap<String,String>();
            try(var files=Files.walk(source.resolve("data"))){for(Path f:files.filter(p->p.toString().endsWith(".mcfunction")).toList())for(String line:new String(Files.readAllBytes(f),java.nio.charset.StandardCharsets.UTF_8).split("\\R")){
                if(line.startsWith("scoreboard objectives add ")){String[] words=line.split(" ");if(!words[3].contains("$"))objectives.put(words[3],words[4]);}}}
            for(var o:objectives.entrySet())exec(server,"scoreboard objectives add "+o.getKey()+" "+o.getValue());
            for(String name:List.of("gamesetting","ggdSession","ggdadv","ggdObjective","ggdMath","ggdTaskGiven","ggdTasksDone","ggdTaskRoll","TaskCount","TaskLimit","achTasksDone","ggdId","ggd_temp","playerLevel","playerExp","playerExpBank","levelMath","trustLevel","achievementPoints"))exec(server,"scoreboard objectives add "+name+" dummy");
            exec(server,"scoreboard players set map gamesetting 11");exec(server,"scoreboard players set FullBloodDLC ggdadv 1");exec(server,"scoreboard players set #MeetingPhase ggdSession 0");
            exec(server,"scoreboard players set HowManyNeedDone ggdObjective 100000");exec(server,"scoreboard players set HalfLevel ggdMath 50000");exec(server,"scoreboard players set HowManyDone ggdObjective 0");
            var player=player(mc);for(String tag:List.of("players","gamingGGD","good"))player.addTag(tag);
            if(Integer.getInteger("artisan.startScenario",0)>0){for(String kind:List.of("civil","foodsafety","nuclear","powerstation","telecom"))player.addTag("ach_artisan_"+kind);run(mc,"lobby:achievement/recalc_points");}
            for(int cx=(-1700>>4);cx<=(-1575>>4);cx++)for(int cz=(-560>>4);cz<=(-485>>4);cz++)player.level().getChunk(cx,cz);
            exec(server,"fill -1586 73 -530 -1578 73 -522 stone_bricks");for(int[] c:POS)exec(server,"setblock "+c[0]+" "+(c[1]-1)+" "+c[2]+" stone");
            run(mc,BASE+"spawn");exec(server,"tp "+name(mc)+" -1584 74 -526 -90 0");exec(server,"goosetools markers reload");
            serverReady=true;
        }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}));
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}});}
    private void initial(Minecraft mc)throws Exception{
        run(mc,BASE+"on_interact");require(player(mc).entityTags().contains("artisan.inDialogue"),"ordinary dialogue absent");require(!player(mc).entityTags().contains("task.artisan.available"),"ordinary dialogue grants task");
        var npc=player(mc).level().getEntitiesOfClass(net.minecraft.world.entity.decoration.Mannequin.class,new net.minecraft.world.phys.AABB(-1585,73,-529,-1579,78,-523),e->e.entityTags().contains("artisan_npc"));
        require(npc.size()==1,"NPC missing/duplicate");require(npc.getFirst().getProfile().skinPatch().body().orElseThrow().toString().contains("artisan_skin"),"NPC skin patch absent");require(npc.getFirst().getYRot()==90,"NPC rotation");
        run(mc,BASE+"give");run(mc,BASE+"on_interact");exec(mc.getSingleplayerServer(),"scoreboard players set "+name(mc)+" ArtisanTrigger 2");run(mc,BASE+"tick");
        require(player(mc).entityTags().contains("artisan.assigned"),"NPC click did not assign");
        require(player(mc).entityTags().stream().filter(t->t.startsWith("task.artisan_")&&t.endsWith(".available")).count()==1,"NPC assigned multiple commissions");
        var c=TaskMarkerConfig.defaults();require(c.task("artisan").category().equals("artisan"),"category missing");require(c.backgroundAt("artisan","normal",0)==0xD0C9A227&&c.backgroundAt("artisan","normal",1)==0xD026A66A,"gold-green ramp missing");
        require(c.backgroundAt("wipeglass_hut","normal",0)==0xD02879C8,"other series changed");
    }
    private void setup(Minecraft mc,int index){
        var s=mc.getSingleplayerServer();var p=player(mc);run(mc,BASE+"clear_all");
        for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("task.")||tag.startsWith("navigating_to_"))p.removeTag(tag);
        p.removeTag("good");p.removeTag("evil");p.removeTag("neutral");p.addTag(index==5?"evil":index==6?"neutral":"good");
        for(String id:index==5?List.of("reading"):List.of("reading","musician","derust"))p.addTag("task."+id+".available");
        run(mc,BASE+"give");p.addTag("artisan.assigned");exec(s,"scoreboard players set "+name(mc)+" ggdTaskGiven "+(index==6?6:index==5?2:4));
        run(mc,BASE+"assign_"+KINDS[index]);run(mc,"ggd:task/eagleton_simplify/count_ordinary");require(get(s,p,"TaskCount")== (index==5?2:4),"parent not one slot");
        exec(s,"scoreboard players set HowManyDone ggdObjective 0");exec(s,"scoreboard players set "+name(mc)+" ggdTasksDone 0");
        exec(s,"clear "+name(mc));int[] c=POS[index];exec(s,"tp "+name(mc)+" "+c[0]+" "+c[1]+" "+c[2]);
    }
    private void cancellation(Minecraft mc){
        var p=player(mc);require(!p.entityTags().contains("inTaskArtisan")&&p.entityTags().contains("artisan.waitExit"),"early close did not latch");require(p.entityTags().contains("task.artisan_civil.available"),"close lost commission");require(!p.entityTags().contains("ach_artisan_civil"),"close awarded achievement");
        var s=mc.getSingleplayerServer();exec(s,"tp "+name(mc)+" -1584 74 -526");run(mc,BASE+"on_interact");exec(s,"scoreboard players set "+name(mc)+" ArtisanTrigger 3");run(mc,BASE+"tick");
        require(!p.entityTags().contains("task.artisan.available")&&!p.entityTags().contains("artisan.assigned"),"giveup did not clear");require(get(s,p,"ggdTaskGiven")==5,"goose giveup did not refill once");require(get(s,"HowManyDone","ggdObjective")==0,"giveup awarded task progress");
        p.removeTag("good");p.addTag("neutral");run(mc,BASE+"give");p.addTag("artisan.assigned");run(mc,BASE+"assign_civil");exec(s,"scoreboard players set "+name(mc)+" ggdTaskGiven 6");run(mc,BASE+"on_interact");exec(s,"scoreboard players set "+name(mc)+" ArtisanTrigger 3");run(mc,BASE+"tick");require(get(s,p,"ggdTaskGiven")==6,"neutral giveup refilled");
    }
    private void solveCurrent(Minecraft mc)throws Exception{
        Field f=TaskServer.class.getDeclaredField("SESSIONS");f.setAccessible(true);var trial=((Map<?,?>)f.get(null)).get(player(mc).getUUID());require(trial!=null,"bound session absent");
        Field session=trial.getClass().getDeclaredField("session");session.setAccessible(true);TaskSession task=(TaskSession)session.get(trial);require(task.started(),"not started");
        ArtisanSolver.solve(task);require(task.complete()&&task.progress()==4,"server model did not complete all four stages");
        Method send=TaskServer.class.getDeclaredMethod("sendState",ServerPlayer.class,trial.getClass());send.setAccessible(true);send.invoke(null,player(mc),trial);
    }
    private void settled(Minecraft mc,int index){var s=mc.getSingleplayerServer();var p=player(mc);
        require(get(s,"HowManyDone","ggdObjective")== (index>=5?0:4000),"wrong role progress "+index);
        require(get(s,p,"ggdTasksDone")==1,"parent not exactly one completion");require(get(s,p,"ggdTaskGiven")== (index==6?6:index==5?3:5),"wrong refill "+index);
        require(p.entityTags().contains("ach_artisan_"+KINDS[index]),"achievement absent "+KINDS[index]);
        require(!p.entityTags().contains("task.artisan.available")&&!p.entityTags().contains("inTaskArtisan"),"completion left state");
        int before=get(s,p,"ggdTasksDone");run(mc,BASE+"checking");require(get(s,p,"ggdTasksDone")==before,"completion replayed");
        require(get(s,p,"achievementPoints")==111*(Math.min(index,4)+1),"achievement points repeated/absent");
    }
    private void serverChecks(Minecraft mc)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);run(mc,"lobby:designation/check_artisan");require(p.entityTags().contains("designation.artisanUnlocked"),"title not unlocked");run(mc,"lobby:designation/select_artisan");require(get(s,p,"ggdDesignation")==9,"title not selected");
        exec(s,"scoreboard players set "+name(mc)+" achMenuPage 3");run(mc,"lobby:achievement/menu_items");
        for(int i=6;i<=10;i++)require(p.getInventory().getNonEquipmentItems().get(i+9).get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)!=null,"achievement icon absent");
        run(mc,BASE+"give");p.addTag("artisan.assigned");p.addTag("task.artisan_civil.available");p.addTag("spectator");run(mc,BASE+"tick");require(!p.entityTags().contains("task.artisan.available"),"ghost inherited artisan");p.removeTag("spectator");
        for(String kind:new String[]{"civil","foodsafety","nuclear","powerstation","telecom"})bindingChecks(mc,kind);
        for(int level:new int[]{249,250,500})for(int points:new int[]{999,1000})for(boolean series:new boolean[]{false,true}){
            for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("ach_artisan_")||tag.startsWith("ach_legendary_coop_")||tag.startsWith("level_")||tag.equals("pending_level_up_seniority"))p.removeTag(tag);
            p.addTag("level_professional");if(series)p.addTag("ach_legendary_coop_phoenix");exec(s,"scoreboard players set "+name(mc)+" playerLevel "+level);exec(s,"scoreboard players set "+name(mc)+" achievementPoints "+points);
            run(mc,"lobby:trust/check_promote_seniority");require(p.entityTags().contains("pending_level_up_seniority")== (level>=250&&points>=1000&&series),"breakthrough boundary "+level+" "+points+" "+series);
        }
        p.removeTag("ach_legendary_coop_phoenix");p.removeTag("pending_level_up_seniority");p.addTag("ach_artisan_civil");exec(s,"scoreboard players set "+name(mc)+" playerLevel 250");exec(s,"scoreboard players set "+name(mc)+" achievementPoints 1000");exec(s,"scoreboard players set "+name(mc)+" playerExpBank 100");exec(s,"scoreboard players set "+name(mc)+" playerExp 0");
        run(mc,"lobby:trust/check_promote_seniority");require(p.entityTags().contains("pending_level_up_seniority"),"artisan cannot qualify");run(mc,"lobby:trust/promote_to_seniority");require(p.entityTags().contains("level_seniority")&&!p.entityTags().contains("level_professional")&&get(s,p,"trustLevel")==4,"seniority not promoted");require(get(s,p,"playerExpBank")==0,"bank not released");
        run(mc,"lobby:reset/trust");require(!p.entityTags().contains("pending_level_up_seniority"),"reset rank left pending");
    }
    private void bindingChecks(Minecraft mc,String kind)throws Exception{
        var s=mc.getSingleplayerServer();var p=player(mc);run(mc,BASE+"clear_all");for(String tag:List.of("task.artisan.available","artisan.assigned","inTaskArtisan","inTaskGooseGui","task.artisan_"+kind+".available"))p.addTag(tag);
        Field field=GuiTaskBridge.class.getDeclaredField("bindings");field.setAccessible(true);@SuppressWarnings("unchecked") Map<UUID,Object> bindings=(Map<UUID,Object>)field.get(null);Class<?> type=Class.forName("com.goosethings.tools.task.GuiTaskBridge$Binding");var ctor=type.getDeclaredConstructor(String.class,long.class);ctor.setAccessible(true);
        bindings.put(p.getUUID(),ctor.newInstance(kind,9001L));set(p,"ggdGuiState",1);GuiTaskBridge.taskProgress(p,9000,true);require(get(s,p,"ggdGuiState")==1,"stale session completed "+kind);p.removeTag("task.artisan_"+kind+".available");GuiTaskBridge.taskProgress(p,9001,true);require(get(s,p,"ggdGuiState")==1,"unassigned callback completed "+kind);p.addTag("task.artisan_"+kind+".available");
        exec(s,"scoreboard players set map gamesetting 8");GuiTaskBridge.taskProgress(p,9001,true);require(get(s,p,"ggdGuiState")==1,"off-map callback completed "+kind);exec(s,"scoreboard players set map gamesetting 11");GuiTaskBridge.taskProgress(p,9001,true);require(get(s,p,"ggdGuiState")==2,"valid callback failed "+kind);GuiTaskBridge.taskClosed(p,9001);require(get(s,p,"ggdGuiState")==2,"close undid success");run(mc,BASE+"clear_all");GuiTaskBridge.taskProgress(p,9001,true);require(get(s,p,"ggdGuiState")==0,"giveup retained session");
    }
    private static boolean screenStarted(Minecraft mc){var s=mc.gui.screen();return s instanceof ProfessionScreen p&&p.currentState()!=null&&p.currentState().started()||s instanceof PowerStationScreen station&&station.currentState()!=null&&station.currentState().started();}
    private void require(boolean ok,String reason){assertions++;if(!ok)throw new IllegalStateException(reason);}
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static void run(Minecraft mc,String fn){exec(mc.getSingleplayerServer(),"execute as "+name(mc)+" at @s run function "+fn);}
    private static void exec(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),command);}
    private static int get(MinecraftServer s,net.minecraft.world.scores.ScoreHolder p,String key){var o=s.getScoreboard().getObjective(key);var v=o==null?null:s.getScoreboard().getPlayerScoreInfo(p,o);return v==null?0:v.value();}
    private static int get(MinecraftServer s,String p,String key){return get(s,net.minecraft.world.scores.ScoreHolder.forNameOnly(p),key);}
    private static void set(ServerPlayer p,String key,int value){var sb=p.level().getServer().getScoreboard();sb.getOrCreatePlayerScore(p,sb.getObjective(key)).set(value);}
    private void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));captures++;}catch(Exception e){GooseTools.LOGGER.error("Artisan screenshot",e);}});}
    private void finish(Minecraft mc,String result){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),result);}catch(Exception e){GooseTools.LOGGER.error("Artisan result",e);}mc.stop();}
    private static final class GradientPreview extends Screen {
        GradientPreview(){super(Component.literal("Artisan markers"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){g.fill(0,0,width,height,0xFF20333A);GgdMapElementRenderer.renderWorldMapOverlay(g,-1638,-520,3,0,0,width,height);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
