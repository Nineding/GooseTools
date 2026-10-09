package com.goosethings.tools.game;

import com.goosethings.tools.task.ArcadeStations;
import com.goosethings.tools.task.GuiTaskBridge;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/** Controlled server-player fixtures verify binding isolation; these do not bypass production handshakes. */
final class GuiBindingRegression {
    private static int assertions;
    static int run(MinecraftServer server) throws Exception {
        assertions=0;
        ServerPlayer a=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"GuiBindA"),ClientInformation.createDefault());
        ServerPlayer b=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"GuiBindB"),ClientInformation.createDefault());
        for(ServerPlayer p:new ServerPlayer[]{a,b}){p.addTag("players");p.addTag("inTaskGooseGui");p.setPos(-1673,72,-502);}
        Field field=GuiTaskBridge.class.getDeclaredField("bindings");field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID,Object> bindings=(Map<UUID,Object>)field.get(null);
        Class<?> binding=Class.forName("com.goosethings.tools.task.GuiTaskBridge$Binding");
        var ctor=binding.getDeclaredConstructor(String.class,long.class);ctor.setAccessible(true);
        try {
            bindings.put(a.getUUID(),ctor.newInstance("traffic",9001L));bindings.put(b.getUUID(),ctor.newInstance("traffic",9002L));
            set(a,"ggdGuiState",1);set(b,"ggdGuiState",1);
            GameSession drive=new GameSession(9001,GameType.TRAFFIC,42,0);drive.startChallenge(0);((TrafficGame)drive.game).nextWave=1_000_000;
            GameSession other=new GameSession(9002,GameType.TRAFFIC,42,0);other.startChallenge(0);
            GuiTaskBridge.taskProgress(a,9001,true);check(get(a,"ggdGuiState")==1,"task/game IDs collide");
            for(int i=1;i<=120;i++)drive.tick(i*250L);
            GuiTaskBridge.gameProgress(a,drive,false);GuiTaskBridge.gameProgress(b,other,false);
            check(get(a,"ggdGuiState")==2,"driving did not complete");check(get(b,"ggdGuiState")==1,"other player completed");
            check(get(a,"ggdGuiTime")==30_000,"driving time");check(get(b,"ggdGuiTime")==0,"other player time");
            GuiTaskBridge.gameClosed(a,9001);check(get(a,"ggdGuiState")==2,"close undid success");
            GuiTaskBridge.gameClosed(b,9999);check(get(b,"ggdGuiState")==1,"stale close failed new session");
            GuiTaskBridge.gameClosed(b,9002);check(get(b,"ggdGuiState")==3,"early close did not fail");
            bindings.put(b.getUUID(),ctor.newInstance("pipes",9001L));set(b,"ggdGuiState",1);
            GuiTaskBridge.gameProgress(b,drive,false);check(get(b,"ggdGuiState")==1,"game completed puzzle with shared numeric ID");
            GuiTaskBridge.taskProgress(b,9000,true);check(get(b,"ggdGuiState")==1,"stale task completed new binding");
            GuiTaskBridge.taskProgress(b,9001,true);check(get(b,"ggdGuiState")==2,"puzzle did not complete");
            GuiTaskBridge.taskClosed(b,9001);check(get(b,"ggdGuiState")==2,"puzzle close undid success");
            bindings.put(b.getUUID(),ctor.newInstance("arcade",0L));set(b,"ggdGuiState",1);set(b,"ggdGuiTime",0);
            GameSession first=new GameSession(9010,GameType.MERGE,42,0);first.apply(0,GameSession.START,0,0,0,0);
            for(int i=1;i<=40;i++)first.tick(i*250L);
            GuiTaskBridge.gameProgress(b,first,false);check(get(b,"ggdGuiTime")==0,"command game counted");
            GuiTaskBridge.gameProgress(b,first,true);check(get(b,"ggdGuiTime")==10_000,"station time missing");
            GuiTaskBridge.gameProgress(b,first,true);check(get(b,"ggdGuiTime")==10_000,"time counted twice");
            GuiTaskBridge.gameClosed(b,9010);check(get(b,"ggdGuiState")==1,"station close failed arcade");
            first.apply(1,GameSession.PAUSE,0,0,0,10_000);first.tick(80_000);GuiTaskBridge.gameProgress(b,first,true);
            check(get(b,"ggdGuiTime")==10_000,"pause counted");
            GameSession second=new GameSession(9011,GameType.MERGE,84,0);second.apply(0,GameSession.START,0,0,0,0);
            for(int i=1;i<=80;i++)second.tick(i*250L);
            GuiTaskBridge.gameProgress(b,second,true);check(get(b,"ggdGuiState")==2,"cumulative game switch did not complete");
            check(get(b,"ggdGuiTime")==30_000,"cumulative time");
            check(ArcadeStations.typeAt(-1669,72,-500)==GameType.MERGE,"2048 mapping");
            check(ArcadeStations.typeAt(-1669,72,-504)==GameType.FLAPPY,"flappy mapping");
            check(ArcadeStations.typeAt(-1670,72,-507)==GameType.SNAKE,"snake mapping");
            check(ArcadeStations.typeAt(-1674,72,-507)==GameType.MINES,"mines mapping");
            check(ArcadeStations.typeAt(-1668,72,-500)==null,"neighbor block mapped");
            check(ArcadeStations.typeAt(-1669,73,-500)==null,"wrong height mapped");
            check(ArcadeStations.insideArcade(-1702,71,-510)&&ArcadeStations.insideArcade(-1667,82,-481),"closed bounds");
            for(double[] p:new double[][]{{-1702.001,72,-500},{-1666.999,72,-500},{-1673,70.999,-500},{-1673,82.001,-500},{-1673,72,-510.001},{-1673,72,-480.999}})
                check(!ArcadeStations.insideArcade(p[0],p[1],p[2]),"outside boundary accepted");
            return assertions;
        } finally {bindings.remove(a.getUUID());bindings.remove(b.getUUID());}
    }
    private static int get(ServerPlayer p,String key){var sb=p.level().getServer().getScoreboard();var score=sb.getPlayerScoreInfo(p,sb.getObjective(key));return score==null?0:score.value();}
    private static void set(ServerPlayer p,String key,int value){var sb=p.level().getServer().getScoreboard();sb.getOrCreatePlayerScore(p,sb.getObjective(key)).set(value);}
    private static void check(boolean condition,String message){assertions++;if(!condition)throw new IllegalStateException(message);}
}
