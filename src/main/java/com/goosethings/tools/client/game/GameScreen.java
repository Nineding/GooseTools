package com.goosethings.tools.client.game;

import com.goosethings.tools.game.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import java.util.Arrays;
import java.util.Locale;

/** Six classic themes, a common lifecycle, and one virtual coordinate space for input and drawing. */
public final class GameScreen extends Screen {
    public static final int WIDTH = 600, HEIGHT = 400;
    private static final int[] MINE_COLORS = {0, 0xFF0000FF, 0xFF008000, 0xFFFF0000, 0xFF000080, 0xFF800000, 0xFF008080, 0xFF000000, 0xFF808080};
    private static final int[] TILE_COLORS = {0xFFCDC1B4, 0xFFEEE4DA, 0xFFEDE0C8, 0xFFF2B179, 0xFFF59563, 0xFFF67C5F, 0xFFF65E3B,
            0xFFEDCF72, 0xFFEDCC61, 0xFFEDC850, 0xFFEDC53F, 0xFFEDC22E};
    private static final int DARK = 0xFF292622, WHITE = 0xFFFFFFFF;
    private final GamePackets.Open open;
    private final GameType type;
    private GameSnapshot state, previous;
    private long revision = -1, receivedAt, moveAt, effectAt, lastInput;
    private int sequence, keyboardPaddle;
    private boolean closing, leftHeld;
    private double paddleTarget = 120, predictedBirdY, predictedBirdV, pointerX, pointerY;
    private long birdCorrectionAt;

    public GameScreen(GamePackets.Open open) {
        super(Component.translatableWithFallback("game.goosetools." + GameType.values()[open.game()].id + ".title", GameType.values()[open.game()].fallback));
        this.open = open; type = GameType.values()[open.game()];
    }
    public long sessionId() { return open.sessionId(); }
    public GameType gameType() { return type; }
    public GameSnapshot currentState() { return state; }
    public void apply(GamePackets.State next) {
        if (next.sessionId() != open.sessionId() || next.revision() < revision) return;
        GameSnapshot s = next.state(); previous = state;
        boolean newEvent = state != null && s.event() != state.event();
        if (newEvent) { effectAt = now(); if (s.effect() != 1) sound(s.effect()); }
        if (state != null && type == GameType.MERGE && s.event() != state.event() && !Arrays.equals(s.board(), state.board())) moveAt = now();
        if (type == GameType.FLAPPY && s.actors().length >= 2) {
            double[] a = s.actors();
            predictedBirdY = a[0]; predictedBirdV = a[1]; birdCorrectionAt = now();
        }
        if (state == null || state.phase() != s.phase()) {
            keyboardPaddle = 0; leftHeld = false;
            if (s.actors().length > 4 && type == GameType.PONG) paddleTarget = s.actors()[4];
        }
        state = s; revision = next.revision(); receivedAt = now();
    }
    private long visualClock() { return state == null ? 0 : state.clock() + (state.phase() == GameSession.RUNNING ? Math.min(100, now() - receivedAt) : 0); }
    private long elapsed() { return state == null ? 0 : state.elapsed() + (state.phase() == GameSession.RUNNING ? Math.min(1000, now() - receivedAt) : 0); }
    @Override public void tick() {
        if (state == null || state.phase() != GameSession.RUNNING || type != GameType.PONG) return;
        if (keyboardPaddle != 0) { paddleTarget = Math.clamp(paddleTarget + keyboardPaddle * 17, 22, 218); send(GameSession.POINT, 0, 0, paddleTarget); }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xDC111518);
        Transform t = transform(); g.pose().pushMatrix(); g.pose().translate((float) t.x, (float) t.y); g.pose().scale((float) t.scale, (float) t.scale);
        double mx = t.x(mouseX), my = t.y(mouseY); pointerX = mx; pointerY = my;
        int bg = switch (type) { case FLAPPY -> 0xFF83C4C5; case SNAKE -> 0xFF162C1E; case PONG -> 0xFF080808; case WHACK -> 0xFFFFE6C3; case MINES -> 0xFFC0C0C0; case MERGE -> 0xFFFAF8EF; };
        int ink = type == GameType.SNAKE || type == GameType.PONG ? WHITE : DARK;
        g.fill(4, 5, 604, 405, 0x77000000); g.fill(0, 0, WIDTH, HEIGHT, bg);
        title(g, getTitle(), 18, 10, 1.45F, ink);
        button(g, 565, 8, 25, 23, text("close", "×"), mx, my, ink);
        if (state != null && state.phase() == GameSession.RUNNING) button(g, 507, 8, 50, 23, text("pause", "Pause"), mx, my, ink);
        if (state != null) {
            Component stats = type == GameType.MINES ? text("mine_stats", "Wins: %s  Best: %s", state.wins(), state.bestTime() == 0 ? "—" : time(state.bestTime()))
                    : text("stats", "Score: %s   Best: %s", state.score(), Math.max(state.best(), state.score()));
            g.text(font, stats, 18, 34, ink, false);
            switch (type) { case FLAPPY -> flappy(g); case SNAKE -> snake(g); case PONG -> pong(g); case WHACK -> whack(g, mx, my); case MINES -> mines(g, mx, my); case MERGE -> merge(g, mx, my); }
        }
        Component hint = Component.translatableWithFallback("game.goosetools." + type.id + ".controls", controls());
        g.text(font, hint, 18, 373, ink, false);
        Component time = text("time", "%s", time(elapsed())); g.text(font, time, 580 - font.width(time), 373, ink, false);
        if (state == null) center(g, text("waiting", "Waiting for the server…"), 300, 195, ink);
        else if (state.phase() != GameSession.RUNNING) overlay(g, mx, my);
        g.pose().popMatrix();
    }
    private void flappy(GuiGraphicsExtractor g) {
        double[] a = state.actors(); if (a.length != 8) return;
        // Portrait field is rendered at its original pixel aspect ratio.
        g.pose().pushMatrix(); g.pose().translate(205, 53); g.pose().scale(1.06F, 1.06F);
        g.enableScissor(0, 0, 180, 280); g.fill(0, 0, 180, 280, 0xFF70C5CE);
        for (int i = 0; i < 6; i++) {
            int x = i * 38 - (int) ((visualClock() * .011) % 38);
            g.fill(x, 205, x + 25, 255, 0xFFB2E0D1); g.fill(x + 6, 196, x + 22, 255, 0xFFB2E0D1);
            circle(g, x + 9, 208, 15, 0xFFD0EAD9); circle(g, x + 23, 207, 12, 0xFFD0EAD9);
            g.fill(x + 3, 225, x + 20, 255, 0xFF93CDA6);
        }
        double dt = state.phase() == GameSession.RUNNING ? Math.min(.08, (now() - receivedAt) / 1000.0) : 0;
        for (int i = 0; i < 3; i++) {
            int x = (int) Math.round(a[2 + i * 2] - 55 * dt), gap = (int) Math.round(a[3 + i * 2]);
            pipe(g, x, 0, gap - 38, true); pipe(g, x, gap + 38, 255 - gap - 38, false);
        }
        g.fill(0, 255, 180, 257, 0xFF543C24); g.fill(0, 257, 180, 267, 0xFF8CC33F);
        for (int x = -(int) ((visualClock() * .055) % 12); x < 180; x += 12) g.fill(x, 259, x + 6, 262, 0xFFD4E66D);
        g.fill(0, 267, 180, 280, 0xFFDED895); g.fill(0, 267, 180, 269, 0xFFF7EDB6);
        double birdDt = state.phase() == GameSession.RUNNING ? Math.min(.12, (now() - birdCorrectionAt) / 1000.0) : 0;
        double birdY = predictedBirdY + predictedBirdV * birdDt + 245 * birdDt * birdDt;
        float angle = (float) Math.toRadians(Math.clamp(predictedBirdV / 5, -24, 80));
        g.pose().pushMatrix(); g.pose().translate(44, (float) birdY); g.pose().rotate(angle);
        sprite(g, "bird" + (visualClock() / 100 % 3), -10, -7, 20, 14); g.pose().popMatrix();
        number(g, Long.toString(state.score()), 90, 15, 2.3F, WHITE, true);
        g.disableScissor(); g.pose().popMatrix();
    }
    private void pipe(GuiGraphicsExtractor g, int x, int y, int h, boolean upper) {
        if (h <= 0) return;
        g.fill(x, y, x + 32, y + h, 0xFF543C24); g.fill(x + 2, y, x + 30, y + h, 0xFF73BF2E);
        g.fill(x + 4, y, x + 8, y + h, 0xFFC5E55D); g.fill(x + 24, y, x + 29, y + h, 0xFF558C25);
        int capY = upper ? y + h - 12 : y;
        g.fill(x - 2, capY, x + 34, capY + 12, 0xFF543C24); g.fill(x, capY + 2, x + 32, capY + 10, 0xFF78C337);
        g.fill(x + 2, capY + 3, x + 30, capY + 5, 0xFFC5E55D); g.fill(x + 28, capY + 2, x + 32, capY + 10, 0xFF558C25);
    }
    private void snake(GuiGraphicsExtractor g) {
        int[] board = state.board(); double[] a = state.actors();
        int ox = 120, oy = 60, cell = 18;
        bevel(g, ox - 6, oy - 6, 372, 282, 0xFF42583C, false);
        for (int i = 0; i < board.length; i++) {
            int x = ox + i % 20 * cell, y = oy + i / 20 * cell;
            g.fill(x, y, x + cell, y + cell, (i % 20 + i / 20) % 2 == 0 ? 0xFFB8D589 : 0xFFACCB7D);
            if (board[i] == 1 || board[i] == 2) {
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF294D2C); g.fill(x + 3, y + 2, x + 15, y + 14, 0xFF427B3F);
                if (board[i] == 2) {
                    int d = a.length > 0 ? (int) a[0] : 1;
                    int ex = x + (d == 3 ? 3 : d == 1 ? 11 : 4), ey = y + (d == 0 ? 3 : d == 2 ? 11 : 4);
                    g.fill(ex, ey, ex + 3, ey + 3, WHITE); g.fill(ex + (d % 2 == 0 ? 6 : 0), ey + (d % 2 == 1 ? 6 : 0), ex + 3 + (d % 2 == 0 ? 6 : 0), ey + 3 + (d % 2 == 1 ? 6 : 0), WHITE);
                }
            } else if (board[i] == 3) { sprite(g, "apple", x + 2, y + 1, 14, 16); }
        }
        center(g, text("snake_length", "Length: %s", a.length > 1 ? (int) a[1] : 3), 300, 345, 0xFFDDE9D2);
    }
    private void pong(GuiGraphicsExtractor g) {
        double[] a = state.actors(); if (a.length != 8) return;
        g.pose().pushMatrix(); g.pose().translate(60, 59); g.pose().scale(1.2F, 1.2F);
        g.fill(0, 0, 400, 240, 0xFF000000); g.outline(0, 0, 400, 240, WHITE);
        for (int y = 8; y < 235; y += 14) g.fill(199, y, 202, y + 8, 0xFFCCCCCC);
        number(g, Long.toString(state.score()), 145, 12, 2.8F, WHITE, false); number(g, Long.toString(state.opponent()), 255, 12, 2.8F, WHITE, false);
        double dt = state.phase() == GameSession.RUNNING && a[7] == 0 ? Math.min(.07, (now() - receivedAt) / 1000.0) : 0;
        int x = (int) Math.clamp(a[0] + a[2] * dt, 0, 400), y = (int) Math.clamp(a[1] + a[3] * dt, 3, 237);
        double paddle = a[4] + (state.phase() == GameSession.RUNNING ? Math.clamp(paddleTarget - a[4], -450 * Math.min(.1, (now() - receivedAt) / 1000.0), 450 * Math.min(.1, (now() - receivedAt) / 1000.0)) : 0);
        g.fill(14, (int) paddle - 22, 20, (int) paddle + 22, WHITE); g.fill(380, (int) a[5] - 22, 386, (int) a[5] + 22, WHITE);
        g.fill(x - 3, y - 3, x + 3, y + 3, WHITE);
        if (a[7] > 0 && state.phase() == GameSession.RUNNING) center(g, text("serve", "Ready…"), 200, 150, 0xFFBBBBBB);
        g.pose().popMatrix();
        center(g, state.mode() == 1 ? text("endless_practice", "Endless practice") : text("first11", "First to 11"), 300, 350, WHITE);
    }
    private void whack(GuiGraphicsExtractor g, double mx, double my) {
        int[] b = state.board(); double[] a = state.actors();
        g.fill(155, 61, 445, 345, 0xFFD9AD74); g.fill(161, 67, 439, 339, 0xFFC89459);
        long drift = state.phase() == GameSession.RUNNING ? Math.min(100, now() - receivedAt) : 0;
        for (int i = 0; i < 9; i++) {
            int x = 202 + i % 3 * 98, y = 112 + i / 3 * 91;
            ellipse(g, x, y + 14, 36, 24, 0xFF7A421F); ellipse(g, x, y + 12, 32, 20, 0xFF452A1D);
            if (b[i] == 1 && a.length == 5) {
                double rise = Math.clamp((a[1] + drift) / 120.0, 0, 1), fall = Math.clamp((a[2] - drift) / 140.0, 0, 1);
                int exposed = (int) (44 * Math.min(rise, fall));
                g.enableScissor(x - 33, y - 40, x + 33, y + 19);
                sprite(g, "mole", x - 25, y + 18 - exposed, 50, 53); g.disableScissor();
            } else if (b[i] == 2) { sprite(g, "mole_hit", x - 25, y - 24, 50, 53); }
            ellipse(g, x, y + 23, 34, 8, 0xFF9C6231);
        }
        number(g, "♥".repeat(state.lives()), 300, 351, 1.6F, 0xFFB53D3D, false);
        if (mx >= 155 && mx <= 445 && my >= 61 && my <= 345) {
            g.pose().pushMatrix(); g.pose().translate((float) mx + 13, (float) my - 10);
            g.pose().rotate((float) Math.toRadians(now() - effectAt < 150 ? -50 : 10)); sprite(g, "hammer", -12, -15, 31, 38); g.pose().popMatrix();
        }
    }
    private void mines(GuiGraphicsExtractor g, double mx, double my) {
        MineLayout l = mineLayout(); int[] b = state.board(); double[] a = state.actors(); int w = state.cols() * l.cell;
        bevel(g, l.x - 10, 55, w + 20, state.rows() * l.cell + 59, 0xFFC0C0C0, false);
        bevel(g, l.x - 3, 64, w + 6, 32, 0xFFC0C0C0, true);
        led(g, l.x + 4, 68, a.length >= 2 ? (int) (a[0] - a[1]) : 0); led(g, l.x + w - 52, 68, (int) Math.min(999, elapsed() / 1000));
        bevel(g, 288, 66, 24, 26, 0xFFC0C0C0, false); circle(g, 300, 79, 8, 0xFF111111); circle(g, 300, 79, 7, 0xFFFFDD22);
        if (state.phase() == GameSession.LOST) {
            for (int x : new int[]{296, 302}) {
                g.fill(x, 74, x + 1, 75, DARK); g.fill(x + 2, 74, x + 3, 75, DARK);
                g.fill(x + 1, 75, x + 2, 76, DARK); g.fill(x, 76, x + 1, 77, DARK); g.fill(x + 2, 76, x + 3, 77, DARK);
            }
            g.fill(297, 80, 304, 81, DARK); g.fill(296, 81, 298, 83, DARK); g.fill(303, 81, 305, 83, DARK);
        } else {
            if (state.phase() == GameSession.WON) { g.fill(294, 74, 306, 76, DARK); g.fill(295, 76, 299, 78, DARK); g.fill(301, 76, 305, 78, DARK); }
            else { g.fill(296, 75, 298, 77, DARK); g.fill(302, 75, 304, 77, DARK); }
            g.fill(296, 82, 304, 83, DARK); g.fill(295, 80, 297, 83, DARK); g.fill(303, 80, 305, 83, DARK);
        }
        for (int i = 0; i < b.length; i++) {
            int x = l.x + i % state.cols() * l.cell, y = l.y + i / state.cols() * l.cell;
            if (b[i] < 0 && b[i] != -3 && b[i] != -4 || b[i] == 9) {
                bevel(g, x, y, l.cell, l.cell, 0xFFC0C0C0, false);
                if (b[i] == -2 || b[i] == -5) sprite(g, "flag", x + 2, y + 2, l.cell - 4, l.cell - 4);
                if (b[i] == 9) number(g, "?", x + l.cell / 2, y + 2, Math.min(1.3F, l.cell / 14F), DARK, false);
                if (b[i] == -5) { g.fill(x + 2, y + 2, x + l.cell - 2, y + 4, 0xFFFF0000); g.fill(x + 2, y + l.cell - 4, x + l.cell - 2, y + l.cell - 2, 0xFFFF0000); }
            } else {
                g.fill(x, y, x + l.cell, y + l.cell, b[i] == -4 ? 0xFFFF3030 : 0xFFC0C0C0); g.outline(x, y, l.cell, l.cell, 0xFF909090);
                if (b[i] == -3 || b[i] == -4) sprite(g, "mine", x + 2, y + 2, l.cell - 4, l.cell - 4);
                else if (b[i] > 0) number(g, Integer.toString(b[i]), x + l.cell / 2, y + (l.cell - 10) / 2, Math.min(1.3F, l.cell / 14F), MINE_COLORS[b[i]], false);
            }
            if (cellAt(mx, my) == i && state.phase() == GameSession.RUNNING) g.outline(x, y, l.cell, l.cell, 0xFF555555);
        }
    }
    private void merge(GuiGraphicsExtractor g, double mx, double my) {
        int[] board = state.board(), moves = state.moves(); double[] a = state.actors();
        int x0 = 165, y0 = 65, step = 67, size = 59;
        g.fill(x0 - 9, y0 - 9, x0 + 4 * step + 1, y0 + 4 * step + 1, 0xFFBBADA0);
        for (int i = 0; i < 16; i++) g.fill(x0 + i % 4 * step, y0 + i / 4 * step, x0 + i % 4 * step + size, y0 + i / 4 * step + size, TILE_COLORS[0]);
        double t = Math.clamp((now() - moveAt) / 110.0, 0, 1);
        if (t < 1 && previous != null && moves.length > 0 && state.phase() == GameSession.RUNNING) {
            double eased = 1 - Math.pow(1 - t, 3);
            for (int i = 0; i + 2 < moves.length; i += 3) {
                int from = moves[i], to = moves[i + 1];
                double cx = from % 4 + (to % 4 - from % 4) * eased, cy = from / 4 + (to / 4 - from / 4) * eased;
                tile(g, x0 + (int) (cx * step), y0 + (int) (cy * step), size, moves[i + 2]);
            }
        } else for (int i = 0; i < 16; i++) if (board[i] > 0) {
            double pop = a.length > 0 && (((int) a[0] & (1 << i)) != 0) && now() - moveAt < 230 ? 1 + Math.sin(Math.PI * Math.clamp((now() - moveAt - 110) / 120.0, 0, 1)) * .12 : 1;
            int spawn = a.length > 1 ? (int) a[1] : -1;
            if (i == spawn && now() - moveAt < 220) pop = Math.clamp((now() - moveAt - 80) / 120.0, .25, 1);
            int scaled = (int) (size * pop), offset = (size - scaled) / 2;
            tile(g, x0 + i % 4 * step + offset, y0 + i / 4 * step + offset, scaled, board[i]);
        }
        for (int d = 0; d < 4; d++) button(g, 471 + (d == 1 ? 32 : d == 3 ? -32 : 0), 269 + (d == 0 ? -32 : d == 2 ? 32 : 0), 29, 29,
                Component.literal(new String[]{"↑", "→", "↓", "←"}[d]), mx, my, DARK);
    }
    private void tile(GuiGraphicsExtractor g, int x, int y, int size, int exponent) {
        if (size <= 0) return;
        int color = exponent < TILE_COLORS.length ? TILE_COLORS[exponent] : 0xFF3C3A32;
        g.fill(x, y, x + size, y + size, color);
        String value = Long.toString(1L << exponent); float scale = (float) Math.min(2.9, (size - 8.0) / Math.max(1, font.width(value)));
        number(g, value, x + size / 2, y + size / 2 - (int) (4 * scale), scale, exponent <= 2 ? 0xFF776E65 : 0xFFF9F6F2, false);
    }
    private void overlay(GuiGraphicsExtractor g, double mx, double my) {
        boolean menu = state.phase() == GameSession.MENU, paused = state.phase() == GameSession.PAUSED, won = state.phase() == GameSession.WON;
        g.fill(0, 48, 600, 365, type == GameType.MERGE ? 0xA8FAF8EF : 0x77000000);
        int panel = type == GameType.MINES ? 0xFFC0C0C0 : type == GameType.PONG || type == GameType.SNAKE ? 0xFF20312C : 0xFFF8EDD9;
        int ink = type == GameType.PONG || type == GameType.SNAKE ? WHITE : DARK;
        bevel(g, 130, 88, 340, 242, panel, false);
        center(g, menu ? getTitle() : paused ? text("paused", "Paused") : won ? text("won", "You win!") : text("lost", "Game over"), 300, 105, ink);
        Component help = menu ? Component.translatableWithFallback("game.goosetools." + type.id + ".help", help())
                : paused ? text("pause_help", "Press P or Resume to continue.") : text("result", "Score: %s   Time: %s", state.score(), time(state.elapsed()));
        int y = 130; for (FormattedCharSequence line : font.split(help, 298)) { g.text(font, line, 151, y, ink, false); y += 11; }
        if (menu) {
            button(g, 163, 184, 274, 25, modeText(), mx, my, ink);
            button(g, 163, 217, 274, 25, difficultyText(), mx, my, ink);
            button(g, 205, 266, 190, 30, text("start", "Start game"), mx, my, ink);
        } else {
            if (paused) button(g, 190, 184, 220, 30, text("resume", "Resume"), mx, my, ink);
            else if (won && type == GameType.MERGE) button(g, 190, 184, 220, 30, text("keep_going", "Keep going"), mx, my, ink);
            else if (won && type == GameType.MINES) button(g, 190, 184, 220, 30, text("next_board", "Next board"), mx, my, ink);
            button(g, 190, 228, 220, 28, text("restart", "New game"), mx, my, ink);
            button(g, 190, 269, 220, 28, text("exit", "Close game"), mx, my, ink);
        }
    }
    private Component modeText() {
        boolean natural = type == GameType.FLAPPY || type == GameType.SNAKE || type == GameType.WHACK;
        return text(natural || state.mode() == 1 ? "mode_endless" : "mode_classic", natural || state.mode() == 1 ? "Mode: Endless" : "Mode: Classic");
    }
    private Component difficultyText() {
        int d = state.difficulty();
        if (type == GameType.MINES) return text("mines" + d, new String[]{"Beginner: 9 × 9 / 10 mines", "Intermediate: 16 × 16 / 40 mines", "Expert: 30 × 16 / 99 mines"}[d]);
        if (type == GameType.SNAKE) return text("speed" + d, new String[]{"Speed: Slow", "Speed: Normal", "Speed: Fast"}[d]);
        if (type == GameType.PONG) return text("ai" + d, new String[]{"AI: Easy", "AI: Normal", "AI: Hard"}[d]);
        if (type == GameType.WHACK) return text("pace" + d, new String[]{"Pace: Easy", "Pace: Normal", "Pace: Hard"}[d]);
        return text("original_rules", "Classic rules");
    }
    private String controls() {
        return switch (type) { case FLAPPY -> "Space / click: flap   P: pause"; case SNAKE -> "Arrows / WASD: turn   P: pause"; case PONG -> "Mouse / W S / Up Down: paddle   P: pause"; case WHACK -> "Click a mole   P: pause"; case MINES -> "Left: reveal   Right: flag   Middle / double: chord"; case MERGE -> "Arrows / WASD: move   P: pause"; };
    }
    private String help() {
        return switch (type) { case FLAPPY -> "Flap through the pipes. Each passed pair scores a point. A collision ends the run."; case SNAKE -> "Eat to grow. Avoid walls and your own body. Fill the entire board to win."; case PONG -> "Return the ball against the AI. Classic: first to 11. Endless practice keeps the match going."; case WHACK -> "Hit the moles before they hide. Misses and empty hits cost a life. Three lives, endless waves."; case MINES -> "Reveal all safe cells. Numbers count neighboring mines. First click is safe. Flag mines and chord numbered cells."; case MERGE -> "Slide equal tiles together to reach 2048. Each tile merges once per move. Keep going after winning."; };
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Transform t = transform(); double x = t.x(event.x()), y = t.y(event.y()); pointerX = x; pointerY = y;
        if (inside(x, y, 565, 8, 25, 23)) { onClose(); return true; }
        if (state == null) return true;
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        if (state.phase() == GameSession.MENU) {
            if (!left) return true;
            if (inside(x, y, 163, 184, 274, 25) && (type == GameType.PONG || type == GameType.MINES || type == GameType.MERGE))
                send(GameSession.OPTIONS, (1 - state.mode()) * 3 + state.difficulty(), 0, 0);
            else if (inside(x, y, 163, 217, 274, 25) && type != GameType.FLAPPY && type != GameType.MERGE)
                send(GameSession.OPTIONS, state.mode() * 3 + (state.difficulty() + 1) % 3, 0, 0);
            else if (inside(x, y, 205, 266, 190, 30)) send(GameSession.START, 0, 0, 0);
            return true;
        }
        if (state.phase() != GameSession.RUNNING) {
            if (!left) return true;
            if (inside(x, y, 190, 184, 220, 30)) send(type == GameType.MINES && state.phase() == GameSession.WON ? GameSession.NEXT : GameSession.CONTINUE, 0, 0, 0);
            else if (inside(x, y, 190, 228, 220, 28)) send(GameSession.RETRY, 0, 0, 0);
            else if (inside(x, y, 190, 269, 220, 28)) onClose();
            return true;
        }
        if (left && inside(x, y, 507, 8, 50, 23)) { send(GameSession.PAUSE, 0, 0, 0); return true; }
        switch (type) {
            case FLAPPY -> { if (left && inside(x, y, 205, 53, 191, 297)) flap(); }
            case PONG -> { if (left) movePaddle(x, y); }
            case WHACK -> { if (left) { int cell = cellAt(x, y); if (cell >= 0) { effectAt = now(); send(GameSession.REVEAL, cell, x, y); } } }
            case MINES -> {
                if (left && inside(x, y, 288, 66, 24, 26)) { send(GameSession.RETRY, 0, 0, 0); return true; }
                int cell = cellAt(x, y);
                if (cell >= 0) send(event.button() == InputConstants.MOUSE_BUTTON_RIGHT ? GameSession.FLAG
                        : event.button() == InputConstants.MOUSE_BUTTON_MIDDLE || doubleClick && state.board()[cell] >= 0 ? GameSession.CHORD : GameSession.REVEAL, cell, x, y);
            }
            case MERGE -> { if (left) for (int d = 0; d < 4; d++) if (inside(x, y, 471 + (d == 1 ? 32 : d == 3 ? -32 : 0), 269 + (d == 0 ? -32 : d == 2 ? 32 : 0), 29, 29)) direction(d); }
            default -> {}
        }
        return true;
    }
    @Override public void mouseMoved(double x, double y) {
        Transform t = transform(); pointerX = t.x(x); pointerY = t.y(y);
        if (state != null && state.phase() == GameSession.RUNNING && type == GameType.PONG) movePaddle(pointerX, pointerY);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (type == GameType.PONG) { Transform t = transform(); movePaddle(t.x(e.x()), t.y(e.y())); return true; } return true;
    }
    private void movePaddle(double x, double y) {
        if (!inside(x, y, 60, 59, 480, 288)) return;
        keyboardPaddle = 0; paddleTarget = Math.clamp((y - 59) / 1.2, 22, 218);
        if (now() - lastInput >= 35) { lastInput = now(); send(GameSession.POINT, 0, 0, paddleTarget); }
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) { onClose(); return true; }
        if (state == null) return true;
        if (event.key() == InputConstants.KEY_P) {
            if (state.phase() == GameSession.RUNNING || state.phase() == GameSession.PAUSED) send(state.phase() == GameSession.PAUSED ? GameSession.CONTINUE : GameSession.PAUSE, 0, 0, 0);
            return true;
        }
        if (state.phase() == GameSession.MENU) { if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_SPACE) send(GameSession.START, 0, 0, 0); return true; }
        if (state.phase() != GameSession.RUNNING) return true;
        if (type == GameType.FLAPPY && event.key() == InputConstants.KEY_SPACE) { if (!leftHeld) { leftHeld = true; flap(); } return true; }
        int direction = switch (event.key()) {
            case InputConstants.KEY_UP, InputConstants.KEY_W -> 0; case InputConstants.KEY_RIGHT, InputConstants.KEY_D -> 1;
            case InputConstants.KEY_DOWN, InputConstants.KEY_S -> 2; case InputConstants.KEY_LEFT, InputConstants.KEY_A -> 3; default -> -1;
        };
        if (direction >= 0) {
            if (type == GameType.PONG && (direction == 0 || direction == 2)) keyboardPaddle = direction == 0 ? -1 : 1;
            else if (type == GameType.SNAKE || type == GameType.MERGE) direction(direction);
        }
        return true;
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) leftHeld = false;
        if (event.key() == InputConstants.KEY_W || event.key() == InputConstants.KEY_S || event.key() == InputConstants.KEY_UP || event.key() == InputConstants.KEY_DOWN) keyboardPaddle = 0;
        return true;
    }
    private void flap() {
        send(GameSession.FLAP, 0, 0, 0);
        double dt = Math.min(.12, (now() - birdCorrectionAt) / 1000.0);
        predictedBirdY += predictedBirdV * dt + 245 * dt * dt; predictedBirdV = -155; birdCorrectionAt = now(); sound(1);
    }
    private void direction(int d) { send(GameSession.DIRECTION, d, 0, 0); }
    private void send(int action, int value, double x, double y) {
        if (ClientPlayNetworking.canSend(GamePackets.Input.TYPE)) ClientPlayNetworking.send(new GamePackets.Input(open.sessionId(), sequence++, action, value, Math.clamp(x, 0, WIDTH), Math.clamp(y, 0, HEIGHT)));
    }
    public int cellAt(double x, double y) {
        if (state == null) return -1;
        if (type == GameType.MINES) {
            MineLayout l = mineLayout(); int col = (int) Math.floor((x - l.x) / l.cell), row = (int) Math.floor((y - l.y) / l.cell);
            return col >= 0 && col < state.cols() && row >= 0 && row < state.rows() ? row * state.cols() + col : -1;
        }
        if (type == GameType.WHACK) for (int i = 0; i < 9; i++) {
            double dx = (x - (202 + i % 3 * 98)) / 37, dy = (y - (112 + i / 3 * 91)) / 38;
            if (dx * dx + dy * dy <= 1) return i;
        }
        return -1;
    }
    public double[] cellCenter(int cell) {
        if (type == GameType.MINES) { MineLayout l = mineLayout(); return new double[]{l.x + (cell % state.cols() + .5) * l.cell, l.y + (cell / state.cols() + .5) * l.cell}; }
        return new double[]{202 + cell % 3 * 98, 112 + cell / 3 * 91};
    }
    private MineLayout mineLayout() {
        int size = state.cols() == 9 ? 25 : state.cols() == 16 ? 15 : 16;
        return new MineLayout((600 - state.cols() * size) / 2, 104, size);
    }
    private record MineLayout(int x, int y, int cell) {}
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { cancel(); minecraft.setScreenAndShow(null); }
    @Override public void removed() { cancel(); }
    private void cancel() { if (!closing) { closing = true; send(GameSession.CANCEL, -1, 0, 0); } }
    public void closeFromServer() { closing = true; if (minecraft != null && minecraft.gui.screen() == this) minecraft.setScreenAndShow(null); }
    private void sound(int effect) {
        if (minecraft == null || effect == 0) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(effect == 2 || effect == 6 ? SoundEvents.NOTE_BLOCK_PLING
                : effect == 5 ? SoundEvents.NOTE_BLOCK_BASS : effect == 1 ? SoundEvents.NOTE_BLOCK_HAT : SoundEvents.UI_BUTTON_CLICK,
                effect == 7 ? 1.3F : effect == 6 ? 1.7F : 1.0F));
    }
    private Component text(String key, String fallback, Object... args) { return Component.translatableWithFallback("game.goosetools.ui." + key, fallback, args); }
    private void center(GuiGraphicsExtractor g, Component s, int x, int y, int ink) { g.text(font, s, x - font.width(s) / 2, y, ink, false); }
    private void title(GuiGraphicsExtractor g, Component s, int x, int y, float scale, int ink) { g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale); g.text(font, s, 0, 0, ink, false); g.pose().popMatrix(); }
    private void number(GuiGraphicsExtractor g, String s, int x, int y, float scale, int ink, boolean shadow) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale); g.text(font, s, -font.width(s) / 2, 0, ink, shadow); g.pose().popMatrix();
    }
    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component s, double mx, double my, int ink) {
        int color = ink == WHITE ? inside(mx, my, x, y, w, h) ? 0xFF4B6258 : 0xFF334B40 : inside(mx, my, x, y, w, h) ? 0xFFD4C6AF : 0xFFDDD1BC;
        if (type == GameType.MINES) color = inside(mx, my, x, y, w, h) ? 0xFFD4D4D4 : 0xFFC0C0C0;
        bevel(g, x, y, w, h, color, false); center(g, s, x + w / 2, y + (h - 8) / 2, ink);
    }
    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, boolean pressed) {
        g.fill(x, y, x + w, y + h, color); int bright = pressed ? 0xFF777777 : 0xFFEEEEEE, dark = pressed ? 0xFFEEEEEE : 0xFF777777;
        g.fill(x, y, x + w, y + 2, bright); g.fill(x, y, x + 2, y + h, bright); g.fill(x + w - 2, y, x + w, y + h, dark); g.fill(x, y + h - 2, x + w, y + h, dark);
    }
    private void led(GuiGraphicsExtractor g, int x, int y, int value) {
        String s = value < 0 ? "-" + String.format(Locale.ROOT, "%02d", Math.min(99, -value)) : String.format(Locale.ROOT, "%03d", Math.min(999, value));
        g.fill(x, y, x + 48, y + 24, 0xFF220000);
        int[] masks = {63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
        for (int i = 0; i < 3; i++) {
            char c = s.charAt(i); int mask = c == '-' ? 64 : masks[c - '0'], dx = x + 3 + i * 15;
            int[][] r = {{2,0,10,2},{11,2,13,10},{11,12,13,20},{2,20,10,22},{0,12,2,20},{0,2,2,10},{2,10,10,12}};
            for (int bit = 0; bit < 7; bit++) g.fill(dx + r[bit][0], y + 1 + r[bit][1], dx + r[bit][2], y + 1 + r[bit][3], (mask & (1 << bit)) != 0 ? 0xFFFF1818 : 0xFF4A0808);
        }
    }
    private static void sprite(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
        g.blit(Identifier.fromNamespaceAndPath("goosetools", "textures/gui/games/" + name + ".png"), x, y, x + w, y + h, 0, 1, 0, 1);
    }
    private static void circle(GuiGraphicsExtractor g, int x, int y, int r, int color) { ellipse(g, x, y, r, r, color); }
    private static void ellipse(GuiGraphicsExtractor g, int x, int y, int rx, int ry, int color) {
        for (int row = -ry; row <= ry; row++) { int half = (int) (rx * Math.sqrt(Math.max(0, 1 - row * row / (double) (ry * ry)))); g.fill(x - half, y + row, x + half + 1, y + row + 1, color); }
    }
    private static boolean inside(double x, double y, int rx, int ry, int w, int h) { return x >= rx && x < rx + w && y >= ry && y < ry + h; }
    private static long now() { return System.nanoTime() / 1_000_000; }
    private static String time(long millis) { return String.format(Locale.ROOT, "%02d:%02d", millis / 60000, millis / 1000 % 60); }
    public Transform transform() { double s = Math.max(.1, Math.min(1.7, Math.min((width - 20.0) / WIDTH, (height - 20.0) / HEIGHT))); return new Transform((width - WIDTH * s) / 2, (height - HEIGHT * s) / 2, s); }
    public record Transform(double x, double y, double scale) { public double x(double px) { return (px - x) / scale; } public double y(double py) { return (py - y) / scale; } }
}
