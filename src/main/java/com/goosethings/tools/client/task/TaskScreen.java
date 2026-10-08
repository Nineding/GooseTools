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
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** A dedicated, non-pausing task panel with one virtual coordinate space for drawing and input. */
public final class TaskScreen extends Screen {
    private static final int INK = 0xFFF0F0EA, MUTED = 0xFFB3BCC1, GREEN = 0xFF7BCF77,
            RED = 0xFFE77969, EDGE = 0xFF7C8A91;
    private static final Identifier DIAL = texture("dial"), READER = texture("reader"),
            BIN = texture("bin"), KNOB = texture("knob"), CARD = texture("key_card");
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
    private ItemStack[] trashItems;

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
        graphics.fill(0, 0, width, height, 0xAE101416);
        Transform transform = transform();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) transform.x, (float) transform.y);
        graphics.pose().scale((float) transform.scale, (float) transform.scale);
        double mx = transform.virtualX(mouseX), my = transform.virtualY(mouseY);
        graphics.fill(3, 5, 423, 325, 0x66000000);
        bevel(graphics, 0, 0, 420, 320, 0xFF3C474D);
        bevel(graphics, 8, 8, 404, 40, 0xFF293136);
        graphics.fill(18, 46, 402, 48, 0xFFBD9563);
        for (int x : new int[]{4, 413}) for (int y : new int[]{4, 312}) screw(graphics, x, y);
        graphics.text(font, getTitle(), 18, 14, INK, false);
        graphics.text(font, text("trial", "Task trial"), 18, 29, MUTED, false);
        bevel(graphics, 382, 12, 25, 24, TaskLayout.CLOSE.contains(mx, my) ? 0xFFB86459 : 0xFF79473F);
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
        bevel(graphics, 18, 80, 384, 185, 0xFF1D252A);
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
        bevel(graphics, 18, 295, 317, 12, 0xFF151C20);
        graphics.fill(19, 297, 19 + (int) (315.0 * progress / type.total), 305, GREEN);
        graphics.text(font, text("progress", "%s / %s", progress, type.total), 348, 296, INK, false);
        if (state != null && state.complete()) drawSuccess(graphics, mx, my);
        graphics.pose().popMatrix();
    }

    private void drawTiming(GuiGraphicsExtractor g, double mx, double my) {
        sprite(g, DIAL, 95, 86, 156, 156);
        for (int degrees = 0; degrees < 360; degrees += 2) {
            if (!TaskLayout.inGreen(degrees)) continue;
            double rad = Math.toRadians(degrees - 90);
            int x = 173 + (int) (Math.cos(rad) * 61), y = 164 + (int) (Math.sin(rad) * 61);
            g.fill(x - 3, y - 3, x + 4, y + 4, GREEN);
        }
        double angle = Math.toRadians(layout.timingAngle(elapsed()) - 90);
        int tipX = 173 + (int) (Math.cos(angle) * 57), tipY = 164 + (int) (Math.sin(angle) * 57);
        line(g, 175, 166, tipX + 2, tipY + 2, 2, 0xFF101619);
        line(g, 173, 164, tipX, tipY, 1, INK);
        circle(g, 173, 164, 6, 0xFF101619); circle(g, 172, 163, 4, 0xFFBD9563);
        boolean hover = TaskLayout.near(mx, my, 173, 243, 17);
        sprite(g, KNOB, 155, 225, 36, 36);
        circle(g, 173, 243, 12, 0xFF542E35);
        circle(g, 172, 242, 10, hover ? 0xFFF4929C : 0xFFBB6576);
        bevel(g, 309, 91, 41, 161, 0xFF59666C); g.fill(313, 95, 346, 248, 0xFF11191C);
        int progress = state == null ? 0 : state.progress();
        g.fill(315, 246 - 149 * progress / type.total, 344, 246, GREEN);
        for (int i = 1; i < type.total; i++) g.fill(313, 246 - 149 * i / type.total, 346, 247 - 149 * i / type.total, 0xFF253148);
    }

    private void drawWires(GuiGraphicsExtractor g) {
        bevel(g, 41, 91, 48, 159, 0xFF59666C); bevel(g, 331, 91, 48, 159, 0xFF59666C);
        screw(g, 44, 94); screw(g, 82, 241); screw(g, 334, 94); screw(g, 372, 241);
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
        bevel(g, x - 14, y - 13, 28, 26, 0xFF242E33);
        bevel(g, x - 11, y - 10, 22, 20, WIRE_COLORS[index]);
        center(g, Component.literal(Integer.toString(index + 1)), x, y - 4, 0xFF162027);
        if (done) circle(g, x + 15, y - 10, 3, GREEN);
    }

    private void wire(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        line(g, x1, y1 + 2, x2, y2 + 2, 5, 0xFF11191C);
        line(g, x1, y1, x2, y2, 3, shade(color, 0.6));
        line(g, x1, y1 - 1, x2, y2 - 1, 1, color);
    }

    private void drawSwipe(GuiGraphicsExtractor g) {
        sprite(g, READER, 50, 97, 320, 100);
        center(g, text("reader", "ACCESS READER"), 210, 112, INK);
        bevel(g, 314, 113, 8, 8, state != null && state.complete() ? GREEN : 0xFFCDB570);
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
        // Keep the existing key-card sprite square so its pixels are not distorted.
        sprite(g, CARD, x + (w - h) / 2, y, h, h);
    }

    private void drawGarbage(GuiGraphicsExtractor g) {
        sprite(g, BIN, 303, 94, 88, 135);
        if (dragging >= 0 && TaskLayout.BIN.contains(garbageX[dragging], garbageY[dragging]))
            g.outline(314, 100, 66, 126, GREEN);
        center(g, text("drop_here", "DROP HERE"), 347, 237, MUTED);
        for (int i = 0; i < 6; i++) if (!masked(i) && i != dragging) drawTrash(g, i, (int) garbageX[i], (int) garbageY[i]);
        if (dragging >= 0) drawTrash(g, dragging, (int) garbageX[dragging], (int) garbageY[dragging]);
    }

    private void drawTrash(GuiGraphicsExtractor g, int i, int x, int y) {
        if (trashItems == null) trashItems = new ItemStack[]{new ItemStack(Items.GLASS_BOTTLE),
                new ItemStack(Items.PAPER), new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.BONE),
                new ItemStack(Items.POISONOUS_POTATO), new ItemStack(Items.STICK)};
        g.fill(x - 12, y + 12, x + 13, y + 16, 0x55000000);
        g.pose().pushMatrix();
        g.pose().translate(x - 16.0F, y - 16.0F);
        g.pose().scale(2.0F, 2.0F);
        g.item(trashItems[i], 0, 0, i);
        g.pose().popMatrix();
    }

    private void drawKnobs(GuiGraphicsExtractor g) {
        for (int i = 0; i < 3; i++) {
            int cx = TaskLayout.knobX(i), cy = 162;
            sprite(g, KNOB, cx - 48, cy - 48, 96, 96);
            for (int angle = -8; angle <= 8; angle += 2) {
                double rad = Math.toRadians(layout.knobTargets[i] + angle - 90);
                g.fill(cx + (int) (Math.cos(rad) * 45) - 2, cy + (int) (Math.sin(rad) * 45) - 2,
                        cx + (int) (Math.cos(rad) * 45) + 3, cy + (int) (Math.sin(rad) * 45) + 3, GREEN);
            }
            double rad = Math.toRadians(knobAngles[i] - 90);
            int tipX = cx + (int) (Math.cos(rad) * 28), tipY = cy + (int) (Math.sin(rad) * 28);
            line(g, cx + 1, cy + 1, tipX + 1, tipY + 1, 2, 0xFF11191C);
            line(g, cx, cy, tipX, tipY, 1, masked(i) ? GREEN : INK);
            circle(g, cx, cy, 5, 0xFF141B1F); circle(g, cx - 1, cy - 1, 3, masked(i) ? GREEN : 0xFFBD9563);
            center(g, text(masked(i) ? "locked" : "knob_number", masked(i) ? "LOCKED" : "KNOB %s", i + 1), cx, 222,
                    masked(i) ? GREEN : MUTED);
            g.fill(cx - 34, 240, cx + 34, 245, 0xFF0D1426);
            double fraction = masked(i) ? 1 : dragging == i && localKnobSince >= 0 ? Math.clamp((now() - localKnobSince) / 600.0, 0, 1) : 0;
            g.fill(cx - 34, 240, cx - 34 + (int) (68 * fraction), 245, GREEN);
        }
    }

    private void drawSuccess(GuiGraphicsExtractor g, double mx, double my) {
        g.fill(19, 81, 401, 265, 0xED182125);
        circle(g, 210, 134, 25, 0xFF27564B);
        line(g, 197, 134, 207, 144, 2, GREEN); line(g, 207, 144, 224, 122, 2, GREEN);
        center(g, text("success", "Task complete!"), 210, 171, GREEN);
        center(g, text("result", "Completed in %s s", String.format(Locale.ROOT, "%.1f", state.elapsed() / 1000.0)), 210, 189, INK);
        bevel(g, 142, 218, 136, 30, TaskLayout.REPLAY.contains(mx, my) ? 0xFF627B69 : 0xFF475B4D);
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
    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("goosetools", "textures/gui/tasks/" + name + ".png");
    }
    private static void sprite(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h) {
        g.blit(texture, x, y, x + w, y + h, 0.0F, 1.0F, 0.0F, 1.0F);
    }
    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0xFF11181B);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, shade(color, 1.4));
        g.fill(x + 1, y + 3, x + 3, y + h - 2, shade(color, 1.2));
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, shade(color, 0.55));
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 3, shade(color, 0.7));
    }
    private static int shade(int color, double factor) {
        return 0xFF000000 | (Math.min(255, (int) (((color >> 16) & 255) * factor)) << 16)
                | (Math.min(255, (int) (((color >> 8) & 255) * factor)) << 8)
                | Math.min(255, (int) ((color & 255) * factor));
    }
    private static void screw(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 4, y + 4, 0xFF161D20);
        g.fill(x, y, x + 3, y + 3, 0xFFA2ABB0);
        g.fill(x, y + 1, x + 3, y + 2, 0xFF515B62);
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
