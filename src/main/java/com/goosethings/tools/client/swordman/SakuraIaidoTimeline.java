package com.goosethings.tools.client.swordman;

/** Client visual clock. Inventory refreshes cannot retrigger it; only an approved dash can slash. */
public final class SakuraIaidoTimeline {
    public enum Phase { IDLE, SHEATHE, HOLD, WAIT, SLASH, CANCEL }
    private Phase phase = Phase.IDLE;
    private float started, waitAt;
    public Phase phase() { return phase; }
    public float elapsed(float now) { return Math.max(0, now - started); }
    public boolean active() { return phase != Phase.IDLE; }
    public void clear() { phase = Phase.IDLE; started = waitAt = 0; }
    public void tick(float now, boolean eligible, boolean charging, boolean held) {
        if (!eligible) { clear(); return; }
        switch (phase) {
            case IDLE -> { if (charging && held) enter(Phase.SHEATHE, now); }
            case SHEATHE, HOLD -> {
                if (!held) { phase = Phase.WAIT; waitAt = now; }
                else if (!charging) enter(Phase.CANCEL, now);
                else if (elapsed(now) >= .8F) phase = Phase.HOLD;
            }
            case WAIT -> { if (now - waitAt > .7F) enter(Phase.CANCEL, now); }
            case SLASH -> { if (elapsed(now) >= 1.15F) clear(); }
            case CANCEL -> { if (elapsed(now) >= .35F) clear(); }
        }
    }
    public void approvedDash(float now, boolean eligible) {
        if (eligible && phase != Phase.SLASH) enter(Phase.SLASH, now);
    }
    private void enter(Phase next, float now) { phase = next; started = now; }
}
