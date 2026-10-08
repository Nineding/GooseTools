package com.goosethings.tools.task;

/** Server-owned rules and timing. Client input describes gestures, never completion. */
public final class TaskSession {
    public static final int READY = 0, HIT = 1, BEGIN = 2, MOVE = 3, END = 4, CANCEL = 5, REPLAY = 6;
    public static final int WAITING = 0, PLAY = 1, SUCCESS = 2, MISS = 3, WRONG_WIRE = 4,
            INSERT_CARD = 5, CARD_READY = 6, TOO_FAST = 7, TOO_SLOW = 8, INCOMPLETE_SWIPE = 9,
            DROP_IN_BIN = 10, ALIGN_KNOB = 11;
    public final long id, seed, createdAt;
    public final TaskType type;
    public final TaskLayout layout;
    private boolean started, complete, cardInserted;
    private long startedAt, finishedAt, lastElapsed = -1, lastHit = -500;
    private long swipeStart = -1, knobSince = -1, lastKnobMove = -1;
    private double swipeMaxX;
    private int mask, progress, feedback = WAITING, dragging = -1, sequence = -1;
    private long rateWindow;
    private int rateCount;
    private final double[] garbageX, garbageY, knobAngles;

    public TaskSession(long id, TaskType type, long seed, long now) {
        this.id = id; this.type = type; this.seed = seed; this.createdAt = now;
        layout = new TaskLayout(seed);
        garbageX = layout.garbageX.clone(); garbageY = layout.garbageY.clone();
        knobAngles = layout.knobInitial.clone();
    }

    public boolean apply(int seq, int action, int item, double x, double y, long clientElapsed,
                         long now, int latency) {
        if (seq <= sequence || seq < 0 || seq > 100_000 || !Double.isFinite(x) || !Double.isFinite(y)
                || x < 0 || x > TaskLayout.WIDTH || y < 0 || y > TaskLayout.HEIGHT) return false;
        if (now - rateWindow >= 1000) { rateWindow = now; rateCount = 0; }
        if (++rateCount > 50) return false;
        sequence = seq;
        if (action == READY) {
            if (started) return false;
            started = true; startedAt = now; feedback = type == TaskType.SWIPE ? INSERT_CARD : PLAY;
            return true;
        }
        if (!started || complete) return false;
        long elapsed = now - startedAt;
        // Bounded input-history window compensates transit time without trusting arbitrary timestamps.
        long tolerance = Math.min(350, 100 + Math.max(0, latency));
        if (clientElapsed < 0 || clientElapsed < lastElapsed || clientElapsed > elapsed + 75
                || elapsed - clientElapsed > tolerance) return false;
        lastElapsed = clientElapsed;
        boolean changed = switch (type) {
            case TIMING -> timing(action, clientElapsed);
            case WIRES -> wires(action, item, x, y);
            case SWIPE -> swipe(action, x, y, clientElapsed);
            case GARBAGE -> garbage(action, item, x, y);
            case KNOBS -> knobs(action, item, x, y, now);
        };
        if (progress == type.total && !complete) {
            complete = true; finishedAt = now; feedback = SUCCESS;
        }
        return changed;
    }

    private boolean timing(int action, long elapsed) {
        if (action != HIT || elapsed - lastHit < 160) return false;
        lastHit = elapsed;
        if (TaskLayout.inGreen(layout.timingAngle(elapsed))) { progress++; feedback = PLAY; }
        else { progress = 0; feedback = MISS; }
        return true;
    }

    private boolean wires(int action, int item, double x, double y) {
        if (action == BEGIN && item >= 0 && item < 4 && (mask & (1 << item)) == 0
                && TaskLayout.near(x, y, 68, TaskLayout.wireY(item), 16)) {
            dragging = item; return false;
        }
        if (action != END || dragging < 0) return false;
        int source = dragging; dragging = -1;
        for (int right = 0; right < 4; right++) {
            if (TaskLayout.near(x, y, 352, TaskLayout.wireY(right), 18)
                    && layout.rightWires[right] == source) {
                mask |= 1 << source; progress = Integer.bitCount(mask); feedback = PLAY; return true;
            }
        }
        feedback = WRONG_WIRE; return true;
    }

    private boolean swipe(int action, double x, double y, long elapsed) {
        if (action == HIT && !cardInserted && TaskLayout.CARD_START.contains(x, y)) {
            cardInserted = true; feedback = CARD_READY; return true;
        }
        if (action == BEGIN && cardInserted && x >= 65 && x <= 125 && y >= 132 && y <= 183) {
            swipeStart = elapsed; swipeMaxX = x; dragging = 0; return false;
        }
        if (dragging != 0 || swipeStart < 0) return false;
        if (action == MOVE) {
            if (y < 128 || y > 187 || x < swipeMaxX - 20) { dragging = -1; feedback = INCOMPLETE_SWIPE; return true; }
            swipeMaxX = Math.max(swipeMaxX, x); return false;
        }
        if (action != END) return false;
        dragging = -1;
        long duration = elapsed - swipeStart;
        if (x < 318 || y < 128 || y > 187 || x < swipeMaxX - 20) feedback = INCOMPLETE_SWIPE;
        else if (duration < 600) feedback = TOO_FAST;
        else if (duration > 1200) feedback = TOO_SLOW;
        else { progress = 1; feedback = SUCCESS; }
        return true;
    }

    private boolean garbage(int action, int item, double x, double y) {
        if (action == BEGIN && item >= 0 && item < 6 && (mask & (1 << item)) == 0
                && TaskLayout.near(x, y, garbageX[item], garbageY[item], 22)) {
            dragging = item; return false;
        }
        if (action != END || dragging < 0) return false;
        int source = dragging; dragging = -1;
        if (TaskLayout.BIN.contains(x, y)) {
            mask |= 1 << source; progress = Integer.bitCount(mask); feedback = PLAY;
        } else {
            // Keep discarded pieces within the reachable work area.
            garbageX[source] = Math.clamp(x, 40, 292); garbageY[source] = Math.clamp(y, 90, 248);
            feedback = DROP_IN_BIN;
        }
        return true;
    }

    private boolean knobs(int action, int item, double x, double y, long now) {
        if (action == BEGIN && item >= 0 && item < 3 && (mask & (1 << item)) == 0
                && TaskLayout.near(x, y, TaskLayout.knobX(item), 162, 49)) {
            dragging = item; knobSince = -1;
        } else if ((action != MOVE && action != END) || dragging < 0) return false;
        int current = dragging;
        knobAngles[current] = TaskLayout.angle(x, y, TaskLayout.knobX(current), 162);
        lastKnobMove = now;
        if (TaskLayout.distance(knobAngles[current], layout.knobTargets[current]) <= 8) {
            if (knobSince < 0) knobSince = now;
        } else knobSince = -1;
        if (action == END) { dragging = -1; knobSince = -1; }
        return false;
    }

    public boolean tick(long now) {
        if (!started || complete || type != TaskType.KNOBS || dragging < 0 || knobSince < 0) return false;
        if (now - lastKnobMove > 200) { knobSince = -1; return false; }
        if (now - knobSince < 600) return false;
        mask |= 1 << dragging; dragging = -1; knobSince = -1;
        progress = Integer.bitCount(mask); feedback = PLAY;
        if (progress == type.total) { complete = true; finishedAt = now; feedback = SUCCESS; }
        return true;
    }

    public boolean started() { return started; }
    public boolean complete() { return complete; }
    public int mask() { return mask; }
    public int progress() { return progress; }
    public int feedback() { return feedback; }
    public boolean cardInserted() { return cardInserted; }
    public long elapsed(long now) { return started ? (complete ? finishedAt : now) - startedAt : 0; }
}
