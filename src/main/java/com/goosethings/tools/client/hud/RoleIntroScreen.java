package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.intro.RoleIntroPayload;
import com.goosethings.tools.intro.RoleIntroPolicy;
import com.goosethings.tools.intro.RoleIntroScene;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DLC-only, non-interactive opening stage. The server has already filtered its roster. */
public final class RoleIntroScreen extends Screen implements ProtectedInputScreen {
    private static final Identifier STAGE = Identifier.fromNamespaceAndPath("goosetools", "role_intro_stage");
    private final RoleIntroPayload payload;
    private final long started = System.nanoTime();
    private final Map<UUID, RemotePlayer> models = new HashMap<>();
    private boolean textureReady;

    public RoleIntroScreen(RoleIntroPayload payload) {
        super(Component.translatableWithFallback("intro.goosetools.title", "Your role"));
        this.payload = payload;
    }
    public RoleIntroPayload payload() { return payload; }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            // The GUI owns the disconnect screen transition; only release our render resources here.
            if (client.gui.screen() instanceof RoleIntroScreen intro) intro.removed();
        }));
    }
    public static void apply(RoleIntroPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!payload.active()) {
            if (mc.gui.screen() instanceof RoleIntroScreen) mc.gui.setScreen(null);
            return;
        }
        if (mc.level != null && mc.player != null) mc.gui.setScreen(new RoleIntroScreen(payload));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {} // Opening owns input until the authoritative transition completes.
    @Override public void tick() {
        if (minecraft.level == null || minecraft.player == null
                || System.nanoTime() - started >= RoleIntroScene.DURATION_NANOS) {
            if (minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
        }
    }
    @Override public void removed() {
        models.clear();
        if (textureReady) { minecraft.getTextureManager().release(STAGE); textureReady = false; }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.nanoTime() - started;
        float brightness = RoleIntroScene.brightness(elapsed);
        ensureStage();
        g.blit(STAGE, 0, 0, width, height, 0, 1, 0, 1);
        if (minecraft.level != null) {
            MeetingAlertTrueSkinCache.capture(minecraft);
            drawText(g);
            List<RoleIntroPayload.Appearance> main = payload.roster().stream().filter(p -> !p.small()).toList();
            List<RoleIntroPayload.Appearance> small = payload.roster().stream().filter(RoleIntroPayload.Appearance::small).toList();
            double seconds = elapsed / 1_000_000_000.0;
            if (small.isEmpty()) {
                drawRow(g, main, width * .09F, width * .91F, height * .84F, height * .17F, seconds);
            } else {
                drawRow(g, main, width * .18F, width * .61F, height * .84F, height * .17F, seconds);
                // Small teammates sit beside the pair, without promoting them to partner scale.
                drawRow(g, small, width * .65F, width * .94F, height * .84F, height * .095F, seconds);
                centered(g, Component.translatableWithFallback("intro.goosetools.teammates", "Duck teammates"),
                        width * .795F, height * .56F, .8F * uiScale(), 0xFFFFC7C7);
            }
        }
        // An opaque stage stays in place throughout both fades; the game never flashes through.
        if (brightness < 1) g.fill(0, 0, width, height, ((int) ((1 - brightness) * 255)) << 24);
    }

    private void drawText(GuiGraphicsExtractor g) {
        float ui = uiScale();
        String mode = switch (payload.mode()) { case 1 -> "draft"; case 2 -> "killer"; default -> "classic"; };
        String fallback = switch (payload.mode()) {
            case 1 -> "Game mode: Role Draft"; case 2 -> "Game mode: Killer"; default -> "Game mode: Classic";
        };
        centered(g, Component.translatableWithFallback("intro.goosetools.mode." + mode, fallback),
                width * .5F, height * .16F, 1.08F * ui, 0xFFEAEAEA);
        RoleIntroCatalog.Role role = RoleIntroCatalog.byTag(payload.role());
        var title = Component.empty().append(Component.translatableWithFallback(role.key(), role.fallback())
                .withStyle(style -> style.withColor(role.color()).withItalic(false)));
        if (payload.theme() == RoleIntroPolicy.LOVER) {
            String suffix = role.faction() == 1 ? "role.lover.duck_suffix" : "role.lover.goose_suffix";
            title.append(Component.translatableWithFallback(suffix, role.faction() == 1 ? " [Lover Duck]" : " [Lover Goose]")
                    .withStyle(style -> style.withColor(0xFF74B8).withItalic(false)));
        } else {
            String key = role.faction() == 1 ? "role.evil" : role.faction() == 0 ? "role.good.goose" : "role.neutral";
            String faction = role.faction() == 1 ? "Duck" : role.faction() == 0 ? "Goose" : "Bird";
            title.append(Component.literal(" [").withColor(role.color()))
                    .append(Component.translatableWithFallback(key, faction).withColor(role.color()))
                    .append(Component.literal("]").withColor(role.color()));
        }
        float titleScale = Math.min(2.65F * ui, width * .88F / Math.max(1, font.width(title)));
        centered(g, title, width * .5F, height * .235F, titleScale, 0xFFFFFFFF);
        Component description = Component.translatableWithFallback(role.descriptionKey(), role.descriptionFallback());
        float descriptionScale = .96F * ui;
        var lines = font.split(description, Math.max(30, (int) (width * .82F / descriptionScale)));
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            g.pose().pushMatrix();
            g.pose().translate(width * .5F, height * .35F + i * 12 * descriptionScale);
            g.pose().scale(descriptionScale, descriptionScale);
            g.text(font, line, -font.width(line) / 2, 0, 0xFFEFEFEF, true);
            g.pose().popMatrix();
        }
    }

    private void drawRow(GuiGraphicsExtractor g, List<RoleIntroPayload.Appearance> row,
                         float left, float right, float floor, float maxScale, double seconds) {
        if (row.isEmpty()) return;
        float step = RoleIntroScene.rowStep(right - left, row.size(), maxScale);
        left = (left + right - step * row.size()) * .5F;
        int scale = Math.max(5, Math.round(Math.min(maxScale, step * .86F)));
        float baseNameScale = Math.min(.9F * uiScale(), step / 43F);
        for (int i = 0; i < row.size(); i++) {
            RoleIntroPayload.Appearance appearance = row.get(i);
            RemotePlayer model = models.computeIfAbsent(appearance.id(), id -> model(appearance, models.size()));
            // Skin Layers uses the entity's world distance for LOD even in GUI renders.
            // Keep this isolated preview at the camera so its voxel layers remain enabled.
            var camera = minecraft.getCameraEntity();
            if (camera != null) model.setPos(camera.position());
            float center = left + step * (i + .5F);
            var renderer = minecraft.getEntityRenderDispatcher().getRenderer(model);
            var state = renderer.createRenderState(model, 1);
            state.shadowPieces.clear(); state.outlineColor = 0; state.nameTag = null;
            state.ageInTicks = (float) (seconds * 20 + i * 7);
            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = 180F + (float) Math.sin(seconds * 1.5 + i) * 3;
                living.yRot = (float) Math.sin(seconds + i) * 4;
                living.xRot = 0;
                living.walkAnimationPos = (float) (seconds * 1.5 + i);
                living.walkAnimationSpeed = 0;
            }
            int top = Math.round(floor - scale * 2.05F), bottom = Math.round(floor);
            // GUI entity renders start at the rectangle center; anchor the actual feet
            // to its bottom rather than centering the shorter collision bounding box.
            float feetOffset = (bottom - top) * .5F / scale;
            drawContactShadow(g, center, bottom, scale);
            g.entity(state, scale, new Vector3f(0, feetOffset, 0),
                    new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(),
                    Math.round(center - step * .48F), top, Math.round(center + step * .48F), bottom);
            float nameScale = Math.max(.45F, baseNameScale);
            // Stagger dense roster labels above the models as in the reference.
            float nameY = top - 12 * nameScale - (row.size() > 9 && (i & 1) == 0 ? 11 * nameScale : 0);
            MeetingNameTagHud.drawIntro(g, minecraft, appearance.id(), appearance.name(),
                    center, nameY, nameScale, Math.max(12, (int) ((step - 3) / nameScale)));
        }
    }

    private RemotePlayer model(RoleIntroPayload.Appearance appearance, int index) {
        UUID isolated = RoleIntroScene.modelId(appearance.id());
        MeetingAlertRemotePlayer model = new MeetingAlertRemotePlayer(minecraft.level,
                new GameProfile(isolated, appearance.name()), () -> skin(appearance.id()));
        model.showAllSkinLayers();
        model.setId(0x6A120000 + index);
        model.refreshDimensions();
        return model;
    }
    private PlayerSkin skin(UUID id) {
        PlayerSkin cached = MeetingAlertTrueSkinCache.get(id);
        if (cached != null) return cached;
        var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(id);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
    }

    private float uiScale() { return Math.max(.55F, Math.min(width / 640F, height / 360F)); }
    private void drawContactShadow(GuiGraphicsExtractor g, float center, float floor, int scale) {
        float radius = scale * .46F;
        int depth = Math.max(2, Math.round(scale * .06F));
        for (int y = -depth; y <= depth; y++) {
            float extent = radius * (float) Math.sqrt(Math.max(0, 1 - (float) y * y / (depth * depth)));
            int alpha = Math.round(46F * (1 - Math.abs(y) / (depth + 1F)));
            g.fill(Math.round(center - extent), Math.round(floor) + y,
                    Math.round(center + extent), Math.round(floor) + y + 1, alpha << 24);
        }
    }
    private void centered(GuiGraphicsExtractor g, Component text, float x, float y, float scale, int color) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale);
        g.text(font, text, -font.width(text) / 2, 0, color, true); g.pose().popMatrix();
    }
    private void ensureStage() {
        if (textureReady) return;
        NativeImage image = new NativeImage(512, 288, false);
        for (int y = 0; y < 288; y++) for (int x = 0; x < 512; x++) {
            image.setPixel(x, y, RoleIntroScene.stagePixel(payload.theme(), x / 511.0, y / 287.0));
        }
        minecraft.getTextureManager().register(STAGE, new DynamicTexture(() -> "GooseTools role stage", image));
        textureReady = true;
    }
}
