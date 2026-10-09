package com.goosethings.tools.game;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.game.GameScreen;
import com.goosethings.tools.client.task.TaskScreen;
import com.goosethings.tools.task.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;

/** Real client/server, authoritative map functions, vanilla block-use packets and GUI inputs. */
public final class GuiMapRegression implements ClientModInitializer {
    private static final String[] IDS={"testdrive","unclogpipes","calibratevoltage","powercontrol","arcadefan","whackmoles"};
    private static final int[][] COORDS={{-1610,73,-488},{-1619,72,-528},{-1626,72,-491},{-1652,72,-505},{-1673,72,-502},{-1638,72,-529}};
    private boolean world,prepared,ready,requested,finished;
    private volatile boolean serverReady;
    private int ticks,scenario=Integer.getInteger("goosetools.guiScenario",0),stage,frame,knob=-1,assertions;
    private long since,pauseAt,pausedTime,lastHit,clickedBorn=-1;
    private volatile int status,time;
    private volatile boolean active,complete;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private long now(){return System.nanoTime()/1_000_000;}
    private void tick(Minecraft mc){
        if(finished||!mc.isGameLoadFinished())return;
        try{
            if(!world){
                world=true;mc.options.pauseOnLostFocus=false;mc.getWindow().setWindowed(1280,900);
                mc.createWorldOpenFlows().createFreshLevel("gui-map-"+System.currentTimeMillis(),
                    new LevelSettings("GUI map regression",net.minecraft.world.level.GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(42,false,false), provider->provider.lookupOrThrow(Registries.WORLD_PRESET)
                    .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||mc.getSingleplayerServer()==null||!mc.player.connection.hasClientLoaded()||++ticks<45)return;
            if(!prepared){prepared=true;mc.getSingleplayerServer().execute(()->prepare(mc));return;}
            if(!serverReady)return;
            if(!ready){ready=true;since=now();}
            if(scenario>=17){finish(mc,"PASS "+assertions+" assertions; real hard traffic survival/pause/close/loss, 20 mole hits/loss/close, three puzzle completions/close, four native block-use targets, arcade cumulative time/close/leave, command-origin exclusion and cancellation");return;}
            require(now()-since<100_000,"timeout scenario="+scenario+" stage="+stage+" status="+status+" active="+active+" complete="+complete);
            frame++;
            if(!requested){requested=true;stage=0;frame=0;knob=-1;clickedBorn=-1;status=0;active=complete=false;
                int id=scenario<=2?0:scenario<=5?5:scenario<=8?scenario-5:scenario<=11?scenario-8:4;
                begin(mc,id);return;}
            sample(mc);
            if(scenario==0){ // Pause and close before thirty.
                if(stage==3&&!active&&status==0){require(!complete,"early close completed");assertions++;next(mc);return;}
                if(!(mc.gui.screen() instanceof GameScreen s)||s.currentState()==null)return;
                var state=s.currentState();assertHard(state);
                if(stage==0&&state.elapsed()>250){key(s,InputConstants.KEY_P);stage=1;return;}
                if(stage==1&&state.phase()==GameSession.PAUSED){pauseAt=now();pausedTime=state.elapsed();stage=2;return;}
                if(stage==2&&now()-pauseAt>700){require(state.elapsed()==pausedTime,"paused driving time increased");assertions++;s.onClose();stage=3;return;}
                if(stage==3&&!active&&status==0){require(!complete,"early close completed");assertions++;next(mc);}return;
            }
            if(scenario==1){
                if(complete&&!active){assertions++;next(mc);return;}
                if(mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){assertHard(s.currentState());drive(s);if(s.currentState().elapsed()>1000&&stage==0){capture(mc,"traffic-hard");stage=1;}}
                return;
            }
            if(scenario==2){if(stage==0&&mc.gui.screen() instanceof GameScreen){stage=1;fixture(mc,g->((TrafficGame)g).obstacles.add(new TrafficGame.Obstacle(((TrafficGame)g).lane,230,0)));}
                if(stage==1&&!active&&status==0){require(!complete,"crash completed");assertions++;next(mc);}return;}
            if(scenario==3){
                if(stage==1&&!active&&!complete)throw new IllegalStateException("whack ended below twenty");
                if(complete&&!active){assertions++;next(mc);return;}
                if(mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){var state=s.currentState();assertHard(state);
                    if(stage==0){capture(mc,"whack-start");stage=1;}
                    if(frame%10==0)GooseTools.LOGGER.info("WHACK_CLIENT score {} phase {} clock {} hole {} bornAgo {}",state.score(),state.phase(),state.clock(),state.actors()[0],state.actors()[1]);
                    if(state.phase()==GameSession.RUNNING&&state.actors()[0]>=0&&state.actors()[1]>170){long born=state.clock()-(long)state.actors()[1];if(born!=clickedBorn){clickedBorn=born;double[] xy=s.cellCenter((int)state.actors()[0]);click(s,xy[0],xy[1]);}}
                    if(state.score()>=10&&stage==0){capture(mc,"whack-hard");stage=1;}}
                return;
            }
            if(scenario==4||scenario==5){
                if(stage==0&&mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){stage=1;if(scenario==5)s.onClose();}
                if(stage==1&&!active&&status==0){require(!complete,"mole death/close completed");assertions++;next(mc);}return;
            }
            if(scenario>=6&&scenario<=11){
                if(complete&&!active){assertions++;next(mc);return;}
                if(stage==1&&!active&&status==0){require(!complete,"puzzle close completed");assertions++;next(mc);return;}
                if(!(mc.gui.screen() instanceof TaskScreen s)||s.currentState()==null||!s.currentState().started())return;
                if(scenario>=9){s.onClose();stage=1;return;}
                if(stage==0){capture(mc,s.taskType().id);stage=2;}
                solve(s);return;
            }
            if(scenario>=12){arcade(mc);}
        }catch(Throwable e){GooseTools.LOGGER.error("GUI map regression",e);finish(mc,"FAIL "+e);}
    }
    private void arcade(Minecraft mc){
        if(scenario==12){
            if(stage==0&&active){command(mc,"tp "+name(mc)+" -1668.5 72 -499.5");stage=1;return;}
            if(stage==1&&frame%8==0&&mc.player.getX()>-1669){use(mc,new BlockPos(-1669,72,-500));stage=2;return;}
            if(stage==2&&mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){require(s.gameType()==GameType.MERGE,"2048 block opened wrong game");assertions++;key(s,InputConstants.KEY_RETURN);stage=3;return;}
            if(stage==3&&time>=10_000&&mc.gui.screen() instanceof GameScreen s){s.onClose();pauseAt=now();pausedTime=time;stage=4;return;}
            if(stage==4&&now()-pauseAt>650){require(active&&time==pausedTime,"arcade close failed or counted time");assertions++;
                command(mc,"tp "+name(mc)+" -1673.5 72 -506.5");stage=5;return;}
            if(stage==5&&frame%8==0&&mc.player.getX()<-1673){use(mc,new BlockPos(-1674,72,-507));stage=6;return;}
            if(stage==6&&mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){require(s.gameType()==GameType.MINES,"mines block opened wrong game");assertions++;key(s,InputConstants.KEY_RETURN);stage=7;return;}
            if(stage==7&&complete&&!active){require(mc.gui.screen() instanceof GameScreen,"arcade completion closed game");assertions++;next(mc);}return;
        }
        if(scenario==13||scenario==14){
            int z=scenario==13?-504:-507;int x=scenario==13?-1669:-1670;
            if(stage==0&&active){command(mc,"tp "+name(mc)+" "+(x+1.5)+" 72 "+(z+.5));stage=1;return;}
            if(stage==1&&frame%8==0&&mc.player.getX()>x){use(mc,new BlockPos(x,72,z));stage=2;return;}
            if(stage==2&&mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){require(s.gameType()==(scenario==13?GameType.FLAPPY:GameType.SNAKE),"arcade wrong block mapping");assertions++;s.onClose();stage=3;return;}
            if(stage==3&&active){command(mc,"tp "+name(mc)+" -1665 72 -499");stage=4;return;}
            if(stage==4&&!active){require(!complete&&time==0,"arcade leave did not reset");assertions++;next(mc);}return;
        }
        if(scenario==15){
            if(stage==0&&active){command(mc,"goosetools games open "+name(mc)+" 2048");stage=1;return;}
            if(stage==1&&mc.gui.screen() instanceof GameScreen s&&s.currentState()!=null){key(s,InputConstants.KEY_RETURN);pauseAt=now();stage=2;return;}
            if(stage==2&&now()-pauseAt>700){require(time==0&&active,"command trial counted toward arcade");assertions++;next(mc);}return;
        }
        if(scenario==16){
            if(stage==0&&active){command(mc,"function ggd:task/eagleton_simplify/gui/clear_player");stage=1;return;}
            if(stage==1&&!active){require(!complete&&time==0,"cancel retained progress");assertions++;next(mc);}
        }
    }
    private void assertHard(GameSnapshot s){require(s.difficulty()==2,"challenge did not lock hard");require(s.phase()!=GameSession.MENU,"challenge requires manual start");}
    private static Object screenCall(TaskScreen s,String method){try{var m=TaskScreen.class.getDeclaredMethod(method);m.setAccessible(true);return m.invoke(s);}catch(Exception e){throw new IllegalStateException(e);}}
    private static TaskLayout layout(TaskScreen s){return (TaskLayout)screenCall(s,"taskLayout");}
    private static long elapsed(TaskScreen s){return (long)screenCall(s,"animationElapsed");}
    private void solve(TaskScreen s){
        var state=s.currentState();
        if(s.taskType()==TaskType.TIMING){double a=layout(s).timingAngle(elapsed(s));
            if(TaskLayout.distance(a,Math.round(a/120)*120)<12&&now()-lastHit>200){s.keyPressed(new KeyEvent(InputConstants.KEY_SPACE,0,0));lastHit=now();}}
        if(s.taskType()==TaskType.KNOBS){int k=state.progress();if(k>=3)return;double rad=Math.toRadians(layout(s).knobTargets[k]-90);
            double x=TaskLayout.knobX(k)+Math.cos(rad)*32,y=162+Math.sin(rad)*32;
            if(knob!=k){taskClick(s,x,y);knob=k;}taskDrag(s,x,y);}
        if(s.taskType()==TaskType.PIPES&&state.stage()==0&&frame%4==0){int cell=-1;
            for(int i=0;i<16;i++)if(((state.pipeBits()>>>(i*2))&3)!=0){cell=i;break;}
            if(cell<0)taskClick(s,345,250);else taskClick(s,TaskExtraLayout.PIPE_X+cell%4*40+20,TaskExtraLayout.PIPE_Y+cell/4*40+20);}
    }
    private void drive(GameScreen s){var state=s.currentState();if(state.phase()!=GameSession.RUNNING)return;
        double[] a=state.actors();double nearest=-1000;int lane=(int)a[1];
        for(int i=6;i<a.length;i+=3)if(a[i+1]<265)nearest=Math.max(nearest,a[i+1]);
        boolean[] occupied=new boolean[3];for(int i=6;i<a.length;i+=3)if(Math.abs(a[i+1]-nearest)<2)occupied[(int)a[i]]=true;
        if(nearest> -1000&&occupied[lane]){int target=0;while(occupied[target])target++;key(s,target>lane?InputConstants.KEY_RIGHT:InputConstants.KEY_LEFT);}}
    private void prepare(Minecraft mc){try{
        MinecraftServer server=mc.getSingleplayerServer();Path root=Path.of(System.getProperty("goosetools.guiDatapack"));
        Path dest=server.getWorldPath(LevelResource.ROOT).resolve("datapacks/gui-tasks");Files.createDirectories(dest);
        Files.copy(root.resolve("pack.mcmeta"),dest.resolve("pack.mcmeta"));
        for(String id:java.util.stream.Stream.concat(Arrays.stream(IDS),java.util.stream.Stream.of("gui")).toList()){
            Path src=root.resolve("data/ggd/function/task/eagleton_simplify/"+id);
            try(var files=Files.walk(src)){for(Path p:files.filter(Files::isRegularFile).toList()){
                Path target=dest.resolve(root.relativize(p));Files.createDirectories(target.getParent());Files.copy(p,target);}}
        }
        Path adv=dest.resolve("data/mainsys/advancement/test/ifinvchange.json");Files.createDirectories(adv.getParent());
        Files.writeString(adv,"{\"criteria\":{\"test\":{\"trigger\":\"minecraft:impossible\"}}}");
        Path load=dest.resolve("data/minecraft/tags/function/load.json");Files.createDirectories(load.getParent());Files.writeString(load,"{\"values\":[\"ggd:task/eagleton_simplify/gui/load\"]}");
        Path tick=dest.resolve("data/minecraft/tags/function/tick.json");Files.writeString(tick,"{\"values\":[\"gui_check:tick\"]}");
        Path fn=dest.resolve("data/gui_check/function/tick.mcfunction");Files.createDirectories(fn.getParent());
        StringBuilder f=new StringBuilder();for(int i=0;i<IDS.length;i++){
            String[] camel={"TestDrive","UnclogPipes","CalibrateVoltage","PowerControl","ArcadeFan","WhackMoles"};
            f.append("execute as @a[tag=inTask").append(camel[i]).append("] at @s run function ggd:task/eagleton_simplify/").append(IDS[i]).append("/checking\n");}
        Files.writeString(fn,f.toString());server.getPackRepository().reload();var packs=new ArrayList<>(server.getPackRepository().getSelectedIds());packs.add("file/gui-tasks");
        server.reloadResources(packs).whenComplete((v,error)->server.execute(()->{
            if(error!=null){mc.execute(()->finish(mc,"FAIL reload "+error));return;}
            exec(server,"scoreboard objectives add gamesetting dummy");exec(server,"scoreboard objectives add ggdSession dummy");
            exec(server,"scoreboard players set map gamesetting 11");exec(server,"scoreboard players set #MeetingPhase ggdSession 0");
            exec(server,"tag "+name(mc)+" add players");exec(server,"tag "+name(mc)+" add gamingGGD");
            for(int[] c:COORDS){exec(server,"setblock "+c[0]+" "+(c[1]-1)+" "+c[2]+" stone");}
            for(int[] c:new int[][]{{-1669,72,-500},{-1669,72,-504},{-1670,72,-507},{-1674,72,-507}})exec(server,"setblock "+c[0]+" "+c[1]+" "+c[2]+" stone");
            serverReady=true;
        }));
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL prepare "+e));}}
    private void begin(Minecraft mc,int id){mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();ServerPlayer p=server.getPlayerList().getPlayer(mc.player.getUUID());GuiTaskBridge.clear(p);
        for(String name:IDS){p.removeTag("task."+name+".finished");p.removeTag("task."+name+".available");}
        exec(server,"execute as "+name(mc)+" run function ggd:task/eagleton_simplify/gui/clear_player");
        int[] c=COORDS[id];
        for(int cx=-1702>>4;cx<=(-1600>>4);cx++)for(int cz=-542>>4;cz<=(-478>>4);cz++)p.level().getChunk(cx,cz);
        exec(server,"fill -1702 71 -510 -1667 71 -481 stone");
        exec(server,"setblock "+c[0]+" "+(c[1]-1)+" "+c[2]+" stone");
        for(int[] b:new int[][]{{-1669,72,-500},{-1669,72,-504},{-1670,72,-507},{-1674,72,-507}})exec(server,"setblock "+b[0]+" "+b[1]+" "+b[2]+" stone");
        exec(server,"tp "+name(mc)+" "+c[0]+" "+c[1]+" "+c[2]);
        exec(server,"kill @e[type=marker,tag=gui_fixture]");exec(server,"summon marker "+c[0]+" "+c[1]+" "+c[2]+" {Tags:[\"task\",\""+IDS[id]+"\",\"gui_fixture\"]}");
        p.addTag("task."+IDS[id]+".available");exec(server,"execute as "+name(mc)+" at @s run function ggd:task/eagleton_simplify/"+IDS[id]+"/accept");
    });}
    private void sample(Minecraft mc){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        var sb=mc.getSingleplayerServer().getScoreboard();var a=sb.getPlayerScoreInfo(p,sb.getObjective("ggdGuiState"));var b=sb.getPlayerScoreInfo(p,sb.getObjective("ggdGuiTime"));
        status=a==null?0:a.value();time=b==null?0:b.value();active=p.entityTags().contains("inTaskGooseGui");complete=Arrays.stream(IDS).anyMatch(id->p.entityTags().contains("task."+id+".finished"));});}
    private void fixture(Minecraft mc,java.util.function.Consumer<ArcadeGame> action){mc.getSingleplayerServer().execute(()->{try{Field f=GameServer.class.getDeclaredField("sessions");f.setAccessible(true);
        Object playing=((Map<?,?>)f.get(null)).get(mc.player.getUUID());Field gf=playing.getClass().getDeclaredField("game");gf.setAccessible(true);action.accept(((GameSession)gf.get(playing)).game);
    }catch(Throwable e){mc.execute(()->finish(mc,"FAIL fixture "+e));}});}
    private void next(Minecraft mc){if(mc.gui.screen() instanceof GameScreen s)s.onClose();if(mc.gui.screen() instanceof TaskScreen s)s.onClose();GooseTools.LOGGER.info("GUI_MAP_PASS scenario {}",scenario);scenario++;requested=false;since=now();}
    private static String name(Minecraft mc){return mc.player.getName().getString();}
    private static void exec(MinecraftServer s,String c){s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),c);}
    private static void command(Minecraft mc,String c){mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();exec(server,"execute as "+name(mc)+" at @s run "+c);});}
    private static void use(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));}
    private static void key(GameScreen s,int key){var e=new KeyEvent(key,0,0);s.keyPressed(e);s.keyReleased(e);}
    private static void click(GameScreen s,double x,double y){s.mouseClicked(new MouseButtonEvent(s.transform().x()+x*s.transform().scale(),s.transform().y()+y*s.transform().scale(),new MouseButtonInfo(0,0)),false);}
    private static double scale(TaskScreen s){return Math.max(.1,Math.min(1.5,Math.min((s.width-24.0)/420,(s.height-24.0)/320)));}
    private static double tx(TaskScreen s,double x){double z=scale(s);return(s.width-420*z)/2+x*z;}
    private static double ty(TaskScreen s,double y){double z=scale(s);return(s.height-320*z)/2+y*z;}
    private static void taskClick(TaskScreen s,double x,double y){s.mouseClicked(new MouseButtonEvent(tx(s,x),ty(s,y),new MouseButtonInfo(0,0)),false);}
    private static void taskDrag(TaskScreen s,double x,double y){s.mouseDragged(new MouseButtonEvent(tx(s,x),ty(s,y),new MouseButtonInfo(0,0)),0,0);}
    private static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    private void capture(Minecraft mc,String n){Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot "+e));}});}
    private void finish(Minecraft mc,String text){if(finished)return;finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),text);}catch(Exception e){GooseTools.LOGGER.error("Result",e);}mc.stop();}
}
