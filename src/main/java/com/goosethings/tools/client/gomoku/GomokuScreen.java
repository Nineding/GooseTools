package com.goosethings.tools.client.gomoku;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.gomoku.GomokuPackets;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.util.*;

public final class GomokuScreen extends Screen implements ProtectedInputScreen {
    public static final int WIDTH = 640, HEIGHT = 420, GRID_X = 48, GRID_Y = 92, STEP = 20;
    private static final int INK = 0xFFF5E8D0, MUTED = 0xFFBDAF97, GOLD = 0xFFEABB63, DARK = 0xFF181D22;
    private final List<Hit> hits = new ArrayList<>();
    private int difficulty = 1, color = 1, page;
    private boolean resignConfirm;
    private double mx, my;
    private record Hit(int x, int y, int w, int h, Runnable action) { boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; } }
    public GomokuScreen() { super(text("title", "Gomoku")); }
    public void updated(GomokuPackets.View old, GomokuPackets.View next) {
        if (old == null || old.position().id() != next.position().id() || next.position().outcome() != 0) resignConfirm = false;
        if (old != null && old.position().id() == next.position().id() && old.position().last() != next.position().last() && next.position().last() >= 0)
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("block.stone.place")), 1.2F, .55F));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xD9101419);
        Transform t = transform(); mx = t.x(mouseX); my = t.y(mouseY); hits.clear();
        g.pose().pushMatrix(); g.pose().translate((float) t.x, (float) t.y); g.pose().scale((float) t.scale, (float) t.scale);
        g.fill(3, 5, WIDTH + 3, HEIGHT + 5, 0x77000000);
        g.fill(0, 0, WIDTH, HEIGHT, DARK); g.fill(0, 0, WIDTH, 3, GOLD);
        title(g, getTitle(), 22, 17, 1.65F, INK);
        label(g, text("subtitle", "FIVE IN A ROW  /  60 SECONDS PER MOVE"), 125, 23, MUTED);
        button(g, 541, 14, 78, 25, text("hide", "Hide"), true, this::onClose);
        GomokuPackets.View v = GomokuClient.view();
        if (v == null) label(g, text("loading", "Waiting for server…"), 24, 80, INK);
        else if (v.position().id() == 0) menu(g, v);
        else board(g, v.position());
        int notice = GomokuClient.notice();
        if (notice != 0) {
            g.fill(12, 400, 628, 418, 0xFF55382A);
            label(g, switch (notice) {
                case 1 -> text("unavailable", "Available in the lobby or after elimination only.");
                case 2 -> text("busy", "That player is busy. Try again shortly.");
                case 4 -> text("expired", "This invitation has expired.");
                default -> text("invalid", "The action is no longer valid. The board has been refreshed.");
            }, 21, 405, INK);
        }
        g.pose().popMatrix();
    }
    private void menu(GuiGraphicsExtractor g, GomokuPackets.View v) {
        panel(g, 20, 59, 342, 286); panel(g, 376, 59, 244, 286);
        title(g, text("players", "Invite a player"), 34, 74, 1.15F, GOLD);
        int pages = Math.max(1, (v.players().size() + 6) / 7); page = Math.min(page, pages - 1);
        if (v.players().isEmpty()) {
            label(g, text("no_players", "No free opponents right now."), 35, 134, MUTED);
            label(g, text("try_ai", "Try a game against the computer."), 35, 152, MUTED);
        }
        for (int row = 0; row < 7 && page * 7 + row < v.players().size(); row++) {
            GomokuPackets.Person person = v.players().get(page * 7 + row);
            button(g, 34, 101 + row * 29, 314, 24, Component.literal(person.name()), v.invite() == 0,
                    () -> GomokuClient.action(GomokuPackets.INVITE, 0, 0, person.id()));
        }
        button(g, 34, 310, 44, 23, text("previous", "<"), page > 0, () -> page--);
        center(g, text("page", "%s / %s", page + 1, pages), 190, 317, MUTED);
        button(g, 304, 310, 44, 23, text("next", ">"), page + 1 < pages, () -> page++);
        title(g, text("computer", "Play the computer"), 392, 74, 1.15F, GOLD);
        label(g, text("difficulty", "Difficulty"), 392, 106, MUTED);
        for (int d = 0; d < 3; d++) { final int choice = d; button(g, 392 + d * 70, 124, 65, 27, difficultyName(d), true, () -> difficulty = choice); if (difficulty == d) g.fill(392 + d * 70, 150, 457 + d * 70, 152, GOLD); }
        label(g, text("your_color", "Your stones"), 392, 169, MUTED);
        for (int c = 1; c <= 2; c++) { final int choice = c; button(g, 392 + (c - 1) * 105, 187, 100, 27, colorName(c), true, () -> color = choice); if (color == c) g.fill(392 + (c - 1) * 105, 213, 492 + (c - 1) * 105, 215, GOLD); }
        button(g, 392, 233, 205, 30, text("start", "Start game"), v.invite() == 0,
                () -> GomokuClient.action(GomokuPackets.AI, color, difficulty, GomokuPackets.NONE));
        label(g, text("rules_one", "Black starts. Five or more wins."), 392, 281, MUTED);
        label(g, text("rules_two", "No forbidden moves. 60s per turn."), 392, 298, MUTED);
        label(g, text("rules_three", "Hidden boards keep counting down."), 392, 315, MUTED);
        panel(g, 20, 358, 600, 40);
        if (v.invite() == 0) {
            label(g, text("waiting_hint", "Invite a friend, or warm up against the computer."), 34, 374, MUTED);
        } else if (v.inviteDirection() == 2) {
            label(g, text("invitation", "%s invites you · %ss", v.inviteName(), (GomokuClient.inviteRemaining() + 999) / 1000), 34, 374, GOLD);
            button(g, 440, 366, 77, 24, text("accept", "Accept"), true, () -> GomokuClient.action(GomokuPackets.ACCEPT));
            button(g, 526, 366, 77, 24, text("decline", "Decline"), true, () -> GomokuClient.action(GomokuPackets.DECLINE));
        } else {
            label(g, text("inviting", "Waiting for %s · %ss", v.inviteName(), (GomokuClient.inviteRemaining() + 999) / 1000), 34, 374, MUTED);
            button(g, 526, 366, 77, 24, text("cancel", "Cancel"), true, () -> GomokuClient.action(GomokuPackets.CANCEL_INVITE));
        }
    }
    private void board(GuiGraphicsExtractor g, GomokuPackets.Position p) {
        g.blit(texture("board"), 20, 64, 356, 400, 0, 1, 0, 1);
        for (int n = 0; n < 15; n++) {
            g.fill(GRID_X + n * STEP, GRID_Y, GRID_X + n * STEP + 1, GRID_Y + 281, 0xBD694019);
            g.fill(GRID_X, GRID_Y + n * STEP, GRID_X + 281, GRID_Y + n * STEP + 1, 0xBD694019);
        }
        for (int x : new int[]{3, 7, 11}) for (int y : new int[]{3, 7, 11}) {
            if (x != y && x + y != 14) continue;
            g.fill(GRID_X + x * STEP - 2, GRID_Y + y * STEP - 2, GRID_X + x * STEP + 3, GRID_Y + y * STEP + 3, 0xFF694019);
        }
        int[] cells = p.board();
        for (int i = 0; i < cells.length; i++) if (cells[i] != 0) stone(g, i, cells[i]);
        int hover = cellAt(mx, my);
        if (!resignConfirm && p.outcome() == 0 && p.turn() == p.color() && hover >= 0 && cells[hover] == 0)
            marker(g, hover, 0xFF447B55, 7);
        if (p.last() >= 0) marker(g, p.last(), 0xFFE3AE41, 3);
        int[] line = p.line();
        if (line.length >= 5) {
            int first = line[0], last = line[line.length - 1];
            int x = GRID_X + first % 15 * STEP, y = GRID_Y + first / 15 * STEP;
            int dx = Integer.signum(last % 15 - first % 15), dy = Integer.signum(last / 15 - first / 15);
            int distance = Math.max(Math.abs(last % 15 - first % 15), Math.abs(last / 15 - first / 15)) * STEP;
            for (int n = 0; n <= distance; n++) g.fill(x + dx * n - 1, y + dy * n - 1, x + dx * n + 2, y + dy * n + 2, 0xD96CBA6C);
        }
        panel(g, 376, 64, 244, 336);
        for (int c = 1; c <= 2; c++) {
            int y = c == 1 ? 80 : 133;
            label(g, colorName(c), 392, y, GOLD);
            String name = c == 1 ? p.black() : p.white();
            Component player = name.isEmpty() ? text("ai_name", "Computer · %s", difficultyName(p.difficulty())) : Component.literal(name);
            label(g, player, 392, y + 18, INK);
            if (p.color() == c) label(g, text("you", "YOU"), 560, y, MUTED);
        }
        g.fill(392, 183, 604, 184, 0xFF3E4244);
        if (p.outcome() == 0) {
            title(g, p.turn() == p.color() ? text("your_turn", "Your turn") : text("their_turn", "Opponent's turn"), 392, 201, 1.25F, INK);
            int seconds = (GomokuClient.remaining() + 999) / 1000;
            title(g, text("seconds", "%ss", seconds), 392, 229, 2.4F, seconds <= 10 ? 0xFFFF8D78 : GOLD);
            label(g, text("clock_hint", "Timeout loses this game."), 392, 260, MUTED);
            if (resignConfirm) {
                label(g, text("resign_confirm", "Resign this game?"), 392, 296, GOLD);
                button(g, 392, 318, 100, 27, text("confirm", "Confirm"), true, () -> { GomokuClient.action(GomokuPackets.RESIGN); resignConfirm = false; });
                button(g, 501, 318, 100, 27, text("cancel", "Cancel"), true, () -> resignConfirm = false);
            } else button(g, 392, 298, 209, 28, text("resign", "Resign"), true, () -> resignConfirm = true);
            label(g, text("hide_hint", "Esc hides the board; time still runs."), 392, 365, MUTED);
        } else {
            Component result = p.outcome() == 3 ? text("draw", "Draw") : p.outcome() == 4 ? text("cancelled", "Game cancelled")
                    : p.outcome() == p.color() ? text("won", "You win!") : text("lost", "You lose");
            title(g, result, 392, 205, 1.65F, GOLD);
            label(g, switch (p.reason()) {
                case 1 -> text("reason_full", "The board is full.");
                case 2 -> text("reason_timeout", "Move time expired.");
                case 3 -> text("reason_resign", "A player resigned.");
                case 4 -> text("reason_disconnect", "An opponent disconnected.");
                case 5 -> text("reason_cancel", "The Goose Goose Duck session changed.");
                default -> text("reason_five", "Five stones in a row.");
            }, 392, 238, MUTED);
            int flag = 1 << (p.color() - 1);
            boolean asked = (p.rematchMask() & flag) != 0;
            button(g, 392, 292, 209, 28, asked ? text("rematch_wait", "Waiting for opponent…") : text("rematch", "Play again · swap colors"),
                    !asked && p.reason() != 4 && p.outcome() != 4, () -> GomokuClient.action(GomokuPackets.REMATCH));
            button(g, 392, 331, 209, 28, text("return", "Return to Gomoku lobby"), true, () -> GomokuClient.action(GomokuPackets.LEAVE));
        }
    }
    private void stone(GuiGraphicsExtractor g, int cell, int color) {
        int x = GRID_X + cell % 15 * STEP, y = GRID_Y + cell / 15 * STEP;
        g.blit(texture("stones"), x - 12, y - 12, x + 12, y + 12, color == 1 ? 0 : .5F, color == 1 ? .5F : 1, 0, 1);
    }
    private void marker(GuiGraphicsExtractor g, int cell, int ink, int radius) {
        int x = GRID_X + cell % 15 * STEP, y = GRID_Y + cell / 15 * STEP;
        g.fill(x - radius, y - radius, x + radius + 1, y - radius + 1, ink); g.fill(x - radius, y + radius, x + radius + 1, y + radius + 1, ink);
        g.fill(x - radius, y - radius, x - radius + 1, y + radius, ink); g.fill(x + radius, y - radius, x + radius + 1, y + radius, ink);
    }
    public static int cellAt(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < GRID_X - 10 || y < GRID_Y - 10 || x >= GRID_X + 290 || y >= GRID_Y + 290) return -1;
        return (int) Math.floor((y - GRID_Y + 10) / STEP) * 15 + (int) Math.floor((x - GRID_X + 10) / STEP);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        Transform t = transform(); double x = t.x(event.x()), y = t.y(event.y());
        for (Hit hit : List.copyOf(hits)) if (hit.contains(x, y)) { hit.action.run(); return true; }
        var view = GomokuClient.view(); int cell = cellAt(x, y);
        if (view != null && view.position().id() != 0 && view.position().outcome() == 0 && !resignConfirm
                && view.position().color() == view.position().turn() && cell >= 0 && view.position().board()[cell] == 0)
            GomokuClient.action(GomokuPackets.MOVE, cell, 0, GomokuPackets.NONE);
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && resignConfirm) { resignConfirm = false; return true; }
        if (GomokuClient.matchesKey(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { GomokuClient.hide(); minecraft.setScreenAndShow(null); }
    @Override public void removed() { GomokuClient.hide(); }
    @Override public boolean isPauseScreen() { return false; }
    public Transform transform() {
        double scale = Math.max(.1, Math.min(1.7, Math.min((width - 16.0) / WIDTH, (height - 16.0) / HEIGHT)));
        return new Transform((width - WIDTH * scale) / 2, (height - HEIGHT * scale) / 2, scale);
    }
    public record Transform(double x, double y, double scale) { public double x(double value) { return (value - x) / scale; } public double y(double value) { return (value - y) / scale; } }
    private static Component text(String key, String fallback, Object... args) { return GomokuClient.text(key, fallback, args); }
    private static Component colorName(int color) { return color == 1 ? text("black", "Black") : text("white", "White"); }
    private static Component difficultyName(int d) { return switch (d) { case 0 -> text("easy", "Easy"); case 2 -> text("hard", "Hard"); default -> text("normal", "Normal"); }; }
    private static Identifier texture(String name) { return Identifier.fromNamespaceAndPath("goosetools", "textures/gui/gomoku/" + name + ".png"); }
    private void label(GuiGraphicsExtractor g, Component text, int x, int y, int ink) {
        // The CJK font needs roughly sixteen physical pixels to retain its strokes.
        float size = Math.min(1.2F, (WIDTH - 22F - x) / Math.max(1, font.width(text)));
        title(g, text, x, y, size, ink);
    }
    private void center(GuiGraphicsExtractor g, Component text, int x, int y, int ink) { title(g, text, x - Math.round(font.width(text) * .6F), y, 1.2F, ink); }
    private void title(GuiGraphicsExtractor g, Component text, int x, int y, float scale, int ink) { g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale); g.text(font, text, 0, 0, ink, false); g.pose().popMatrix(); }
    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) { g.fill(x, y, x + w, y + h, 0xFF242A30); g.fill(x, y, x + w, y + 1, 0xFF45494A); }
    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean enabled, Runnable action) {
        Hit hit = new Hit(x, y, w, h, action); boolean hover = hit.contains(mx, my);
        g.fill(x, y, x + w, y + h, !enabled ? 0xFF303438 : hover ? 0xFF645137 : 0xFF3B4147);
        g.fill(x, y + h - 1, x + w, y + h, enabled && hover ? GOLD : 0xFF53585B);
        float size = Math.min(1.2F, (w - 12F) / Math.max(1, font.width(label)));
        title(g, label, x + Math.round((w - font.width(label) * size) / 2), y + Math.round((h - 8 * size) / 2), size, enabled ? INK : 0xFF7B7F80);
        if (enabled) hits.add(hit);
    }
}
