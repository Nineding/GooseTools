package com.goosethings.tools.game;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class GamePackets {
    private GamePackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, "game_" + name + "_v1"));
    }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Close.TYPE, Close.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Input.TYPE, Input.CODEC);
    }
    public record Open(long sessionId, int game) implements CustomPacketPayload {
        public static final Type<Open> TYPE = id("open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (b, v) -> { b.writeLong(v.sessionId); b.writeVarInt(v.game); }, b -> new Open(b.readLong(), b.readVarInt()));
        public Open { if (sessionId < 1 || game < 0 || game >= GameType.values().length) throw new IllegalArgumentException("Invalid game open"); }
        @Override public Type<Open> type() { return TYPE; }
    }
    public record Close(long sessionId) implements CustomPacketPayload {
        public static final Type<Close> TYPE = id("close");
        public static final StreamCodec<RegistryFriendlyByteBuf, Close> CODEC = StreamCodec.of((b, v) -> b.writeLong(v.sessionId), b -> new Close(b.readLong()));
        @Override public Type<Close> type() { return TYPE; }
    }
    public record Input(long sessionId, int sequence, int action, int value, double x, double y) implements CustomPacketPayload {
        public static final Type<Input> TYPE = id("input");
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
                (b, v) -> { b.writeLong(v.sessionId); b.writeVarInt(v.sequence); b.writeVarInt(v.action); b.writeVarInt(v.value); b.writeDouble(v.x); b.writeDouble(v.y); },
                b -> new Input(b.readLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble()));
        public boolean valid() { return sessionId > 0 && sequence >= 0 && action >= 0 && action <= GameSession.NEXT && value >= -1 && value <= 479
                && Double.isFinite(x) && Double.isFinite(y) && x >= 0 && x <= 600 && y >= 0 && y <= 400; }
        @Override public Type<Input> type() { return TYPE; }
    }
    public record State(long sessionId, long revision, GameSnapshot state) implements CustomPacketPayload {
        public static final Type<State> TYPE = id("state");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (b, v) -> {
                    b.writeLong(v.sessionId); b.writeLong(v.revision); GameSnapshot s = v.state;
                    b.writeVarInt(s.phase()); b.writeVarInt(s.mode()); b.writeVarInt(s.difficulty()); b.writeVarInt(s.cols()); b.writeVarInt(s.rows());
                    b.writeLong(s.score()); b.writeLong(s.opponent()); b.writeVarInt(s.lives()); b.writeLong(s.elapsed()); b.writeLong(s.clock()); b.writeLong(s.event());
                    b.writeVarInt(s.effect()); b.writeVarInt(s.detail()); b.writeLong(s.best()); b.writeLong(s.bestTime()); b.writeVarInt(s.wins());
                    ints(b, s.board()); double[] actors = s.actors(); b.writeVarInt(actors.length); for (double a : actors) b.writeDouble(a); ints(b, s.moves());
                }, b -> new State(b.readLong(), b.readLong(), read(b)));
        public State { if (sessionId < 1 || revision < 0 || state == null) throw new IllegalArgumentException("Invalid game state envelope"); }
        private static void ints(RegistryFriendlyByteBuf b, int[] values) { b.writeVarInt(values.length); for (int value : values) b.writeVarInt(value); }
        private static int size(RegistryFriendlyByteBuf b, int limit) { int n = b.readVarInt(); if (n < 0 || n > limit) throw new IllegalArgumentException("Oversized game array"); return n; }
        private static int[] ints(RegistryFriendlyByteBuf b, int limit) { int[] a = new int[size(b, limit)]; for (int i = 0; i < a.length; i++) a[i] = b.readVarInt(); return a; }
        private static GameSnapshot read(RegistryFriendlyByteBuf b) {
            int phase = b.readVarInt(), mode = b.readVarInt(), diff = b.readVarInt(), cols = b.readVarInt(), rows = b.readVarInt();
            long score = b.readLong(), opponent = b.readLong(); int lives = b.readVarInt();
            long elapsed = b.readLong(), clock = b.readLong(), event = b.readLong(); int effect = b.readVarInt(), detail = b.readVarInt();
            long best = b.readLong(), bestTime = b.readLong(); int wins = b.readVarInt();
            int[] board = ints(b, 480); double[] actors = new double[size(b, 64)]; for (int i = 0; i < actors.length; i++) actors[i] = b.readDouble();
            return new GameSnapshot(phase, mode, diff, cols, rows, score, opponent, lives, elapsed, clock, event, effect, detail, best, bestTime, wins, board, actors, ints(b, 128));
        }
        @Override public Type<State> type() { return TYPE; }
    }
}
