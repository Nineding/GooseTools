package com.goosethings.tools.game;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.game.GameScreen;
import com.goosethings.tools.client.game.GameSoundCues;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.goosethings.tools.client.task.TaskScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public final class GameGuiRegression implements ClientModInitializer {
    private boolean world, language, languageReady, requested, finished, fixturePending, fixtureReady;
    private int scenario=Boolean.getBoolean("goosetools.trafficOnly")?GameType.TRAFFIC.ordinal():0;
    private int stage, ticks, frame, captures, lastHead=-1, cleanup;
    private long since, pauseAt, pauseElapsed;
    private volatile int[] safeCells;
    private volatile int mineCell=-1;
    private boolean audioReady;
    private final Set<String> heardCues = new HashSet<>();
    private final Map<String,Long> captureSince=new HashMap<>();
    private final Set<String> capturedNames=new HashSet<>();
    @Override public void onInitializeClient() { if(Boolean.getBoolean("goosetools.gameRegressionTest")) ClientTickEvents.END_CLIENT_TICK.register(this::tick); }
    private static long now(){return System.nanoTime()/1_000_000;}
    private void tick(Minecraft mc) {
        if(finished||!mc.isGameLoadFinished()) return;
        try {
            if (!audioReady) {
                audioReady = true;
                mc.getSoundManager().addListener((sound, event, distance) -> {
                    Identifier id = sound.getIdentifier();
                    if (id.getNamespace().equals("goosetools") && id.getPath().startsWith("game.")) {
                        require(sound.getSource() == SoundSource.UI, "arcade sound volume category");
                        heardCues.add(id.getPath());
                    }
                });
                for (GameSoundCues.Cue cue : GameSoundCues.Cue.values()) {
                    Identifier id = Identifier.fromNamespaceAndPath("goosetools", "game." + cue.path());
                    require(mc.getSoundManager().getSoundEvent(id) != null, "missing arcade sound " + id);
                    require(mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("goosetools", "sounds/game/" + cue.path() + ".ogg")).isPresent(), "missing sound file " + id);
                }
            }
            if(!world) {
                if(!language) {language=true;mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks().whenComplete((v,e)->mc.execute(()->{if(e!=null)finish(mc,"FAIL language "+e);else languageReady=true;}));return;}
                if(!languageReady)return;world=true;mc.options.pauseOnLostFocus=false;mc.getWindow().setWindowed(1280,900);
                mc.createWorldOpenFlows().createFreshLevel("arcade-"+System.currentTimeMillis(),new LevelSettings("Arcade regression",net.minecraft.world.level.GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(42,false,false),
                        provider->provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),null);return;
            }
            if(mc.player==null||!mc.player.connection.hasClientLoaded()||mc.getSingleplayerServer()==null||++ticks<40)return;
            if(scenario>=GameType.values().length){cleanup(mc);return;}
            GameType type=GameType.values()[scenario];
            if(!requested){requested=true;stage=0;frame=0;since=now();lastHead=-1;fixturePending=fixtureReady=false;mc.options.guiScale().set(new int[]{2,3,1}[scenario%3]);mc.resizeGui();command(mc,"goosetools games open "+mc.player.getName().getString()+" "+type.id);return;}
            require(now()-since<90_000,"timeout "+type+" stage="+stage);
            if(!(mc.gui.screen() instanceof GameScreen s)||s.gameType()!=type||s.currentState()==null)return;
            frame++;GameSnapshot state=s.currentState();
            require(!s.isPauseScreen(),"Minecraft paused");
            if(stage==0&&frame>=8){if(!capture(mc,type.id+"-menu"))return;click(s,300,280,0,false);stage=1;return;}
            if(stage==1&&state.phase()==GameSession.RUNNING){if(!capture(mc,type.id))return;key(s,InputConstants.KEY_P);stage=2;return;}
            if(stage==2&&state.phase()==GameSession.PAUSED){pauseAt=now();pauseElapsed=state.elapsed();stage=3;return;}
            if(stage==3){if(now()-pauseAt<550)return;require(state.elapsed()==pauseElapsed,"pause counted as playing "+type);key(s,InputConstants.KEY_P);stage=4;return;}
            if(stage<4||state.phase()==GameSession.PAUSED)return;
            switch(type) {
                case FLAPPY -> {
                    if(state.phase()==GameSession.RUNNING&&state.elapsed()>=2300)capture(mc,"flappy-playing");
                    if(state.phase()==GameSession.RUNNING&&state.score()<1) {
                        double[] a=state.actors();double target=120;
                        for(int p=0;p<3;p++)if(a[2+p*2]+34>=38){target=a[3+p*2];break;}
                        if(a[1]>-40&&a[0]>target-4) key(s,InputConstants.KEY_SPACE);
                    } else if(state.phase()==GameSession.LOST){require(state.score()>=1,"Flappy bot failed before one pipe");if(!capture(mc,"flappy-over"))return;done(s);}
                }
                case SNAKE -> {
                    if(state.phase()==GameSession.RUNNING&&state.score()<1){int[] b=state.board();int head=-1,food=-1;for(int i=0;i<b.length;i++){if(b[i]==2)head=i;if(b[i]==3)food=i;}
                        if(head!=lastHead){lastHead=head;int direction=pathDirection(b,head,food,state.cols(),state.rows(),(int)state.actors()[0]);if(direction>=0)arrow(s,direction);}
                    } else if(state.phase()==GameSession.LOST){require(state.score()>=1,"snake did not eat");if(!capture(mc,"snake-over"))return;done(s);}
                }
                case PONG -> pong(mc,s,state);
                case WHACK -> {
                    if(state.phase()==GameSession.RUNNING&&state.score()>=3&&state.actors()[0]>=0)capture(mc,"whack-playing");
                    if(state.phase()==GameSession.RUNNING&&state.score()<3&&state.actors()[1]>170){int hole=(int)state.actors()[0];if(hole>=0){double[] p=s.cellCenter(hole);click(s,p[0],p[1],0,false);}}
                    else if(state.phase()==GameSession.LOST){require(state.score()>=3,"whack bot failed");if(!capture(mc,"whack-over"))return;done(s);}
                }
                case MINES -> mines(mc,s,state);
                case MERGE -> merge(mc,s,state);
                case TRAFFIC -> {
                    if(stage==4){s.keyPressed(new KeyEvent(InputConstants.KEY_S,0,0));pauseAt=now();stage=5;return;}
                    if(stage==5){if(state.actors()[4]!=1||now()-pauseAt<600)return;require(state.actors()[2]<70,"held brake did not reduce speed");if(!capture(mc,"traffic-braking"))return;s.keyReleased(new KeyEvent(InputConstants.KEY_S,0,0));stage=6;return;}
                    if(state.phase()==GameSession.RUNNING&&state.opponent()<3){
                        double[] a=state.actors();double nearest=-1000;int lane=(int)a[1];
                        for(int i=6;i<a.length;i+=3)if(a[i+1]<270)nearest=Math.max(nearest,a[i+1]);
                        boolean[] occupied=new boolean[3];for(int i=6;i<a.length;i+=3)if(Math.abs(a[i+1]-nearest)<2)occupied[(int)a[i]]=true;
                        if(nearest> -1000&&occupied[lane]){int target=0;while(occupied[target])target++;arrow(s,target>lane?1:3);}
                        if(state.opponent()>0)capture(mc,"traffic-playing");
                    }else if(state.phase()==GameSession.LOST){require(state.opponent()>=3,"traffic dodged too few obstacles");if(!capture(mc,"traffic-over"))return;done(s);}
                }
            }
        }catch(Throwable e){GooseTools.LOGGER.error("Arcade regression failed",e);finish(mc,"FAIL "+e);}
    }
    private void pong(Minecraft mc,GameScreen s,GameSnapshot state) {
        if(stage==4&&state.elapsed()<1800){s.mouseMoved(screenX(s,300),screenY(s,59+state.actors()[1]*1.2));return;}
        if(stage==4){fixture(mc,g->{PongGame p=(PongGame)g;p.clock=Math.max(1500,p.clock);p.score=10;p.x=405;p.vx=200;});stage=5;return;}
        if(stage==5&&fixtureReady&&state.phase()==GameSession.WON){require(state.score()==11,"Pong final score");if(!capture(mc,"pong-won"))return;click(s,300,241,0,false);stage=6;return;}
        if(stage==6&&state.phase()==GameSession.MENU){click(s,300,196,0,false);stage=7;return;}
        if(stage==7&&state.mode()==1){click(s,300,280,0,false);stage=8;fixtureReady=false;return;}
        if(stage==8&&state.phase()==GameSession.RUNNING){fixture(mc,g->{PongGame p=(PongGame)g;p.clock=Math.max(1500,p.clock);p.score=10;p.x=405;p.vx=200;});stage=9;return;}
        if(stage==9&&fixtureReady&&state.score()>=11){require(state.phase()==GameSession.RUNNING,"endless Pong ended at 11");if(!capture(mc,"pong-endless"))return;done(s);}
    }
    private void mines(Minecraft mc,GameScreen s,GameSnapshot state) {
        if(stage==4){double[] p=s.cellCenter(40);click(s,p[0],p[1],0,false);stage=5;return;}
        if(stage==5&&state.score()>0){if(!capture(mc,"mines-open"))return;fixture(mc,g->{MinesGame m=(MinesGame)g;safeCells=java.util.stream.IntStream.range(0,m.mines.length).filter(c->!m.mines[c]).toArray();mineCell=java.util.stream.IntStream.range(0,m.mines.length).filter(c->m.mines[c]).findFirst().orElseThrow();});stage=6;return;}
        if(stage==6&&fixtureReady){double[] p=s.cellCenter(mineCell);click(s,p[0],p[1],1,false);stage=7;return;}
        if(stage==7&&state.board()[mineCell]==-2){double[] p=s.cellCenter(mineCell);click(s,p[0],p[1],1,false);stage=8;return;}
        if(stage==8&&state.board()[mineCell]==9){double[] p=s.cellCenter(mineCell);click(s,p[0],p[1],1,false);stage=9;return;}
        if(stage==9&&state.board()[mineCell]==-1){double[] p=s.cellCenter(mineCell);click(s,p[0],p[1],0,false);stage=10;return;}
        if(stage==10&&state.phase()==GameSession.LOST){if(!capture(mc,"mines-over"))return;click(s,300,241,0,false);stage=11;return;}
        if(stage==11&&state.phase()==GameSession.MENU){click(s,300,280,0,false);stage=12;return;}
        if(stage==12&&state.phase()==GameSession.RUNNING){double[] p=s.cellCenter(40);click(s,p[0],p[1],0,false);stage=13;return;}
        if(stage==13&&state.score()>0){fixture(mc,g->{MinesGame m=(MinesGame)g;safeCells=java.util.stream.IntStream.range(0,m.mines.length).filter(c->!m.mines[c]).toArray();});stage=14;return;}
        if(stage==14&&fixtureReady){
            if(state.phase()==GameSession.WON){if(!capture(mc,"mines-won"))return;click(s,300,198,0,false);stage=15;return;}
            if(frame%2==0)for(int c:safeCells)if(state.board()[c]==-1){double[] p=s.cellCenter(c);click(s,p[0],p[1],0,false);break;}
        }
        if(stage==15&&state.wins()==1&&state.phase()==GameSession.RUNNING){click(s,300,79,0,false);stage=16;return;}
        if(stage==16&&state.phase()==GameSession.MENU){click(s,300,230,0,false);stage=17;return;}
        if(stage==17&&state.difficulty()==1){click(s,300,280,0,false);stage=18;return;}
        if(stage==18&&state.phase()==GameSession.RUNNING){if(!capture(mc,"mines-intermediate"))return;click(s,300,79,0,false);stage=19;return;}
        if(stage==19&&state.phase()==GameSession.MENU){click(s,300,230,0,false);stage=20;return;}
        if(stage==20&&state.difficulty()==2){click(s,300,280,0,false);stage=21;return;}
        if(stage==21&&state.phase()==GameSession.RUNNING){if(!capture(mc,"mines-expert"))return;done(s);}
    }
    private void merge(Minecraft mc,GameScreen s,GameSnapshot state) {
        if(stage==4){fixture(mc,g->{MergeGame m=(MergeGame)g;Arrays.fill(m.cells,0);m.cells[0]=m.cells[1]=10;m.clock=Math.max(m.clock,1000);});stage=5;return;}
        if(stage==5&&fixtureReady){arrow(s,3);stage=6;return;}
        if(stage==6&&state.phase()==GameSession.WON){require(state.board()[0]==11,"2048 missing");if(!capture(mc,"2048-won"))return;click(s,300,198,0,false);stage=7;return;}
        if(stage==7&&state.phase()==GameSession.RUNNING){fixture(mc,g->{MergeGame m=(MergeGame)g;Arrays.fill(m.cells,0);m.cells[0]=m.cells[1]=11;m.clock+=200;});stage=8;return;}
        if(stage==8&&fixtureReady){arrow(s,3);stage=9;return;}
        if(stage==9&&state.board()[0]==12){require(state.phase()==GameSession.RUNNING,"continued 2048 ended");if(!capture(mc,"2048-4096"))return;fixture(mc,g->{MergeGame m=(MergeGame)g;int[] b={1,1,3,4,5,6,7,8,9,10,12,13,14,15,16,17};System.arraycopy(b,0,m.cells,0,16);m.clock+=200;});stage=10;return;}
        if(stage==10&&fixtureReady){arrow(s,3);stage=11;return;}
        if(stage==11&&state.phase()==GameSession.LOST){if(!capture(mc,"2048-over"))return;done(s);}
    }
    private void fixture(Minecraft mc,Consumer<ArcadeGame> action) {
        fixtureReady=false;mc.getSingleplayerServer().execute(()->{
            try {Field mapField=GameServer.class.getDeclaredField("sessions");mapField.setAccessible(true);Map<?,?> map=(Map<?,?>)mapField.get(null);Object playing=map.get(mc.player.getUUID());
                Field gameField=playing.getClass().getDeclaredField("game");gameField.setAccessible(true);GameSession session=(GameSession)gameField.get(playing);action.accept(session.game);mc.execute(()->fixtureReady=true);
            }catch(Throwable e){mc.execute(()->finish(mc,"FAIL fixture "+e));}
        });
    }
    private static int pathDirection(int[] b,int head,int food,int cols,int rows,int direction) {
        int[] first=new int[b.length];Arrays.fill(first,-1);ArrayDeque<Integer>q=new ArrayDeque<>();q.add(head);first[head]=4;
        while(!q.isEmpty()){int c=q.removeFirst();for(int d=0;d<4;d++) {if(c==head&&d==(direction+2)%4)continue;int x=c%cols+(d==1?1:d==3?-1:0),y=c/cols+(d==2?1:d==0?-1:0);if(x<0||x>=cols||y<0||y>=rows)continue;int n=y*cols+x;if(b[n]==1||first[n]>=0)continue;first[n]=c==head?d:first[c];if(n==food)return first[n];q.add(n);}}
        return -1;
    }
    private void done(GameScreen s){
        try {
            Field revision = GameScreen.class.getDeclaredField("revision"); revision.setAccessible(true);
            long current = revision.getLong(s);
            GameSnapshot original = s.currentState();
            GameSnapshot stale = new GameSnapshot(GameSession.RUNNING, original.mode(), original.difficulty(),
                    original.cols(), original.rows(), original.score(), original.opponent(), original.lives(),
                    original.elapsed(), original.clock(), original.event() + 1, 6, original.detail(),
                    original.best(), original.bestTime(), original.wins(), original.board(), original.actors(), original.moves());
            s.apply(new GamePackets.State(s.sessionId(), current, stale));
            require(s.currentState() == original, "duplicate revision was accepted");
            s.apply(new GamePackets.State(s.sessionId(), current - 1, stale));
            require(s.currentState() == original, "out-of-order revision was accepted");
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        s.onClose();scenario++;requested=false;
    }
    private void cleanup(Minecraft mc) {
        if(++frame%8!=0)return;String target=mc.player.getName().getString();
        switch(cleanup) {
            case 0->{command(mc,"goosetools games open "+target+" snake");cleanup++;}
            case 1->{if(!(mc.gui.screen() instanceof GameScreen))return;command(mc,"goosetools tasks open "+target+" sorting");cleanup++;}
            case 2->{if(!(mc.gui.screen() instanceof TaskScreen))return;command(mc,"goosetools games open "+target+" pong");cleanup++;}
            case 3->{if(!(mc.gui.screen() instanceof GameScreen s))return;key(s,InputConstants.KEY_ESCAPE);cleanup++;}
            case 4->{require(!(mc.gui.screen() instanceof GameScreen),"ESC cleanup");command(mc,"goosetools games open "+target+" whack");cleanup++;}
            case 5->{if(!(mc.gui.screen() instanceof GameScreen))return;command(mc,"goosetools games close "+target);cleanup++;}
            case 6->{require(!(mc.gui.screen() instanceof GameScreen),"admin close");command(mc,"goosetools games open "+target+" flappy");cleanup++;}
            case 7->{if(!(mc.gui.screen() instanceof GameScreen))return;command(mc,"scoreboard objectives add ggdSession dummy");command(mc,"tag "+target+" add gamingGGD");command(mc,"scoreboard players set #MeetingPhase ggdSession 1");cleanup++;}
            case 8->{require(!(mc.gui.screen() instanceof GameScreen),"meeting cleanup");command(mc,"scoreboard players set #MeetingPhase ggdSession 0");command(mc,"tag "+target+" remove gamingGGD");command(mc,"goosetools games open "+target+" 2048");cleanup++;}
            case 9->{if(!(mc.gui.screen() instanceof GameScreen))return;command(mc,"kill "+target);cleanup++;}
            case 10->{require(!(mc.gui.screen() instanceof GameScreen),"death cleanup");if(captures<(Boolean.getBoolean("goosetools.trafficOnly")?5:20))return;finish(mc,(Boolean.getBoolean("goosetools.trafficOnly")?"PASS Traffic held brake/release, steering, dodging, pause, collision and cleanup":"PASS seven arcade games through real commands, packets and keys/mouse; Chinese; GUI scales 1/2/3; natural Flappy/Snake/Whack/Traffic scores and loss; Pong classic/endless; mine flag cycle/victory/next/difficulties; 2048 win/4096/loss; pause exclusion; task replacement; ESC/admin/meeting/death")+"; "+captures+" GPU screenshots");}
        }
    }
    private boolean capture(Minecraft mc,String name){if(capturedNames.contains(name))return true;if(now()-captureSince.computeIfAbsent(name,k->now())<300)return false;capturedNames.add(name);Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(mc.gameDirectory.toPath().resolve("game-"+name+".png"));captures++;}catch(Exception e){mc.execute(()->finish(mc,"FAIL screenshot "+e));}});return true;}
    private static double screenX(GameScreen s,double x){return s.transform().x()+x*s.transform().scale();}
    private static double screenY(GameScreen s,double y){return s.transform().y()+y*s.transform().scale();}
    private static void click(GameScreen s,double x,double y,int button,boolean doubleClick){s.mouseClicked(new MouseButtonEvent(screenX(s,x),screenY(s,y),new MouseButtonInfo(new int[]{InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOUSE_BUTTON_RIGHT,InputConstants.MOUSE_BUTTON_MIDDLE}[button],0)),doubleClick);}
    private static void key(GameScreen s,int key){KeyEvent e=new KeyEvent(key,0,0);s.keyPressed(e);s.keyReleased(e);}
    private static void arrow(GameScreen s,int d){key(s,new int[]{InputConstants.KEY_UP,InputConstants.KEY_RIGHT,InputConstants.KEY_DOWN,InputConstants.KEY_LEFT}[d]);}
    private static void command(Minecraft mc,String cmd){mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),cmd);});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private void finish(Minecraft mc,String message){
        if(finished)return;
        if (message.startsWith("PASS ")) {
            Set<String> required = Boolean.getBoolean("goosetools.trafficOnly")
                    ? Set.of("game.start", "game.pause", "game.resume", "game.crash", "game.lane", "game.brake")
                    : Set.of("game.start", "game.pause", "game.resume", "game.flap", "game.eat",
                    "game.whack_hit", "game.reveal", "game.flag", "game.merge", "game.crash", "game.win", "game.lane", "game.brake");
            if (!heardCues.containsAll(required)) { Set<String> missing = new HashSet<>(required); missing.removeAll(heardCues); message = "FAIL arcade sounds not played: " + missing; }
            else {
                // Exercise decoding/playback of less frequent cues as well as resource registration.
                for (GameSoundCues.Cue cue : GameSoundCues.Cue.values()) {
                    SoundEvent event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("goosetools", "game." + cue.path()));
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1, .25F));
                }
                if (heardCues.size() != GameSoundCues.Cue.values().length) message = "FAIL incomplete arcade playback: " + heardCues;
                else message += "; 18 registered audio cues played via UI category; real game feedback and stale-packet rejection verified";
            }
        }
        finished=true;try{Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"),message);}catch(Exception e){GooseTools.LOGGER.error("Arcade result",e);}mc.stop();
    }
}
