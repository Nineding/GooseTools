package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.intro.RoleIntroScene;
import com.goosethings.tools.result.MatchResultPayload;
import com.goosethings.tools.result.MatchResultPolicy;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Frozen end-of-round roster: defeated participants above, winners below. */
public final class MatchResultScreen extends Screen implements ProtectedInputScreen {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("goosetools", "match_result_stage");
    private final MatchResultPayload payload;
    private final Map<UUID, RemotePlayer> models = new HashMap<>();
    private final long started = System.nanoTime();
    private boolean textureReady;
    public MatchResultScreen(MatchResultPayload payload) {
        super(Component.translatableWithFallback("result.goosetools.title", "Match results"));
        this.payload = payload;
    }
    public MatchResultPayload payload() { return payload; }
    public static void apply(MatchResultPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!payload.active()) {
            if (mc.gui.screen() instanceof MatchResultScreen) mc.gui.setScreen(null);
        } else if (mc.level != null && mc.player != null) mc.gui.setScreen(new MatchResultScreen(payload));
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {}
    @Override public void tick() {
        if ((minecraft.level == null || minecraft.player == null || System.nanoTime()-started > 30_000_000_000L)
                && minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
    }
    @Override public void removed() {
        models.clear();
        if (textureReady) { minecraft.getTextureManager().release(BACKGROUND); textureReady = false; }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float partial) {
        ensureBackground();
        g.blit(BACKGROUND, 0, 0, width, height, 0, 1, 0, 1);
        MeetingAlertTrueSkinCache.capture(minecraft);
        var losers = payload.roster().stream().filter(p -> !p.winner()).toList();
        var winners = payload.roster().stream().filter(MatchResultPayload.Player::winner).toList();
        drawGroup(g, losers, height * .025F, height * .215F, height * .055F, 21);
        int theme = MatchResultPolicy.theme(payload.outcome());
        String fallback = theme == 0 ? "Goose Victory" : theme == 1 ? "Duck Victory"
                : theme == 3 ? "Lover Victory" : payload.outcome().substring(0, 1).toUpperCase()+payload.outcome().substring(1)+" Victory";
        Component title = Component.translatableWithFallback(MatchResultPolicy.victoryKey(payload.outcome()), fallback);
        int color = theme == 0 ? 0xFF5EFF36 : theme == 1 ? 0xFFFF9898 : theme == 3 ? 0xFFFFB6D7 : 0xFF000000 | MatchResultPolicy.neutralColor(payload.outcome());
        centered(g, title, width*.5F, height*.305F, Math.min(4.8F*ui(), width*.9F/Math.max(1, font.width(title))), color);
        Component reason = Component.translatableWithFallback(MatchResultPolicy.reasonKey(payload.outcome()), "");
        drawReason(g, reason);
        drawGroup(g, winners, height*.53F, height*.96F, height*.175F, 10);
        float cover = com.goosethings.tools.intro.PresentationTransition.resultCoverOpacity(System.nanoTime()-started);
        if (cover > 0) {
            g.nextStratum();
            g.fill(0, 0, width, height, Math.round(cover*255) << 24);
        }
    }
    private void drawReason(GuiGraphicsExtractor g, Component reason) {
        float scale = 1.6F*ui();
        var lines = font.split(reason, Math.max(1, (int)(width*.96F/scale)));
        for (int attempt=0; lines.size() > 2 && attempt<12; attempt++) {
            scale *= .9F;
            lines = font.split(reason, Math.max(1, (int)(width*.96F/scale)));
        }
        for (int i=0; i<lines.size(); i++) {
            var line = lines.get(i);
            g.pose().pushMatrix();
            g.pose().translate(width*.5F, height*.425F+i*11*scale);
            g.pose().scale(scale, scale);
            g.text(font, line, -font.width(line)/2, 0, MatchResultPolicy.theme(payload.outcome()) == 2
                    ? 0xFF000000 | MatchResultPolicy.neutralColor(payload.outcome()) : 0xFFE8E8E8, true);
            g.pose().popMatrix();
        }
    }
    private void drawGroup(GuiGraphicsExtractor g, List<MatchResultPayload.Player> group, float top, float bottom, float maxScale, int maxColumns) {
        if (group.isEmpty() || minecraft.level == null) return;
        int columns = Math.min(maxColumns, group.size()), rows = (group.size()+columns-1)/columns;
        float rowHeight = (bottom-top)/rows;
        float labelScale = Math.max(.5F, Math.min(.95F*ui(), rowHeight/45F));
        int widest = group.stream().mapToInt(p -> font.width(p.name()) + font.width(
                com.goosethings.tools.client.nametag.SerialBadgeStyle.glyphComponent(p.serial(), p.rgb()))).max().orElse(1);
        labelScale = Math.min(labelScale, (width*.9F/columns-6)/Math.max(1, widest+1));
        float scale = Math.max(5F, Math.min(maxScale, (rowHeight-24*labelScale)/2.05F));
        if (maxColumns == 21) scale = Math.min(scale, width*.9F/columns*.4F);
        for (int row = 0; row < rows; row++) {
            int first = row*columns, count = Math.min(columns, group.size()-first);
            float step = Math.min(width*.9F/columns, Math.max(scale*1.65F, (widest+6)*labelScale+4));
            float left = (width-step*count)*.5F;
            float floor = top+rowHeight*(row+1);
            for (int column = 0; column < count; column++) {
                var appearance = group.get(first+column);
                float center = left+step*(column+.5F), modelTop = floor-scale*2.05F;
                float nameY = modelTop-23*labelScale;
                MeetingNameTagHud.drawFrozen(g, minecraft, appearance.name(), appearance.rgb(), appearance.serial(),
                        appearance.lover(), center, nameY, labelScale, Math.max(12, (int)((step-4)/labelScale)));
                var role = RoleIntroCatalog.byTag(appearance.role());
                Component roleText = Component.translatableWithFallback(role.key(), role.fallback());
                if (appearance.lover()) roleText = Component.empty().append(roleText)
                        .append(Component.translatableWithFallback(role.faction()==1 ? "role.lover.duck_suffix" : "role.lover.goose_suffix",
                                role.faction()==1 ? " [Lover Duck]" : " [Lover Goose]"));
                centered(g, roleText, center, modelTop-10*labelScale,
                        Math.min(labelScale, (step-4)/Math.max(1, font.width(roleText))), 0xFF000000|role.color());
                RemotePlayer model = models.computeIfAbsent(appearance.id(), id -> model(appearance, models.size()));
                var camera = minecraft.getCameraEntity();
                if (camera != null) model.setPos(camera.position());
                var state = minecraft.getEntityRenderDispatcher().getRenderer(model).createRenderState(model, 1);
                boolean speaking = com.goosethings.tools.client.voice.GgdSpeakingIcon.speakingInResults(appearance.id());
                state.nameTag = null;
                state.outlineColor = speaking ? 0xFF66FF99 : 0;
                if (speaking) {
                    // GUI entity textures do not run the world outline post-process.
                    // A layered frame remains visible for translucent deceased models too.
                    int bx = Math.round(center - scale * .58F), by = Math.round(modelTop);
                    int bw = Math.max(8, Math.round(scale * 1.16F)), bh = Math.round(floor - modelTop);
                    g.outline(bx - 2, by - 2, bw + 4, bh + 4, 0x3066FF99);
                    g.outline(bx - 1, by - 1, bw + 2, bh + 2, 0x8066FF99);
                    g.outline(bx, by, bw, bh, 0xFF99FFBB);
                }
                state.shadowPieces.clear();
                if (state instanceof LivingEntityRenderState living) {
                    living.bodyRot = 180; living.yRot = 0; living.xRot = 0;
                    living.walkAnimationSpeed = 0;
                    if (appearance.dead()) living.isInvisibleToPlayer = true;
                }
                if (appearance.dead()) state.isInvisible = true;
                int pixelScale = Math.max(5, Math.round(scale));
                g.entity(state, pixelScale, new Vector3f(0, (floor-modelTop)*.5F/pixelScale, 0),
                        new Quaternionf().rotateZ((float)Math.PI), new Quaternionf(),
                        Math.round(center-step*.48F), Math.round(modelTop), Math.round(center+step*.48F), Math.round(floor));
            }
        }
    }
    private RemotePlayer model(MatchResultPayload.Player appearance, int index) {
        var model = new MeetingAlertRemotePlayer(minecraft.level,
                new GameProfile(RoleIntroScene.modelId(appearance.id()), appearance.name()), () -> {
                    var skin = MeetingAlertTrueSkinCache.get(appearance.id());
                    if (skin != null) return skin;
                    var connection = minecraft.getConnection();
                    var info = connection == null ? null : connection.getPlayerInfo(appearance.id());
                    return info == null ? DefaultPlayerSkin.get(appearance.id()) : info.getSkin();
                });
        model.setId(0x6A130000+index);
        model.showAllSkinLayers();
        model.setInvisible(appearance.dead());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            boolean visible = slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
                    && (!appearance.dead() || slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST);
            model.setItemSlot(slot, visible ? appearance.equipment().get(slot.ordinal()).copy() : ItemStack.EMPTY);
        }
        model.refreshDimensions();
        return model;
    }
    private float ui() { return Math.max(.55F, Math.min(width/640F, height/360F)); }
    private void centered(GuiGraphicsExtractor g, Component text, float x, float y, float scale, int color) {
        g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(scale,scale);
        g.text(font, text, -font.width(text)/2, 0, color, true); g.pose().popMatrix();
    }
    private void ensureBackground() {
        if (textureReady) return;
        int tint = switch(MatchResultPolicy.theme(payload.outcome())) {
            case 1 -> 0xC10B18; case 2 -> MatchResultPolicy.neutralColor(payload.outcome()); case 3 -> 0xE85C99; default -> 0xD9DEE6;
        };
        NativeImage image = new NativeImage(512,288,false);
        for (int y=0;y<288;y++) for(int x=0;x<512;x++) {
            double edge = Math.clamp(1-Math.abs(x/511.0-.5)*1.8,0,1);
            double light = (.12+.76*edge*edge)*(.63+.37*y/287.0);
            int r=(int)((tint>>16&255)*light), green=(int)((tint>>8&255)*light), b=(int)((tint&255)*light);
            image.setPixel(x,y,0xFF000000|r<<16|green<<8|b);
        }
        minecraft.getTextureManager().register(BACKGROUND,new DynamicTexture(() -> "GooseTools match results",image));
        textureReady=true;
    }
}
