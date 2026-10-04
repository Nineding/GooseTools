package com.goosethings.tools.client.hud;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Animated full-width report/bell banner rendered above the ordinary HUD. */
public final class MeetingAlertHud {
    private static ItemStack bell;
    private static Alert active;
    private static RemotePlayer callerModel;
    private static RemotePlayer victimModel;

    private MeetingAlertHud() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static void apply(GooseToolsPayloads.MeetingAlertS2C payload) {
        active = new Alert(payload, System.nanoTime());
        callerModel = null;
        victimModel = null;
    }

    /** True while the meeting transition owns non-spectator movement input. */
    public static boolean blocksMovement() {
        Alert alert = active;
        return alert != null
                && System.nanoTime() - alert.startedNanos() < MeetingAlertAnimation.DURATION_NANOS;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Alert alert = active;
        if (alert == null) {
            return;
        }
        long elapsed = System.nanoTime() - alert.startedNanos();
        if (elapsed >= MeetingAlertAnimation.DURATION_NANOS) {
            clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ensureModels(minecraft, alert.payload());

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        MeetingAlertAnimation.Frame frame = MeetingAlertAnimation.sample(elapsed, width);
        float alpha = frame.alpha();
        int offset = frame.xOffset();
        int bandHeight = Math.clamp(Math.round(height * 0.29F), 92, 154);
        int top = (height - bandHeight) / 2;
        int bottom = top + bandHeight;
        int kind = alert.payload().kind();

        graphics.fill(0, 0, width, height, argb(0x000000, alpha * 0.44F));
        drawBand(graphics, offset, width, top, bottom, kind, alpha, frame.pulse());
        drawContents(graphics, minecraft, offset, width, top, bottom, kind, alpha, alert.payload());
    }

    private static void drawBand(
            GuiGraphicsExtractor graphics,
            int offset,
            int width,
            int top,
            int bottom,
            int kind,
            float alpha,
            float pulse) {
        boolean report = kind == GooseToolsPayloads.MeetingAlertS2C.REPORT;
        boolean sacrifice = kind == GooseToolsPayloads.MeetingAlertS2C.SACRIFICE;
        int topColor = report ? 0x8E1017 : sacrifice ? 0x701746 : 0x8A4E08;
        int bottomColor = report ? 0xB51620 : sacrifice ? 0xB51D78 : 0xB6760C;
        int edgeColor = report ? 0xFF5962 : sacrifice ? 0xFF83CF : 0xFFD067;
        int left = offset - 12;
        int right = offset + width + 12;

        graphics.fillGradient(left, top, right, bottom,
                argb(topColor, alpha * 0.97F), argb(bottomColor, alpha * 0.97F));
        graphics.fill(left, top, right, top + 3, argb(edgeColor, alpha * 0.88F));
        graphics.fill(left, bottom - 3, right, bottom, argb(0x320408, alpha * 0.92F));
        graphics.fill(left, top + 6, right, top + 13, argb(0xFFFFFF, alpha * 0.08F));
        graphics.fill(left, bottom - 17, right, bottom - 9, argb(0x000000, alpha * 0.12F));

        int sweep = left + Math.round((width + 160) * pulse) - 80;
        graphics.fill(sweep, top + 3, sweep + 22, bottom - 3, argb(0xFFFFFF, alpha * 0.055F));
        graphics.fill(sweep + 22, top + 3, sweep + 28, bottom - 3, argb(0xFFFFFF, alpha * 0.025F));
    }

    private static void drawContents(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            int offset,
            int width,
            int top,
            int bottom,
            int kind,
            float alpha,
            GooseToolsPayloads.MeetingAlertS2C payload) {
        boolean report = kind == GooseToolsPayloads.MeetingAlertS2C.REPORT;
        boolean sacrifice = kind == GooseToolsPayloads.MeetingAlertS2C.SACRIFICE;
        int bandHeight = bottom - top;
        int callerCenter = offset + Math.round(width * (report ? 0.17F : 0.27F));
        int subjectCenter = offset + Math.round(width * (report ? 0.37F : sacrifice ? 0.31F : 0.43F));
        int textCenter = offset + Math.round(width * (report ? 0.72F : sacrifice ? 0.67F : 0.68F));

        int modelTop = top + 5;
        int modelBottom = bottom - 20;
        int callerScale = Math.clamp(Math.round(bandHeight * 0.34F), 31, 48);
        int victimScale = Math.clamp(Math.round(bandHeight * 0.27F), 26, 40);
        int callerHalfWidth = Math.max(34, callerScale);
        int victimHalfWidth = Math.max(82, Math.round(victimScale * 2.55F));

        if (!sacrifice && callerModel != null) {
            float callerLookX = report ? subjectCenter : callerCenter;
            float callerLookY = report
                    ? modelTop + (modelBottom - modelTop) * 0.68F
                    : (modelTop + modelBottom) * 0.5F;
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    callerCenter - callerHalfWidth,
                    modelTop,
                    callerCenter + callerHalfWidth,
                    modelBottom,
                    callerScale,
                    0.0F,
                    callerLookX,
                    callerLookY,
                    callerModel);
        }

        if (report && victimModel != null) {
            drawSleepingCorpse(graphics, victimModel,
                    subjectCenter - victimHalfWidth, modelTop + 4,
                    subjectCenter + victimHalfWidth, modelBottom - 2,
                    victimScale);
        } else if (!report) {
            drawBell(graphics, subjectCenter, (top + bottom) / 2, bandHeight,
                    sacrifice ? 1.24F : 1.0F);
        }

        int nameY = bottom - 15;
        if (!sacrifice) {
            MeetingNameTagHud.draw(graphics, minecraft, payload.caller(),
                    callerCenter, nameY, alpha);
        }
        if (report && payload.victim() != null) {
            MeetingNameTagHud.draw(graphics, minecraft, payload.victim(),
                    subjectCenter, nameY, alpha);
        }

        Component title = report
                ? Component.translatableWithFallback("hud.goosetools.meeting.report", "Body reported!")
                : sacrifice
                ? Component.translatableWithFallback("title.sacrificebell.meeting", "Someone Must Go")
                : Component.translatableWithFallback("hud.goosetools.meeting.bell", "A player rang the bell!");
        Component subtitle = sacrifice
                ? Component.translatableWithFallback(
                "subtitle.sacrificebell.meeting", "Entering the meeting phase...")
                : Component.translatableWithFallback(
                "hud.goosetools.meeting.incoming", "Entering the meeting phase...");
        float titleScale = width < 520 ? 1.25F : width < 780 ? 1.55F : 1.9F;
        drawCentered(graphics, minecraft.font, title, textCenter,
                (top + bottom) / 2 - Math.round(10 * titleScale),
                titleScale, argb(0xFFFFFF, alpha));
        drawCentered(graphics, minecraft.font, subtitle, textCenter,
                (top + bottom) / 2 + Math.round(8 * titleScale),
                width < 520 ? 0.82F : 1.0F,
                argb(report ? 0xFFD5D8 : sacrifice ? 0xFFD1EF : 0xFFF0C2, alpha));
    }

    private static void drawBell(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int bandHeight,
            float sizeMultiplier) {
        if (bell == null) {
            // Item component holders are not bound while Fabric client entrypoints run.
            // Build the stack only after a world exists and the alert is actually rendered.
            bell = new ItemStack(Items.BELL);
        }
        float scale = Math.clamp(bandHeight / 42.0F, 2.35F, 3.35F) * sizeMultiplier;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - 8.0F * scale, centerY - 8.0F * scale);
        graphics.pose().scale(scale, scale);
        graphics.item(bell, 0, 0, 0xB311);
        graphics.pose().popMatrix();
    }

    private static void drawCentered(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            float scale,
            int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popMatrix();
    }

    private static void drawSleepingCorpse(
            GuiGraphicsExtractor graphics,
            RemotePlayer player,
            int left,
            int top,
            int right,
            int bottom,
            int scale) {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState state = renderer.createRenderState(player, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F;
            // Sleeping rotates the body's local up axis into the screen plane. A
            // 60-degree relative head yaw therefore turns the face toward the viewer.
            living.yRot = 60.0F;
            living.xRot = 0.0F;
            living.bedOrientation = null;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        Vector3f offset = new Vector3f(0.0F, state.boundingBoxHeight * 0.5F, 0.0F);
        Quaternionf inventoryOrientation = new Quaternionf().rotateZ(Mth.PI);
        graphics.entity(state, scale, offset, inventoryOrientation, new Quaternionf(),
                left, top, right, bottom);
    }

    private static void ensureModels(Minecraft minecraft, GooseToolsPayloads.MeetingAlertS2C payload) {
        if (callerModel == null) {
            callerModel = createModel(minecraft, payload.caller(), false, 0x6A110001);
        }
        if (payload.victim() != null && victimModel == null) {
            victimModel = createModel(minecraft, payload.victim(), true, 0x6A110002);
        }
    }

    private static RemotePlayer createModel(
            Minecraft minecraft,
            GooseToolsPayloads.MeetingAppearance appearance,
            boolean corpse,
            int entityId) {
        RemotePlayer player = new RemotePlayer(
                minecraft.level,
                new GameProfile(appearance.playerId(), appearance.playerName()));
        player.setId(entityId);
        player.setPose(corpse ? Pose.SLEEPING : Pose.STANDING);
        player.setYRot(0.0F);
        player.yRotO = 0.0F;
        player.setXRot(0.0F);
        player.xRotO = 0.0F;
        player.yHeadRot = 0.0F;
        player.yHeadRotO = 0.0F;
        player.yBodyRot = 0.0F;
        player.yBodyRotO = 0.0F;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND
                    ? ItemStack.EMPTY
                    : appearance.equipment().get(slot.ordinal()).copy();
            player.setItemSlot(slot, stack);
        }
        player.refreshDimensions();
        return player;
    }

    private static int argb(int rgb, float alpha) {
        int value = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        return value << 24 | rgb & 0x00FFFFFF;
    }

    private static void clear() {
        active = null;
        callerModel = null;
        victimModel = null;
    }

    private record Alert(GooseToolsPayloads.MeetingAlertS2C payload, long startedNanos) {
    }
}
