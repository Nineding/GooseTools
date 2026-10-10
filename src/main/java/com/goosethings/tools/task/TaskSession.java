package com.goosethings.tools.task;

/** Server-owned rules and timing. Client input describes gestures, never completion. */
public final class TaskSession {
    public static final int READY = 0, HIT = 1, BEGIN = 2, MOVE = 3, END = 4, CANCEL = 5, REPLAY = 6;
    public static final int WAITING = 0, PLAY = 1, SUCCESS = 2, MISS = 3, WRONG_WIRE = 4,
            INSERT_CARD = 5, CARD_READY = 6, TOO_FAST = 7, TOO_SLOW = 8, INCOMPLETE_SWIPE = 9,
            DROP_IN_BIN = 10, ALIGN_KNOB = 11, WRONG_CATEGORY = 12, WATCH_SEQUENCE = 13,
            REPEAT_SEQUENCE = 14, WRONG_SEQUENCE = 15, PIPE_LEAK = 16, WATER_FLOW = 17, WIPE = 18;
    public final long id, seed, createdAt;
    public final TaskType type;
    public final TaskLayout layout;
    private final PowerStationSession station;
    private final com.goosethings.tools.task.profession.ProfessionSession profession;
    private boolean started, complete, cardInserted;
    private long startedAt, finishedAt, lastElapsed = -1, lastHit = -500;
    private long swipeStart = -1, knobSince = -1, lastKnobMove = -1;
    private double swipeMaxX;
    private int mask, progress, feedback = WAITING, dragging = -1, sequence = -1;
    private long rateWindow;
    private int rateCount;
    private final double[] garbageX, garbageY, knobAngles;
    private final int[] pipeRotations;
    private final long[] cleaned = new long[TaskExtraLayout.CLEAN_WORDS];
    private int stage, cursor;
    private long chargeSince = -1, lastCharge = -1;
    private final PurificationLaserLayout laser;
    private int laserBits;
    private long phaseAt, lastBrushAt;
    private double brushX, brushY;

    public TaskSession(long id, TaskType type, long seed, long now) {
        this.id = id; this.type = type; this.seed = seed; this.createdAt = now;
        layout = new TaskLayout(seed);
        laser = type == TaskType.PURIFICATIONLASER ? new PurificationLaserLayout(seed) : null;
        if (laser != null) laserBits = laser.initialBits;
        station = type == TaskType.POWERSTATION ? new PowerStationSession(seed) : null;
        profession = type.profession() ? new com.goosethings.tools.task.profession.ProfessionSession(type, seed) : null;
        garbageX = layout.garbageX.clone(); garbageY = layout.garbageY.clone();
        knobAngles = layout.knobInitial.clone();
        pipeRotations = layout.extra.pipeInitial.clone();
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
            started = true; startedAt = now;
            if (station != null) station.start(now);
            if (profession != null) profession.start(now);
            feedback = switch (type) { case SWIPE -> INSERT_CARD; case MEMORY -> WATCH_SEQUENCE; case CLEANING -> WIPE; default -> PLAY; };
            return true;
        }
        if (!started || complete) return false;
        long elapsed = now - startedAt;
        // Bounded input-history window compensates transit time without trusting arbitrary timestamps.
        long tolerance = Math.min(350, 100 + Math.max(0, latency));
        if (type != TaskType.PURIFICATION && (clientElapsed < 0 || clientElapsed < lastElapsed || clientElapsed > elapsed + 75
                || elapsed - clientElapsed > tolerance)) return false;
        lastElapsed = clientElapsed;
        boolean changed = switch (type) {
            case TIMING -> timing(action, clientElapsed);
            case WIRES -> wires(action, item, x, y);
            case SWIPE -> swipe(action, x, y, clientElapsed);
            case GARBAGE -> garbage(action, item, x, y);
            case KNOBS -> knobs(action, item, x, y, now);
            case SORTING -> sorting(action, item, x, y);
            case MEMORY -> memory(action, item, x, y, elapsed);
            case PIPES -> pipes(action, item, x, y, elapsed);
            case CLEANING -> cleaning(action, x, y, now);
            case KEEPGREEN -> keepGreen(action, x, y, now);
            case CUTWIRES -> cutWires(action, x, y, now);
            case PURIFICATION -> purification(action, item, x, y, now);
            case PURIFICATIONLASER -> purificationLaser(action, item, x, y);
            case POWERSTATION, TELECOM, NUCLEAR, FOODSAFETY, CIVIL -> false;
        };
        if (progress == type.total && !complete) {
            complete = true; finishedAt = now; feedback = SUCCESS;
        }
        return changed;
    }

    private boolean sorting(int action, int item, double x, double y) {
        if (action == BEGIN && item >= 0 && item < 6 && (mask & (1 << item)) == 0
                && TaskLayout.near(x, y, TaskExtraLayout.sortX(item), TaskExtraLayout.SORT_Y, 22)) {
            dragging = item; return false;
        }
        if (action != END || dragging < 0 || item != dragging) return false;
        int source = dragging; dragging = -1;
        if (TaskExtraLayout.category(layout.extra.sortItems[source] / 2).contains(x, y)) {
            mask |= 1 << source; progress = Integer.bitCount(mask); feedback = PLAY;
        } else feedback = WRONG_CATEGORY;
        return true;
    }
    public long demonstrationDuration() { return layout.extra.memory[Math.min(progress, 2)].length * 650L + 350; }
    private boolean memory(int action, int item, double x, double y, long elapsed) {
        if (action != HIT || stage != 1 || item < 0 || item >= 4
                || !TaskExtraLayout.memoryButton(item).contains(x, y) || elapsed - lastHit < 100) return false;
        lastHit = elapsed;
        if (layout.extra.memory[progress][cursor] != item) {
            stage = 0; cursor = 0; phaseAt = Math.min(600_000, elapsed + 500); feedback = WRONG_SEQUENCE;
        } else {
            cursor++; feedback = REPEAT_SEQUENCE;
            if (cursor == layout.extra.memory[progress].length) {
                progress++; cursor = 0; stage = 0; phaseAt = Math.min(600_000, elapsed + 500); feedback = WATCH_SEQUENCE;
            }
        }
        return true;
    }
    private boolean pipes(int action, int item, double x, double y, long elapsed) {
        if (action != HIT || stage != 0) return false;
        int cell = TaskExtraLayout.pipeCell(x, y);
        if (cell >= 0 && item == cell) {
            pipeRotations[cell] = (pipeRotations[cell] + 1) & 3; mask = 0; cursor = 0; feedback = PLAY; return true;
        }
        if (item != -1 || !TaskExtraLayout.FLOW.contains(x, y)) return false;
        TaskExtraLayout.PipeResult result = layout.extra.inspect(pipeRotations);
        if (result.connected()) { stage = 2; phaseAt = elapsed; mask = result.reached(); feedback = WATER_FLOW; }
        else { mask = result.errors(); cursor = 1; feedback = PIPE_LEAK; }
        return true;
    }
    private boolean cleaning(int action, double x, double y, long now) {
        if (action == BEGIN) {
            if (!TaskExtraLayout.GLASS.contains(x, y)) return false;
            dragging = 0; brushX = x; brushY = y; lastBrushAt = now;
        } else if ((action != MOVE && action != END) || dragging != 0) return false;
        double distance = Math.hypot(x - brushX, y - brushY);
        // Don't interpolate a giant cursor jump or a gap in the gesture history.
        boolean continuous = now - lastBrushAt <= 300 && distance <= 96;
        int steps = continuous ? Math.max(1, (int) Math.ceil(distance / 4)) : 1;
        boolean changed = false;
        for (int step = 1; step <= steps; step++) {
            double t = step / (double) steps;
            double px = continuous ? brushX + (x - brushX) * t : x;
            double py = continuous ? brushY + (y - brushY) * t : y;
            if (!TaskExtraLayout.GLASS.contains(px, py)) continue;
            for (int cell = 0; cell < TaskExtraLayout.CLEAN_CELLS; cell++) {
                if (layout.extra.stains[cell] < 0 || TaskExtraLayout.cleaned(cleaned, cell)) continue;
                double cx = TaskExtraLayout.CLEAN_X + (cell % TaskExtraLayout.COLS + 0.5) * TaskExtraLayout.CELL;
                double cy = TaskExtraLayout.CLEAN_Y + (cell / TaskExtraLayout.COLS + 0.5) * TaskExtraLayout.CELL;
                if (TaskLayout.near(px, py, cx, cy, 11)) { cleaned[cell / 64] |= 1L << (cell % 64); changed = true; }
            }
        }
        brushX = x; brushY = y; lastBrushAt = now;
        if (action == END) dragging = -1;
        if (changed) {
            int remaining = 0;
            for (int cell = 0; cell < TaskExtraLayout.CLEAN_CELLS; cell++)
                if (layout.extra.stains[cell] >= 0 && !TaskExtraLayout.cleaned(cleaned, cell)) remaining |= 1 << layout.extra.stains[cell];
            mask = (~remaining) & 63; progress = Integer.bitCount(mask); feedback = WIPE;
        }
        return changed;
    }

    private boolean keepGreen(int action, double x, double y, long now) {
        if (action == BEGIN) {
            if (!KeepGreenLayout.inside(x, y)) return false;
            dragging = 0; brushX = x; brushY = y; lastBrushAt = now;
        } else if ((action != MOVE && action != END) || dragging != 0) return false;
        double distance = Math.hypot(x - brushX, y - brushY);
        long delta = Math.max(0, now - lastBrushAt);
        boolean continuous = delta <= 300 && distance <= Math.min(64, (delta + 30) * .8);
        int steps = continuous ? Math.max(1, (int) Math.ceil(distance / 2)) : 1;
        boolean changed = false;
        for (int step = 1; step <= steps; step++) {
            double t = step / (double) steps;
            double px = continuous ? brushX + (x - brushX) * t : x;
            double py = continuous ? brushY + (y - brushY) * t : y;
            if (!KeepGreenLayout.inside(px, py)) continue;
            for (int cell = 0; cell < KeepGreenLayout.CELLS; cell++) {
                if (!KeepGreenLayout.target(cell) || TaskExtraLayout.cleaned(cleaned, cell)) continue;
                if (TaskLayout.near(px, py, KeepGreenLayout.cellX(cell), KeepGreenLayout.cellY(cell), 5)) {
                    cleaned[cell / 64] |= 1L << (cell % 64); changed = true;
                }
            }
        }
        brushX = x; brushY = y; lastBrushAt = now;
        if (action == END) dragging = -1;
        if (changed) {
            mask = 0;
            for (int lamp = 0; lamp < 3; lamp++) if (KeepGreenLayout.percent(cleaned, lamp) >= 95) mask |= 1 << lamp;
            progress = Integer.bitCount(mask); feedback = PLAY;
        }
        return changed;
    }

    private boolean cutWires(int action, double x, double y, long now) {
        if (action == BEGIN) {
            if (!CutWiresLayout.inside(x, y)) return false;
            dragging = 0; brushX = x; brushY = y; lastBrushAt = now;
            return false;
        }
        if ((action != MOVE && action != END) || dragging != 0) return false;
        long delta = Math.max(0, now - lastBrushAt);
        double distance = Math.hypot(x - brushX, y - brushY);
        int before = mask;
        // A missing history segment or giant jump cannot cut unseen wires.
        if (delta <= 300 && distance <= Math.min(64, (delta + 30) * .8)) {
            for (int wire = 0; wire < 4; wire++)
                if (CutWiresLayout.intersects(wire, brushX, brushY, x, y)) mask |= 1 << wire;
        }
        brushX = x; brushY = y; lastBrushAt = now;
        if (action == END) dragging = -1;
        progress = Integer.bitCount(mask); feedback = PLAY;
        return before != mask;
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

    private boolean purification(int action, int item, double x, double y, long now) {
        if (action != HIT) return false;
        if (item >= 0 && item <= 11 && stage == 0 && PurificationLayout.key(item).contains(x, y)) {
            if (item <= 9 && cursor < 4) { mask = mask * 10 + item; cursor++; feedback = PLAY; }
            else if (item == 10) { mask = 0; cursor = 0; feedback = PLAY; }
            else if (item == 11) {
                if (cursor == 4 && mask == PurificationLayout.password(seed)) { stage = 1; feedback = PLAY; }
                else { mask = 0; cursor = 0; feedback = MISS; }
            }
            return true;
        }
        if (item != 12 || stage == 0 || !PurificationLayout.ACTIVATE.contains(x, y)) return false;
        // Real clicks only. Mouse-hold/MOVE and bursts cannot advance charging.
        if (lastCharge >= 0 && now - lastCharge < 100) return false;
        if (chargeSince < 0 || now - lastCharge > 500) chargeSince = now;
        lastCharge = now; stage = 2; phaseAt = Math.min(5000, now - chargeSince); feedback = PLAY;
        if (now - chargeSince >= 5000) progress = 1;
        return true;
    }
    private boolean purificationLaser(int action, int item, double x, double y) {
        if (action != HIT || item < 0 || item > 2 || laser.mirrorAt(x, y) != item) return false;
        laserBits ^= 1 << item;
        mask = laser.trace(laserBits).visited();
        if (laser.trace(laserBits).returned()) progress = 1;
        return true;
    }

    public boolean tick(long now) {
        if (type == TaskType.PURIFICATION && started && !complete && stage == 2) {
            if (now - lastCharge > 500) { chargeSince = -1; stage = 1; phaseAt = 0; feedback = MISS; }
            else phaseAt = Math.min(5000, now - chargeSince);
            return true;
        }
        if (profession != null) return profession.tick(now);
        if (station != null) return station.tick(now);
        if (started && !complete && type == TaskType.MEMORY && stage == 0
                && now - startedAt >= phaseAt + demonstrationDuration()) {
            stage = 1; feedback = REPEAT_SEQUENCE; return true;
        }
        if (started && !complete && type == TaskType.PIPES && stage == 2 && now - startedAt >= phaseAt + 900) {
            progress = 1; complete = true; finishedAt = now; feedback = SUCCESS; return true;
        }
        if (!started || complete || type != TaskType.KNOBS || dragging < 0 || knobSince < 0) return false;
        if (now - lastKnobMove > 200) { knobSince = -1; return false; }
        if (now - knobSince < 600) return false;
        mask |= 1 << dragging; dragging = -1; knobSince = -1;
        progress = Integer.bitCount(mask); feedback = PLAY;
        if (progress == type.total) { complete = true; finishedAt = now; feedback = SUCCESS; }
        return true;
    }

    public PowerStationSession station() { return station; }
    public com.goosethings.tools.task.profession.ProfessionSession profession() { return profession; }
    public boolean started() { return profession != null ? profession.started() : station != null ? station.started() : started; }
    public boolean complete() { return profession != null ? profession.complete() : station != null ? station.complete() : complete; }
    public int mask() { return mask; }
    public int progress() { return profession != null ? profession.stage() : station != null ? station.stage() : progress; }
    public int feedback() { return feedback; }
    public boolean cardInserted() { return cardInserted; }
    public long elapsed(long now) { return profession != null ? profession.elapsed(now) : station != null ? station.elapsed(now) : started ? (complete ? finishedAt : now) - startedAt : 0; }
    public int stage() { return stage; }
    public int cursor() { return cursor; }
    public long phaseAt() { return phaseAt; }
    public long[] cleaned() { return cleaned.clone(); }
    public int pipeBits() { if (laser != null) return laserBits; int bits = 0; for (int i = 0; i < 16; i++) bits |= pipeRotations[i] << (2 * i); return bits; }
}
