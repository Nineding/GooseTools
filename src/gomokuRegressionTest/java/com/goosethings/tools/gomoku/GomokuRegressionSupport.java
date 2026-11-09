package com.goosethings.tools.gomoku;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.nio.file.*;
import java.util.Set;

/** Isolated test mod; excluded from the runtime JAR. Fixtures do not bypass play packets. */
public final class GomokuRegressionSupport implements ModInitializer {
    public static Path output() { return Path.of(System.getProperty("goosetools.gomokuOutput")); }
    public record Control(int operation) implements CustomPacketPayload {
        public static final Type<Control> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools_gomoku_test", "control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC = StreamCodec.of((b,v)->b.writeInt(v.operation), b->new Control(b.readInt()));
        @Override public Type<Control> type() {return TYPE;}
    }
    @Override public void onInitialize() {
        if (!Boolean.getBoolean("goosetools.gomokuRegression")) return;
        PayloadTypeRegistry.serverboundPlay().register(Control.TYPE, Control.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Control.TYPE, (packet, ctx) -> ctx.server().execute(() -> {
            if (!ctx.player().getScoreboardName().equals("GomokuA")) return;
            try { fixture(ctx.server(), packet.operation); }
            catch(Exception e) {throw new RuntimeException(e);}
        }));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            command(server,"whitelist off");
            command(server,"scoreboard objectives add ggdGameId dummy");
            command(server,"scoreboard objectives add ggdSession dummy");
            command(server,"scoreboard players set #Global ggdGameId 0");
            command(server,"scoreboard players set #MeetingPhase ggdSession 0");
            try {Files.createDirectories(output());Files.writeString(output().resolve("server-ready.txt"),"Dedicated server initialized without client classes");}catch(Exception e){throw new RuntimeException(e);}
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)-> {
            handler.player.addTag("lobby");
            handler.player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        });
    }
    private static void command(MinecraftServer server,String command) {server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private static void fixture(MinecraftServer server,int operation) throws Exception {
        switch(operation) {
            case 1 -> {
                for(var p:server.getPlayerList().getPlayers()) {
                    p.removeTag("lobby");for(String tag:Set.of("players","gamingGGD","spectator","deadInMap","inTalk"))p.addTag(tag);
                    command(server,"scoreboard players operation "+p.getScoreboardName()+" ggdGameId = #Global ggdGameId");
                    if(p.getScoreboardName().equals("GomokuA")){p.addTag("dlcGhostActive");p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.setNoGravity(true);}
                    else {p.removeTag("players");p.addTag("ggdLateSpectator");p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);}
                }
                command(server,"scoreboard players set #MeetingPhase ggdSession 1");
            }
            case 2 -> command(server,"execute in minecraft:the_end run tp GomokuA 0 100 0");
            case 3 -> {command(server,"scoreboard players set #MeetingPhase ggdSession 0");for(var p:server.getPlayerList().getPlayers())p.removeTag("inTalk");}
            case 4 -> {
                var field=GomokuServer.class.getDeclaredField("room");field.setAccessible(true);
                var room=(GomokuRoom)field.get(null);var deadline=GomokuMatch.class.getDeclaredField("deadline");deadline.setAccessible(true);
                for(var game:room.games())deadline.setLong(game,GomokuServer.now()+1800);
            }
            case 5 -> server.getPlayerList().getPlayerByName("GomokuA").removeTag("spectator");
            case 6 -> {
                command(server,"scoreboard players add #Global ggdGameId 1");command(server,"scoreboard players set #MeetingPhase ggdSession 0");
                for(var p:server.getPlayerList().getPlayers()) {for(String tag:Set.of("players","gamingGGD","spectator","deadInMap","inTalk","endGame","inPelican","inTutorial","dlcGhostActive","ggdLateSpectator"))p.removeTag(tag);p.addTag("lobby");p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.setNoGravity(true);}
            }
            case 7 -> {for(var p:server.getPlayerList().getPlayers())p.addTag("endGame");}
            case 8 -> {var p=server.getPlayerList().getPlayerByName("GomokuA");p.removeTag("lobby");p.addTag("players");p.addTag("gamingGGD");p.addTag("deadInMap");p.addTag("inPelican");}
            case 10 -> command(server,"goosetools gomoku close GomokuA");
            case 11 -> command(server,"goosetools gomoku open GomokuA");
            case 12 -> {var p=server.getPlayerList().getPlayerByName("GomokuA");p.addTag("gamingGGD");p.addTag("ggdStarting");}
            case 13 -> {var p=server.getPlayerList().getPlayerByName("GomokuA");p.removeTag("ggdStarting");p.removeTag("lobby");p.addTag("inTutorial");}
            case 14 -> command(server,"scoreboard players add #Global ggdGameId 1");
            case 99 -> {Files.writeString(output().resolve("server-pass.txt"),"PASS dedicated server, two real clients, isolated fixtures and authoritative Gomoku packets");server.halt(false);}
            default -> throw new IllegalArgumentException("Unknown fixture");
        }
        Files.writeString(output().resolve("fixture.txt"),Integer.toString(operation));
    }
}
