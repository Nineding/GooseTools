package com.goosethings.tools.gomoku;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Additive v1 channels; the existing arcade wire format is unchanged. */
public final class GomokuPackets {
    public static final int OPEN = 0, HIDE = 1, AI = 2, INVITE = 3, ACCEPT = 4, DECLINE = 5,
            CANCEL_INVITE = 6, MOVE = 7, RESIGN = 8, REMATCH = 9, LEAVE = 10;
    public static final UUID NONE = new UUID(0, 0);
    private GomokuPackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String suffix) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("goosetools", "gomoku_" + suffix + "_v1"));
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(View.TYPE, View.CODEC);
    }
    public record Action(int action, long request, long match, long revision, long sequence, int value, int option, UUID target) implements CustomPacketPayload {
        public static final Type<Action> TYPE = id("action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (b, v) -> { b.writeVarInt(v.action); b.writeLong(v.request); b.writeLong(v.match); b.writeLong(v.revision); b.writeLong(v.sequence); b.writeVarInt(v.value); b.writeVarInt(v.option); b.writeUUID(v.target); },
                b -> new Action(b.readVarInt(), b.readLong(), b.readLong(), b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readUUID()));
        public boolean valid() { return action >= OPEN && action <= LEAVE && request >= 0 && match >= 0 && revision >= 0 && sequence >= 0 && value >= -1 && value < 225 && option >= 0 && option <= 2 && target != null; }
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Person(UUID id, String name) {}
    public record Position(long id, long revision, String black, String white, int color, int turn,
                           int outcome, int reason, int difficulty, int remaining, int last, int rematchMask,
                           boolean ai, int[] board, int[] line) {
        public Position {
            if (id < 0 || revision < 0 || color < 0 || color > 2 || turn < 1 || turn > 2 || outcome < 0 || outcome > 4
                    || reason < 0 || reason > 5 || difficulty < 0 || difficulty > 2 || remaining < 0 || remaining > 60_000
                    || last < -1 || last >= 225 || rematchMask < 0 || rematchMask > 3 || board.length != 225 || line.length > 15)
                throw new IllegalArgumentException("Invalid Gomoku snapshot");
            for (int c : board) if (c < 0 || c > 2) throw new IllegalArgumentException("Invalid stone");
            for (int c : line) if (c < 0 || c >= 225) throw new IllegalArgumentException("Invalid line");
            board = board.clone(); line = line.clone();
        }
        @Override public int[] board() { return board.clone(); }
        @Override public int[] line() { return line.clone(); }
        public static Position empty() { return new Position(0, 0, "", "", 0, 1, 0, 0, 1, 0, -1, 0, false, new int[225], new int[0]); }
    }
    public record View(long serial, long request, int group, int notice, Position position,
                       List<Person> players, long invite, int inviteDirection, String inviteName, int inviteRemaining) implements CustomPacketPayload {
        public static final Type<View> TYPE = id("view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.of(GomokuPackets::write, GomokuPackets::read);
        public View {
            if (serial < 0 || request < -1 || group < 0 || group > 2 || notice < 0 || notice > 10 || players.size() > 128
                    || invite < 0 || inviteDirection < 0 || inviteDirection > 2 || inviteRemaining < 0 || inviteRemaining > 30_000)
                throw new IllegalArgumentException("Invalid Gomoku view");
            players = List.copyOf(players);
        }
        @Override public Type<View> type() { return TYPE; }
    }
    private static void write(RegistryFriendlyByteBuf b, View v) {
        b.writeLong(v.serial); b.writeLong(v.request); b.writeVarInt(v.group); b.writeVarInt(v.notice);
        Position p = v.position;
        b.writeLong(p.id); b.writeLong(p.revision); b.writeUtf(p.black, 64); b.writeUtf(p.white, 64);
        b.writeVarInt(p.color); b.writeVarInt(p.turn); b.writeVarInt(p.outcome); b.writeVarInt(p.reason); b.writeVarInt(p.difficulty);
        b.writeVarInt(p.remaining); b.writeVarInt(p.last); b.writeVarInt(p.rematchMask); b.writeBoolean(p.ai);
        for (int cell : p.board) b.writeByte(cell);
        b.writeVarInt(p.line.length); for (int cell : p.line) b.writeVarInt(cell);
        b.writeVarInt(v.players.size()); for (Person person : v.players) { b.writeUUID(person.id); b.writeUtf(person.name, 64); }
        b.writeLong(v.invite); b.writeVarInt(v.inviteDirection); b.writeUtf(v.inviteName, 64); b.writeVarInt(v.inviteRemaining);
    }
    private static View read(RegistryFriendlyByteBuf b) {
        long serial = b.readLong(), request = b.readLong(); int group = b.readVarInt(), notice = b.readVarInt();
        long id = b.readLong(), revision = b.readLong(); String black = b.readUtf(64), white = b.readUtf(64);
        int color = b.readVarInt(), turn = b.readVarInt(), outcome = b.readVarInt(), reason = b.readVarInt(), diff = b.readVarInt();
        int remaining = b.readVarInt(), last = b.readVarInt(), rematch = b.readVarInt(); boolean ai = b.readBoolean();
        int[] board = new int[225]; for (int i = 0; i < 225; i++) board[i] = b.readUnsignedByte();
        int[] line = new int[size(b, 15)]; for (int i = 0; i < line.length; i++) line[i] = b.readVarInt();
        List<Person> people = new ArrayList<>(); for (int i = 0, n = size(b, 128); i < n; i++) people.add(new Person(b.readUUID(), b.readUtf(64)));
        return new View(serial, request, group, notice, new Position(id, revision, black, white, color, turn, outcome, reason, diff, remaining, last, rematch, ai, board, line),
                people, b.readLong(), b.readVarInt(), b.readUtf(64), b.readVarInt());
    }
    private static int size(RegistryFriendlyByteBuf b, int max) { int n = b.readVarInt(); if (n < 0 || n > max) throw new IllegalArgumentException("Oversized Gomoku list"); return n; }
}
