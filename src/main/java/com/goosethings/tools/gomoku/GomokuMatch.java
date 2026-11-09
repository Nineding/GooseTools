package com.goosethings.tools.gomoku;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-thread owned; AI receives only a copied board. null denotes the computer seat. */
public final class GomokuMatch {
    public static final long TURN_MILLIS = 60_000;
    public static final int RUNNING = 0, DRAW = 3, CANCELLED = 4;
    public static final int FIVE = 0, FULL = 1, TIMEOUT = 2, RESIGN = 3, DISCONNECT = 4, CANCEL = 5;
    public final long id;
    public final UUID black, white;
    public final GomokuAccess.Group group;
    public final int difficulty;
    public final GomokuBoard board = new GomokuBoard();
    private final Map<UUID, Long> sequences = new HashMap<>();
    private int turn = 1, outcome, reason, rematchMask;
    private long revision = 1, deadline, finishedAt, rematchDeadline;

    public GomokuMatch(long id, UUID black, UUID white, GomokuAccess.Group group, int difficulty, long now) {
        if (id < 1 || group == null || black == null && white == null || Objects.equals(black, white)
                || difficulty < 0 || difficulty > 2) throw new IllegalArgumentException("Invalid match");
        this.id = id; this.black = black; this.white = white; this.group = group; this.difficulty = difficulty;
        deadline = now + TURN_MILLIS;
    }
    public int color(UUID player) { return player == null ? 0 : player.equals(black) ? 1 : player.equals(white) ? 2 : 0; }
    public UUID seat(int color) { return color == 1 ? black : white; }
    public boolean computer() { return black == null || white == null; }
    public boolean aiTurn() { return outcome == RUNNING && seat(turn) == null; }
    public int turn() { return turn; }
    public int outcome() { return outcome; }
    public int reason() { return reason; }
    public long revision() { return revision; }
    public long finishedAt() { return finishedAt; }
    public long remaining(long now) { return outcome == RUNNING ? Math.max(0, deadline - now) : 0; }
    public int rematchMask() { return rematchMask; }

    public boolean sequence(UUID player, long sequence) {
        if (color(player) == 0 || sequence < 0 || sequence <= sequences.getOrDefault(player, -1L)) return false;
        sequences.put(player, sequence); return true;
    }
    public boolean move(UUID player, long sequence, long expectedRevision, int cell, long now) {
        if (!sequence(player, sequence)) return false;
        expire(now);
        return color(player) == turn && expectedRevision == revision && place(cell, now);
    }
    public boolean aiMove(long expectedRevision, int cell, long now) {
        expire(now);
        return aiTurn() && expectedRevision == revision && place(cell, now);
    }
    private boolean place(int cell, long now) {
        if (outcome != RUNNING || !board.place(cell, turn)) return false;
        revision++;
        if (board.winner() != 0) finish(board.winner(), FIVE, now);
        else if (board.full()) finish(DRAW, FULL, now);
        else { turn = 3 - turn; deadline = now + TURN_MILLIS; }
        return true;
    }
    public void expire(long now) {
        if (outcome == RUNNING && now >= deadline) finish(3 - turn, TIMEOUT, now);
        if (rematchMask != 0 && now >= rematchDeadline) { rematchMask = 0; revision++; }
    }
    public void forfeit(UUID player, int reason, long now) {
        if (color(player) != 0 && outcome == RUNNING) finish(3 - color(player), reason, now);
    }
    public void cancel(long now) { if (outcome == RUNNING) finish(CANCELLED, CANCEL, now); }
    private void finish(int result, int why, long now) {
        outcome = result; reason = why; finishedAt = now; rematchMask = 0; revision++;
    }
    public boolean requestRematch(UUID player, long now) {
        expire(now);
        if (color(player) == 0 || outcome == RUNNING || outcome == CANCELLED) return false;
        if (rematchMask == 0) rematchDeadline = now + 30_000;
        int flag = 1 << (color(player) - 1);
        if ((rematchMask & flag) == 0) { rematchMask |= flag; revision++; }
        return computer() || rematchMask == 3;
    }
}
