package com.goosethings.tools.client.task;

import com.goosethings.tools.task.TaskLayout;
import com.goosethings.tools.task.TaskPackets;
import com.goosethings.tools.task.TaskSession;
import com.goosethings.tools.task.TaskType;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/** A dedicated, non-pausing task panel with one virtual coordinate space for drawing and input. */
public final class TaskScreen extends Screen {
    private static final int INK = 0xFFECF2FF, MUTED = 0xFFA5B4D0, GREEN = 0xFF60E5A5,
            RED = 0xFFFF7082, EDGE = 0xFF647396;
    private static final int[] WIRE_COLORS = {0xFFFF7588, 0xFF67BAFF, 0xFFFFD066, 0xFFBB91FF};
    private static final String[] FEEDBACK = {"waiting", "play", "success", "miss", "wrong_wire", "insert_card",
            "card_ready", "too_fast", "too_slow", "incomplete_swipe", "drop_in_bin", "align_knob"};
    private static final String[] FALLBACK = {"Waiting for the server…", "Keep going!", "Task complete!",
            "Missed! Progress reset.", "Wrong connector. Try the matching symbol.", "Click the card to insert it.",
            "Drag the card right in 0.6–1.2 seconds.", "Too fast! Swipe more slowly.", "Too slow! Swipe faster.",
            "Swipe all the way right along the slot.", "Drop the garbage inside the bin.", "Hold on the target for 0.6 seconds."};
    private final TaskPackets.Open open;
    private final TaskType type;
    private final TaskLayout layout;
    private final double[] garbageX, garbageY, knobAngles;
    private TaskPackets.State state;
    private boolean readySent, closing;
    private int sequence, dragging = -1;
    private double pointerX, pointerY, grabX, grabY, cardX = 65;
    private long localStart, lastMove, feedbackAt, localKnobSince = -1;
    private int previousProgress;

    public TaskScreen(TaskPackets.Open open) {
        super(Component.translatableWithFallback("task.goosetools." + TaskType.values()[open.task()].id + ".title",
                TaskType.values()[open.task()].fallback));
        this.open = open; type = TaskType.values()[open.task()]; layout = new TaskLayout(open.seed());
        garbageX = layout.garbageX.clone(); garbageY = layout.garbageY.clone(); knobAngles = layout.knobInitial.clone();
    }

    public long sessionId() { return open.sessionId(); }
    public TaskType taskType() { return type; }
    public TaskPackets.State currentState() { return state; }
    TaskLayout taskLayout() { return layout; }
    long animationElapsed() { return elapsed(); }
    @Override protected void init() {
        if (!readySent) { readySent = true; send(TaskSession.READY, -1, 0, 0); }
    }

    public void apply(TaskPackets.State next) {
        if (next.sessionId() != open.sessionId()) return;
        if (state == null || !state.started()) localStart = now() - next.elapsed();
        if (state == null || next.feedback() != state.feedback() || next.progress() != state.progress()) feedbackAt = now();
        if (type == TaskType.SWIPE && next.feedback() >= TaskSession.TOO_FAST && next.feedback() <= TaskSession.INCOMPLETE_SWIPE) {
            dragging = -1; cardX = 65;
        }
        if (type == TaskType.KNOBS && dragging >= 0 && (next.mask() & (1 << dragging)) != 0) {
            knobAngles[dragging] = layout.knobTargets[dragging]; dragging = -1; localKnobSince = -1;
        }
        previousProgress = state == null ? 0 : state.progress();
        state = next;
        if (next.progress() > previousProgress || next.complete()) clickSound();
        if (next.complete()) dragging = -1;
    }

    @Override public void tick() {
        if (dragging >= 0 && (type == TaskType.KNOBS || type == TaskType.SWIPE) && now() - lastMove >= 50) {
            send(TaskSession.MOVE, dragging, pointerX, pointerY); lastMove = now();
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAE080C18);
        Transform transform = transform();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) transform.x, (float) transform.y);
        graphics.pose().scale((float) transform.scale, (float) transform.scale);
        double mx = transform.virtualX(mouseX), my = transform.virtualY(mouseY);
        graphics.fill(3, 5, 423, 325, 0x66000000);
        graphics.fill(0, 0, 420, 320, 0xFF202C49);
        graphics.outline(0, 0, 420, 320, EDGE);
        graphics.fill(1, 1, 419, 46, 0xFF172039);
        graphics.fill(1, 46, 419, 48, 0xFFB294E8);
        graphics.text(font, getTitle(), 18, 14, INK, false);
        graphics.text(font, text("trial", "Task trial"), 18, 29, MUTED, false);
        graphics.fill(382, 12, 407, 36, TaskLayout.CLOSE.contains(mx, my) ? 0xFFBC546E : 0xFF7C3E58);
        center(graphics, text("close", "×"), 394, 19, INK);
        String elapsed = String.format(Locale.ROOT, "%.1f", elapsed() / 1000.0);
        Component timer = text("time", "%s s", elapsed);
        graphics.text(font, timer, 367 - font.width(timer), 19, MUTED, false);
        String help = switch (type) {
            case TIMING -> "Click the button / press Space while the pointer is green.";
            case WIRES -> "Drag each left connector to its matching right connector.";
            case SWIPE -> "Insert the card, then drag it right at a steady speed.";
            case GARBAGE -> "Drag all six pieces of garbage into the bin.";
            case KNOBS -> "Drag around each knob; hold the target for 0.6 seconds.";
        };
        int helpY = 56;
        for (FormattedCharSequence line : font.split(Component.translatableWithFallback(
                "task.goosetools." + type.id + ".help", help), 382)) {
            graphics.text(font, line, 18, helpY, MUTED, false); helpY += 10;
        }
        graphics.fill(18, 80, 402, 265, 0xFF151E34);
        graphics.outline(18, 80, 384, 185, 0xFF3F4E70);
        switch (type) {
            case TIMING -> drawTiming(graphics, mx, my);
            case WIRES -> drawWires(graphics);
            case SWIPE -> drawSwipe(graphics);
            case GARBAGE -> drawGarbage(graphics);
            case KNOBS -> drawKnobs(graphics);
        }
        int feedback = state == null ? TaskSession.WAITING : state.feedback();
        int color = feedback == TaskSession.MISS || feedback == TaskSession.WRONG_WIRE
                || (feedback >= TaskSession.TOO_FAST && feedback <= TaskSession.DROP_IN_BIN) ? RED : MUTED;
        if (now() - feedbackAt > 2200 && state != null && !state.complete()
                && type != TaskType.SWIPE && feedback != TaskSession.WAITING) { feedback = TaskSession.PLAY; color = MUTED; }
        center(graphics, text(FEEDBACK[feedback], FALLBACK[feedback]), 210, 274, color);
        int progress = state == null ? 0 : state.progress();
        graphics.fill(18, 296, 335, 306, 0xFF0D1426);
        graphics.fill(19, 297, 19 + (int) (315.0 * progress / type.total), 305, GREEN);
        graphics.text(font, text("progress", "%s / %s", progress, type.total), 348, 296, INK, false);
        if (state != null && state.complete()) drawSuccess(graphics, mx, my);
        graphics.pose().popMatrix();
    }

    private void drawTiming(GuiGraphicsExtractor g, double mx, double my) {
        circle(g, 173, 164, 78, 0xFF101728);
        circle(g, 173, 164, 74, EDGE);
        circle(g, 173, 164, 70, 0xFF415074);
        for (int degrees = 0; degrees < 360; degrees += 2) {
            double rad = Math.toRadians(degrees - 90);
            int x = 173 + (int) (Math.cos(rad) * 61), y = 164 + (int) (Math.sin(rad) * 61);
            g.fill(x - 5, y - 5, x + 5, y + 5, TaskLayout.inGreen(degrees) ? GREEN : 0xFF273453);
        }
        circle(g, 173, 164, 53, 0xFF415074);
        double angle = Math.toRadians(layout.timingAngle(elapsed()) - 90);
        int tipX = 173 + (int) (Math.cos(angle) * 57), tipY = 164 + (int) (Math.sin(angle) * 57);
        line(g, 173, 164, tipX, tipY, 3, INK);
        circle(g, 173, 164, 8, 0xFF101728); circle(g, 173, 164, 5, 0xFFB294E8);
        boolean hover = TaskLayout.near(mx, my, 173, 243, 17);
        circle(g, 173, 243, 18, EDGE);
        circle(g, 173, 243, 14, hover ? 0xFFFF829B : 0xFFAA4769);
        g.fill(153, 258, 193, 259, 0xFF573F60);
        g.fill(309, 91, 350, 252, EDGE); g.fill(313, 95, 346, 248, 0xFF0D1426);
        int progress = state == null ? 0 : state.progress();
        g.fill(315, 246 - 149 * progress / type.total, 344, 246, GREEN);
        for (int i = 1; i < type.total; i++) g.fill(313, 246 - 149 * i / type.total, 346, 247 - 149 * i / type.total, 0xFF253148);
    }

    private void drawWires(GuiGraphicsExtractor g) {
        g.fill(41, 91, 89, 250, 0xFF2A3654); g.fill(331, 91, 379, 250, 0xFF2A3654);
        for (int left = 0; left < 4; left++) {
            if (masked(left)) {
                for (int right = 0; right < 4; right++) if (layout.rightWires[right] == left)
                    wire(g, 68, TaskLayout.wireY(left), 352, TaskLayout.wireY(right), WIRE_COLORS[left]);
            }
        }
        if (dragging >= 0) wire(g, 68, TaskLayout.wireY(dragging), (int) pointerX, (int) pointerY, WIRE_COLORS[dragging]);
        for (int i = 0; i < 4; i++) {
            terminal(g, 68, TaskLayout.wireY(i), i, masked(i));
            terminal(g, 352, TaskLayout.wireY(i), layout.rightWires[i], masked(layout.rightWires[i]));
        }
    }

    private void terminal(GuiGraphicsExtractor g, int x, int y, int index, boolean done) {
        circle(g, x, y, 13, 0xFF091021); circle(g, x, y, 10, WIRE_COLORS[index]);
        center(g, Component.literal(Integer.toString(index + 1)), x, y - 4, 0xFF162039);
        if (done) circle(g, x + 15, y - 10, 3, GREEN);
    }

    private void wire(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        line(g, x1, y1, x2, y2, 5, 0xFF080E1C); line(g, x1, y1, x2, y2, 2, color);
    }

    private void drawSwipe(GuiGraphicsExtractor g) {
        g.fill(49, 104, 371, 185, 0xFF364563); g.outline(49, 104, 322, 81, EDGE);
        g.fill(63, 133, 357, 179, 0xFF090F1F);
        for (int x = 130; x < 340; x += 24) {
            line(g, x, 152, x + 7, 158, 1, 0xFF687895); line(g, x + 7, 158, x, 164, 1, 0xFF687895);
        }
        center(g, text("reader", "ACCESS READER"), 210, 115, MUTED);
        circle(g, 350, 118, 4, state != null && state.complete() ? GREEN : 0xFFFFD066);
        boolean inserted = state != null && state.cardInserted();
        if (inserted) drawCard(g, (int) cardX, 139, 54, 32);
        else drawCard(g, 44, 190, 90, 54);
        center(g, text("swipe_speed", "Accepted swipe: 0.6–1.2 s"), 248, 211, MUTED);
        if (dragging >= 0) {
            g.fill(143, 231, 354, 239, 0xFF0D1426);
            g.fill(144, 232, 144 + (int) (208 * Math.clamp((cardX - 65) / 253, 0, 1)), 238, 0xFF67BAFF);
        }
    }

    private void drawCard(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFE2E8F5); g.outline(x, y, w, h, 0xFF92A5C8);
        g.fill(x + 2, y + 6, x + w - 2, y + 13, 0xFF243047);
        g.fill(x + 9, y + 18, x + 18, y + 26, 0xFFC9A45F);
        g.fill(x + 24, y + 20, x + w - 7, y + 23, 0xFF7586A5);
        if (h > 40) g.text(font, text("card", "ID CARD"), x + 8, y + 36, 0xFF394762, false);
    }

    private void drawGarbage(GuiGraphicsExtractor g) {
        g.fill(313, 108, 381, 226, 0xFF334962); g.outline(313, 108, 68, 118, EDGE);
        g.fill(305, 101, 389, 116, 0xFF67BAFF); g.fill(319, 104, 375, 112, 0xFF0D1527);
        for (int x = 324; x <= 366; x += 14) g.fill(x, 134, x + 3, 211, 0xFF243650);
        center(g, text("bin", "BIN"), 347, 169, INK);
        center(g, text("drop_here", "DROP HERE"), 347, 237, MUTED);
        for (int i = 0; i < 6; i++) if (!masked(i) && i != dragging) drawTrash(g, i, (int) garbageX[i], (int) garbageY[i]);
        if (dragging >= 0) drawTrash(g, dragging, (int) garbageX[dragging], (int) garbageY[dragging]);
    }

    private void drawTrash(GuiGraphicsExtractor g, int i, int x, int y) {
        int color = WIRE_COLORS[i % 4];
        if (i % 3 == 0) {
            g.fill(x - 7, y - 13, x + 8, y + 13, color); g.fill(x - 4, y - 18, x + 5, y - 12, 0xFFE2E8F5);
            g.fill(x - 7, y - 1, x + 8, y + 5, 0xFF31476A);
        } else if (i % 3 == 1) {
            g.fill(x - 12, y - 12, x + 12, y + 13, 0xFFE2E8F5);
            line(g, x - 7, y - 4, x + 7, y - 4, 1, 0xFF8393AF);
            line(g, x - 7, y + 3, x + 4, y + 3, 1, 0xFF8393AF);
        } else {
            circle(g, x, y, 13, color); circle(g, x - 3, y - 3, 5, 0xFF344360);
            g.fill(x + 7, y - 15, x + 14, y - 9, 0xFF60E5A5);
        }
    }

    private void drawKnobs(GuiGraphicsExtractor g) {
        for (int i = 0; i < 3; i++) {
            int cx = TaskLayout.knobX(i), cy = 162;
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle - 90);
                line(g, cx + (int) (Math.cos(rad) * 43), cy + (int) (Math.sin(rad) * 43),
                        cx + (int) (Math.cos(rad) * 48), cy + (int) (Math.sin(rad) * 48), 1, EDGE);
            }
            for (int angle = -8; angle <= 8; angle += 2) {
                double rad = Math.toRadians(layout.knobTargets[i] + angle - 90);
                g.fill(cx + (int) (Math.cos(rad) * 45) - 2, cy + (int) (Math.sin(rad) * 45) - 2,
                        cx + (int) (Math.cos(rad) * 45) + 3, cy + (int) (Math.sin(rad) * 45) + 3, GREEN);
            }
            circle(g, cx, cy, 36, EDGE); circle(g, cx, cy, 32, masked(i) ? 0xFF285A50 : 0xFF364563);
            double rad = Math.toRadians(knobAngles[i] - 90);
            line(g, cx, cy, cx + (int) (Math.cos(rad) * 26), cy + (int) (Math.sin(rad) * 26), 2, masked(i) ? GREEN : INK);
            circle(g, cx, cy, 5, 0xFF0D1426);
            center(g, text(masked(i) ? "locked" : "knob_number", masked(i) ? "LOCKED" : "KNOB %s", i + 1), cx, 222,
                    masked(i) ? GREEN : MUTED);
            g.fill(cx - 34, 240, cx + 34, 245, 0xFF0D1426);
            double fraction = masked(i) ? 1 : dragging == i && localKnobSince >= 0 ? Math.clamp((now() - localKnobSince) / 600.0, 0, 1) : 0;
            g.fill(cx - 34, 240, cx - 34 + (int) (68 * fraction), 245, GREEN);
        }
    }

    private void drawSuccess(GuiGraphicsExtractor g, double mx, double my) {
        g.fill(19, 81, 401, 265, 0xED101C30);
        circle(g, 210, 134, 25, 0xFF27564B);
        line(g, 197, 134, 207, 144, 2, GREEN); line(g, 207, 144, 224, 122, 2, GREEN);
        center(g, text("success", "Task complete!"), 210, 171, GREEN);
        center(g, text("result", "Completed in %s s", String.format(Locale.ROOT, "%.1f", state.elapsed() / 1000.0)), 210, 189, INK);
        g.fill(142, 218, 278, 248, TaskLayout.REPLAY.contains(mx, my) ? 0xFF456B83 : 0xFF304863);
        g.outline(142, 218, 136, 30, EDGE);
        center(g, text("replay", "Try again"), 210, 229, INK);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        Transform t = transform(); double x = t.virtualX(event.x()), y = t.virtualY(event.y());
        if (TaskLayout.CLOSE.contains(x, y)) { onClose(); return true; }
        if (state == null || !state.started()) return true;
        if (state.complete()) {
            if (TaskLayout.REPLAY.contains(x, y)) send(TaskSession.REPLAY, -1, x, y);
            return true;
        }
        pointerX = x; pointerY = y;
        switch (type) {
            case TIMING -> { if (TaskLayout.near(x, y, 173, 243, 18)) hit(); }
            case WIRES -> {
                for (int i = 0; i < 4; i++) if (!masked(i) && TaskLayout.near(x, y, 68, TaskLayout.wireY(i), 16)) {
                    dragging = i; send(TaskSession.BEGIN, i, x, y); break;
                }
            }
            case SWIPE -> {
                if (!state.cardInserted() && TaskLayout.CARD_START.contains(x, y)) send(TaskSession.HIT, 0, x, y);
                else if (state.cardInserted() && x >= 65 && x <= 125 && y >= 134 && y <= 179) {
                    dragging = 0; cardX = 65; send(TaskSession.BEGIN, 0, x, y);
                }
            }
            case GARBAGE -> {
                for (int i = 5; i >= 0; i--) if (!masked(i) && TaskLayout.near(x, y, garbageX[i], garbageY[i], 22)) {
                    dragging = i; grabX = x - garbageX[i]; grabY = y - garbageY[i]; send(TaskSession.BEGIN, i, x, y); break;
                }
            }
            case KNOBS -> {
                for (int i = 0; i < 3; i++) if (!masked(i) && TaskLayout.near(x, y, TaskLayout.knobX(i), 162, 49)) {
                    dragging = i; updateDrag(x, y); send(TaskSession.BEGIN, i, x, y); break;
                }
            }
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || dragging < 0) return super.mouseDragged(event, deltaX, deltaY);
        Transform t = transform(); updateDrag(t.virtualX(event.x()), t.virtualY(event.y()));
        if ((type == TaskType.KNOBS || type == TaskType.SWIPE) && now() - lastMove >= 50) {
            send(TaskSession.MOVE, dragging, pointerX, pointerY); lastMove = now();
        }
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || dragging < 0) return super.mouseReleased(event);
        Transform t = transform(); updateDrag(t.virtualX(event.x()), t.virtualY(event.y()));
        double x = pointerX, y = pointerY;
        if (type == TaskType.GARBAGE) {
            x = garbageX[dragging]; y = garbageY[dragging];
            if (!TaskLayout.BIN.contains(x, y)) {
                x = garbageX[dragging] = Math.clamp(x, 40, 292); y = garbageY[dragging] = Math.clamp(y, 90, 248);
            }
        }
        send(TaskSession.END, dragging, x, y);
        if (type == TaskType.SWIPE) cardX = 65;
        dragging = -1; localKnobSince = -1;
        return true;
    }

    private void updateDrag(double x, double y) {
        pointerX = Math.clamp(x, 0, TaskLayout.WIDTH); pointerY = Math.clamp(y, 0, TaskLayout.HEIGHT);
        if (dragging < 0) return;
        if (type == TaskType.GARBAGE) {
            garbageX[dragging] = Math.clamp(pointerX - grabX, 24, 390);
            garbageY[dragging] = Math.clamp(pointerY - grabY, 90, 248);
        } else if (type == TaskType.SWIPE) cardX = Math.clamp(pointerX, 65, 318);
        else if (type == TaskType.KNOBS) {
            knobAngles[dragging] = TaskLayout.angle(pointerX, pointerY, TaskLayout.knobX(dragging), 162);
            if (TaskLayout.distance(knobAngles[dragging], layout.knobTargets[dragging]) <= 8) {
                if (localKnobSince < 0) localKnobSince = now();
            } else localKnobSince = -1;
        }
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE && type == TaskType.TIMING
                && state != null && state.started() && !state.complete()) { hit(); return true; }
        return super.keyPressed(event);
    }
    private void hit() { send(TaskSession.HIT, -1, 173, 243); }

    private void send(int action, int item, double x, double y) {
        if (!ClientPlayNetworking.canSend(TaskPackets.Action.TYPE)) return;
        ClientPlayNetworking.send(new TaskPackets.Action(open.sessionId(), sequence++, action, item,
                Math.clamp(x, 0, TaskLayout.WIDTH), Math.clamp(y, 0, TaskLayout.HEIGHT), elapsed()));
    }
    @Override public void onClose() { cancel(); minecraft.setScreenAndShow(null); }
    @Override public void removed() { cancel(); }
    private void cancel() { if (!closing) { closing = true; send(TaskSession.CANCEL, -1, 0, 0); } }
    public void closeFromServer() { closing = true; if (minecraft != null && minecraft.gui.screen() == this) minecraft.setScreenAndShow(null); }
    @Override public boolean isPauseScreen() { return false; }
    private boolean masked(int index) { return state != null && (state.mask() & (1 << index)) != 0; }
    private long elapsed() { return state == null || !state.started() ? 0 : state.complete() ? state.elapsed() : Math.max(0, now() - localStart); }
    private static long now() { return System.nanoTime() / 1_000_000; }
    private void clickSound() {
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
    private Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("task.goosetools.ui." + key, fallback, args);
    }
    private void center(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, x - font.width(text) / 2, y, color, false);
    }
    private static void circle(GuiGraphicsExtractor g, int x, int y, int radius, int color) {
        for (int row = -radius; row <= radius; row++) {
            int half = (int) Math.sqrt(radius * radius - row * row);
            g.fill(x - half, y + row, x + half + 1, y + row + 1, color);
        }
    }
    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int thickness, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t), y = (int) Math.round(y1 + (y2 - y1) * t);
            g.fill(x - thickness, y - thickness, x + thickness + 1, y + thickness + 1, color);
        }
    }
    private Transform transform() {
        double scale = Math.max(0.1, Math.min(1.5, Math.min((width - 24.0) / 420, (height - 24.0) / 320)));
        return new Transform((width - 420 * scale) / 2, (height - 320 * scale) / 2, scale);
    }
    private record Transform(double x, double y, double scale) {
        double virtualX(double px) { return (px - x) / scale; }
        double virtualY(double py) { return (py - y) / scale; }
    }
}
