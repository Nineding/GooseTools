package com.goosethings.tools.task;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class TaskPackets {
    private TaskPackets() {}
    public static void registerTypes() {
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Close.TYPE, Close.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, name));
    }
    public record Open(long sessionId, int task, long seed) implements CustomPacketPayload {
        public static final Type<Open> TYPE = TaskPackets.type("task_open_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buffer, value) -> { buffer.writeLong(value.sessionId); buffer.writeVarInt(value.task); buffer.writeLong(value.seed); },
                buffer -> new Open(buffer.readLong(), buffer.readVarInt(), buffer.readLong()));
        public Open { if (sessionId < 1 || task < 0 || task >= TaskType.values().length) throw new IllegalArgumentException("Invalid task open"); }
        @Override public Type<Open> type() { return TYPE; }
    }
    public record State(long sessionId, int progress, int mask, int feedback,
                        boolean started, boolean complete, boolean cardInserted, long elapsed,
                        int stage, int cursor, long phaseAt, int pipeBits, long[] cleaned) implements CustomPacketPayload {
        public static final Type<State> TYPE = TaskPackets.type("task_state_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeLong(value.sessionId); buffer.writeVarInt(value.progress); buffer.writeVarInt(value.mask);
                    buffer.writeVarInt(value.feedback); buffer.writeBoolean(value.started); buffer.writeBoolean(value.complete);
                    buffer.writeBoolean(value.cardInserted); buffer.writeLong(value.elapsed);
                    buffer.writeVarInt(value.stage); buffer.writeVarInt(value.cursor); buffer.writeLong(value.phaseAt);
                    buffer.writeInt(value.pipeBits);
                    for (long word : value.cleaned) buffer.writeLong(word);
                }, buffer -> new State(buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readLong(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(), buffer.readInt(), readCleaned(buffer)));
        public State {
            if (sessionId < 1 || progress < 0 || progress > 6 || mask < 0 || mask > 65535
                    || feedback < 0 || feedback > TaskSession.WIPE || elapsed < 0 || elapsed > 600_000
                    || stage < 0 || stage > 2 || cursor < 0 || cursor > 4 || phaseAt < 0 || phaseAt > 600_000
                    || cleaned == null || cleaned.length != TaskExtraLayout.CLEAN_WORDS
                    || (cleaned[4] >>> 32) != 0) throw new IllegalArgumentException("Invalid task state");
            cleaned = cleaned.clone();
        }
        @Override public long[] cleaned() { return cleaned.clone(); }
        private static long[] readCleaned(RegistryFriendlyByteBuf buffer) {
            long[] words = new long[TaskExtraLayout.CLEAN_WORDS];
            for (int i = 0; i < words.length; i++) words[i] = buffer.readLong();
            return words;
        }
        @Override public Type<State> type() { return TYPE; }
    }
    public record Close(long sessionId) implements CustomPacketPayload {
        public static final Type<Close> TYPE = TaskPackets.type("task_close_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Close> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeLong(value.sessionId), buffer -> new Close(buffer.readLong()));
        @Override public Type<Close> type() { return TYPE; }
    }
    public record Action(long sessionId, int sequence, int action, int item, double x, double y, long elapsed) implements CustomPacketPayload {
        public static final Type<Action> TYPE = TaskPackets.type("task_action_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeLong(value.sessionId); buffer.writeVarInt(value.sequence); buffer.writeVarInt(value.action);
                    buffer.writeVarInt(value.item); buffer.writeDouble(value.x); buffer.writeDouble(value.y); buffer.writeLong(value.elapsed);
                }, buffer -> new Action(buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readDouble(), buffer.readDouble(), buffer.readLong()));
        public boolean valid() {
            return sessionId > 0 && sequence >= 0 && sequence <= 100_000 && action >= 0 && action <= TaskSession.REPLAY
                    && item >= -1 && item <= 15 && Double.isFinite(x) && Double.isFinite(y)
                    && x >= 0 && x <= TaskLayout.WIDTH && y >= 0 && y <= TaskLayout.HEIGHT
                    && elapsed >= 0 && elapsed <= 600_000;
        }
        @Override public Type<Action> type() { return TYPE; }
    }
}
