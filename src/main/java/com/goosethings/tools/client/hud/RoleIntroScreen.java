package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.input.ProtectedInputScreen;
import com.goosethings.tools.intro.RoleIntroCatalog;
import com.goosethings.tools.intro.RoleIntroMap;
import com.goosethings.tools.intro.RoleIntroAct;
import net.minecraft.client.renderer.RenderPipelines;
import com.goosethings.tools.intro.RoleIntroArtwork;
import com.goosethings.tools.GooseTools;
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
    private static final Identifier ARTWORK = Identifier.fromNamespaceAndPath("goosetools", "role_intro_artwork");
    private int artworkWidth, artworkHeight;
    private int artworkScreenWidth = -1, artworkScreenHeight = -1;
    private RoleIntroArtwork.Bounds artworkBounds;
    private final RoleIntroPayload payload;
    private final boolean smallTeammates;
    private final long started = System.nanoTime();
    private final Map<UUID, RemotePlayer> models = new HashMap<>();
    private boolean textureReady;
    private static final int LIGHT_FRAMES = 25;
    private final java.util.Set<Identifier> lightTextures = new java.util.HashSet<>();
    private float textOpacity = 1;
    private float rosterOpacity;
    private static final Identifier DAGGER = Identifier.fromNamespaceAndPath("goosetools", "textures/gui/role_intro/bloody_dagger.png");
    private static final Identifier HEART = Identifier.fromNamespaceAndPath("goosetools", "textures/gui/role_intro/heart.png");
    // Match Minecraft's accessibility screen-effect setting; do not force spatial motion.
    private boolean reducedMotion() { return minecraft.options.screenEffectScale().get() == 0; }
    private static Identifier lightTexture(int frame) {
        return frame == 0 ? STAGE : Identifier.fromNamespaceAndPath("goosetools", "role_intro_light_"+frame);
    }

    public RoleIntroScreen(RoleIntroPayload payload) {
        super(Component.translatableWithFallback("intro.goosetools.title", "Your role"));
        this.payload = payload;
        this.smallTeammates = payload.roster().stream().anyMatch(RoleIntroPayload.Appearance::small);
    }
    public RoleIntroPayload payload() { return payload; }
    public boolean spectator() { return payload.theme() == RoleIntroPolicy.SPECTATOR; }

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
            if(!PostgameSummaryScreen.waiting()) PresentationBlackoutScreen.apply(false);
            return;
        }
        if (mc.level != null && mc.player != null) mc.gui.setScreen(new RoleIntroScreen(payload));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() {} // Opening owns input until the authoritative transition completes.
    @Override public void tick() {
        if (minecraft.level == null || minecraft.player == null) {
            if (minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
        } else if (System.nanoTime() - started >= RoleIntroScene.DURATION_NANOS && minecraft.gui.screen() == this) {
            PresentationBlackoutScreen.apply(true);
        }
    }
    @Override public void removed() {
        models.clear();
        if (artworkWidth > 0) { minecraft.getTextureManager().release(ARTWORK); artworkWidth = artworkHeight = 0; }
        for (Identifier texture : lightTextures) minecraft.getTextureManager().release(texture);
        lightTextures.clear(); textureReady = false;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.nanoTime() - started;
        float brightness = RoleIntroScene.brightness(elapsed);
        float expansion = spectator() ? Math.clamp(elapsed/1_400_000_000F,0,1) : RoleIntroScene.expansion(elapsed);
        rosterOpacity = spectator() ? RoleIntroScene.ease(Math.clamp((elapsed/1_000_000_000F-.25F)/1.1F,0,1)) : RoleIntroScene.rosterOpacity(elapsed);
        float movement = reducedMotion() ? (expansion >= 1 ? 1 : 0) : RoleIntroScene.ease(expansion);
        int frame = Math.round(movement*(LIGHT_FRAMES-1));
        ensureStage(); ensureLight(frame);
        g.blit(lightTexture(frame), 0, 0, width, height, 0, 1, 0, 1);
        if (minecraft.level != null) {
            MeetingAlertTrueSkinCache.capture(minecraft);
            textOpacity = 1-Math.clamp(expansion/.4F,0,1);
            if (!spectator() && (textOpacity > .01F || RoleIntroAct.keepRoleArtwork(payload.theme()))) drawArtwork(g);
            drawSecondBackdrop(g,movement);
            drawText(g);
            drawCast(g, movement, expansion, elapsed/1_000_000_000.0);
            drawMatchCounts(g);
        }
        // An opaque stage stays in place throughout both fades; the game never flashes through.
        if (brightness < 1) g.fill(0, 0, width, height, ((int) ((1 - brightness) * 255)) << 24);
    }

    private void drawMatchCounts(GuiGraphicsExtractor g) {
        if (rosterOpacity <= .01F || payload.counts().players() == 0) return;
        var counts = payload.counts();
        String[] keys = {"players","geese","ducks","birds"};
        String[] fallback = {"Players","Geese","Ducks","Birds"};
        int[] values = {counts.players(),counts.geese(),counts.ducks(),counts.birds()};
        int[] colors = {0xE6FAFF,0xA9EDB7,0xFF8585,0xFFED94};
        Component[] labels = new Component[4];
        float content = 0;
        for (int i=0;i<4;i++) {
            labels[i] = Component.translatableWithFallback("intro.goosetools.counts."+keys[i],fallback[i]);
            content += 20 + font.width(labels[i]) + 8 + font.width(Integer.toString(values[i])) + 20;
        }
        float scale = Math.min(uiScale(),width*.90F/content);
        g.pose().pushMatrix();
        g.pose().translate((width-content*scale)/2,height*.92F);
        g.pose().scale(scale,scale);

        int x=0;
        for (int i=0;i<4;i++) {
            Identifier icon = Identifier.fromNamespaceAndPath("goosetools","textures/gui/match_counts/"+keys[i]+".png");
            g.blit(RenderPipelines.GUI_TEXTURED,icon,x,-2,0,0,14,14,16,16,16,16,alphaColor(0xFFFFFF,rosterOpacity));
            x+=20;
            g.text(font,labels[i],x,1,alphaColor(0xDBE6DE,rosterOpacity),true);
            x+=font.width(labels[i])+8;
            String value=Integer.toString(values[i]);
            g.text(font,Component.literal(value),x,1,alphaColor(colors[i],rosterOpacity),true);
            x+=font.width(value)+20;
        }
        g.pose().popMatrix();
    }

    private void drawText(GuiGraphicsExtractor g) {
        float ui = uiScale();
        String mode = switch (payload.mode()) { case 1 -> "draft"; case 2 -> "killer"; default -> "classic"; };
        String fallback = switch (payload.mode()) {
            case 1 -> "Game mode: Role Draft"; case 2 -> "Game mode: Killer"; default -> "Game mode: Classic";
        };
        centered(g, Component.translatableWithFallback("intro.goosetools.mode." + mode, fallback),
                width * .5F, height * .16F, 1.22F * ui, 0xFFEAEAEA);
        RoleIntroMap map = RoleIntroMap.of(payload.map(),payload.fullMap());
        if (!map.key().isEmpty()) {
            Component mapName = Component.translatableWithFallback(map.key(),map.fallback());
            centered(g, mapName, width*.5F, height*.09F,
                    Math.min(1.30F*ui,width*.88F/Math.max(1,font.width(mapName))),0xFF000000|map.color());
        }
        if (spectator()) {
            centered(g,Component.translatableWithFallback("intro.goosetools.spectator","Spectator"),
                    width*.5F,height*.235F,2.2F*ui,0xFFE5E7EC);
            return;
        }
        if (textOpacity <= .01F) return;
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
        float titleScale = Math.min(2.95F * ui, width * .88F / Math.max(1, font.width(title)));
        centered(g, title, width * .5F, height * .235F, titleScale, alphaColor(0xFFFFFF,textOpacity));
        Component description = Component.translatableWithFallback(role.descriptionKey(), role.descriptionFallback());
        float descriptionScale = 1.08F * ui;
        var lines = font.split(description, Math.max(30, (int) (width * .82F / descriptionScale)));
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            g.pose().pushMatrix();
            g.pose().translate(width * .5F, height * .35F + i * 12 * descriptionScale);
            g.pose().scale(descriptionScale, descriptionScale);
            g.text(font, line, -font.width(line) / 2, 0, alphaColor(role.faction() == 2 ? role.color() : 0xEFEFEF,textOpacity), true);
            g.pose().popMatrix();
        }
    }

    private static int alphaColor(int rgb, float alpha) {
        return Math.clamp(Math.round(alpha*255),0,255)<<24 | rgb & 0xFFFFFF;
    }

    private List<RoleIntroScene.Slot> slots(List<RoleIntroPayload.Appearance> group, float left, float right, boolean small) {
        float nameScale = .8F*uiScale();
        float widest = group.stream().mapToInt(p -> MeetingNameTagHud.introWidth(minecraft,p.id(),p.name())).max().orElse(40)*nameScale;
        if (spectator()) widest = Math.max(widest,group.stream().filter(p -> !p.observerRole().isEmpty())
                .mapToInt(p -> font.width(Component.translatableWithFallback(RoleIntroCatalog.byTag(p.observerRole()).key(),RoleIntroCatalog.byTag(p.observerRole()).fallback())))
                .max().orElse(0)*nameScale);
        // Bound long localized labels instead of shrinking 21 models into a fourth row.
        if (spectator() && !group.isEmpty()) widest = Math.min(widest,
                width*(right-left)/Math.min(7,group.size())-12*nameScale-.01F);
        var result = RoleIntroScene.grid(width*left,width*right,height*(spectator()?.38F:.34F),height*.86F,group.size(),widest,nameScale,spectator()?2:1);
        if (!small) return result;
        return result.stream().map(p -> new RoleIntroScene.Slot(p.center(),p.floor(),p.scale()*.65F,p.step(),p.nameScale())).toList();
    }

    private void drawCast(GuiGraphicsExtractor g, float movement, float progress, double seconds) {
        if (spectator()) {
            var placements=slots(payload.roster(),.06F,.94F,false);
            for(int i=0;i<payload.roster().size();i++) {
                float alpha=RoleIntroScene.ease((float)Math.clamp((seconds-.25-i*.035)/1.1,0,1));
                drawPlayer(g,payload.roster().get(i),placements.get(i),i,seconds,alpha);
            }
            return;
        }
        var main = payload.roster().stream().filter(p -> !p.small()).toList();
        var small = payload.roster().stream().filter(RoleIntroPayload.Appearance::small).toList();
        if (main.isEmpty()) return;
        boolean duel = RoleIntroAct.duel(payload.theme(),payload.role(),main.size());
        var mainSlots = slots(main,.06F,small.isEmpty()?.94F:.60F,false);
        if (duel && main.size() == 2) mainSlots = List.of(
                new RoleIntroScene.Slot(width*.32F,height*.84F,height*.15F,width*.30F,.9F*uiScale()),
                new RoleIntroScene.Slot(width*.68F,height*.84F,height*.15F,width*.30F,.9F*uiScale()));
        var sideSlots = slots(small,.66F,.96F,true);
        UUID self = minecraft.player.getUUID();
        var hero = main.stream().filter(p -> p.id().equals(self)).findFirst().orElse(main.getFirst());
        for (int i=0;i<main.size();i++) {
            var appearance = main.get(i); var target = mainSlots.get(i);
            if (appearance.id().equals(hero.id())) continue;
            drawPlayer(g,appearance,target,i,seconds,rosterOpacity);
        }
        for (int i=0;i<small.size();i++) drawPlayer(g,small.get(i),sideSlots.get(i),main.size()+i,seconds,rosterOpacity);
        float titleOpacity = RoleIntroScene.ease(Math.clamp((progress-.55F)/.4F,0,1));
        if (titleOpacity > .01F) {
            String key = RoleIntroAct.title(payload.theme(),payload.loneDuck(),payload.role(),main.size());
            drawGroupTitle(g,key,small.isEmpty()?.5F:.33F,RoleIntroAct.color(payload.theme(),payload.role()),titleOpacity,
                    small.isEmpty()?.88F:.48F);
            if (duel) centered(g,Component.literal("VS"),width*.5F,height*.60F,1.7F*uiScale(),alphaColor(0xF3C13D,titleOpacity*.7F));
            if (!small.isEmpty()) drawGroupTitle(g,"colleagues",.81F,0xFF8D91,titleOpacity,.30F);
        }
        var target = mainSlots.get(main.indexOf(hero));
        // The only first-act model moves directly to its ascending seat-order slot.
        var pose = new RoleIntroScene.Slot(RoleIntroScene.mix(width*.5F,target.center(),movement),
                RoleIntroScene.mix(height*.84F,target.floor(),movement),
                RoleIntroScene.mix(height*.17F,target.scale(),movement),
                RoleIntroScene.mix(width*.4F,target.step(),movement),
                RoleIntroScene.mix(.9F*uiScale(),target.nameScale(),movement));
        drawPlayer(g,hero,pose,main.indexOf(hero),seconds,1);
    }

    private void drawPlayer(GuiGraphicsExtractor g, RoleIntroPayload.Appearance appearance,
                            RoleIntroScene.Slot slot, int index, double seconds, float alpha) {
        if(alpha<=.001F) return;
        RemotePlayer model = models.computeIfAbsent(appearance.id(), id -> model(appearance, models.size()));
        var camera = minecraft.getCameraEntity();
        if (camera != null) model.setPos(camera.position());
        float center = slot.center(), floor = slot.floor(), scale = slot.scale(), step = slot.step();
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(model);
        var state = renderer.createRenderState(model,1);
        state.shadowPieces.clear(); state.outlineColor = 0; state.nameTag = null;
        if (state instanceof com.goosethings.tools.client.animation.ResultOutline outline)
            outline.goosetools$resultOutline(spectator() ? alphaColor(appearance.observerGlowRgb(),alpha) : 0);
        ((com.goosethings.tools.client.animation.IntroOpacity)state).goosetools$introOpacity(alpha);
        state.ageInTicks = (float)(seconds*20+index*7);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180F + (reducedMotion()?0:(float)Math.sin(seconds*1.5+index)*3);
            living.yRot = reducedMotion()?0:(float)Math.sin(seconds+index)*4;
            if (RoleIntroAct.duel(payload.theme(),payload.role(),payload.roster().size()) && payload.roster().size()==2)
                living.bodyRot += (index==0?-12F:12F)*RoleIntroScene.ease(RoleIntroScene.expansion((long)(seconds*1_000_000_000L)));
            living.xRot = 0;
            living.walkAnimationPos = (float)(seconds*1.5+index);
            living.walkAnimationSpeed = 0;
        }
        int top = Math.round(floor-scale*2.05F), bottom = Math.round(floor);
        float feetOffset = (bottom-top)*.5F/scale;
        drawContactShadow(g,center,bottom,Math.round(scale),alpha);
        g.entity(state,scale,new Vector3f(0,feetOffset,0),
                new Quaternionf().rotateZ((float)Math.PI),new Quaternionf(),
                Math.round(center-step*.48F),top,Math.round(center+step*.48F),bottom);
        if (spectator() && !appearance.observerRole().isEmpty()) {
            var role=RoleIntroCatalog.byTag(appearance.observerRole());
            var label=Component.translatableWithFallback(role.key(),role.fallback());
            float labelScale=Math.min(slot.nameScale(),(step-8)/Math.max(1,font.width(label)));
            centered(g,label,center,top-28*slot.nameScale(),labelScale,alphaColor(role.color(),alpha));
        }
        MeetingNameTagHud.drawIntro(g,minecraft,appearance.id(),appearance.name(),center,
                top-14*slot.nameScale(),slot.nameScale(),Math.max(12,(int)((step-6)/slot.nameScale())),alpha);
    }

    private RemotePlayer model(RoleIntroPayload.Appearance appearance, int index) {
        UUID isolated = RoleIntroScene.modelId(appearance.id());
        MeetingAlertRemotePlayer model = new MeetingAlertRemotePlayer(minecraft.level,
                new GameProfile(isolated, appearance.name()), () -> skin(appearance.id()));
        model.showAllSkinLayers();
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
            model.setItemSlot(slot, appearance.equipment().get(slot.ordinal()).copy());
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
    private void drawContactShadow(GuiGraphicsExtractor g, float center, float floor, int scale, float opacity) {
        float radius = scale * .46F;
        int depth = Math.max(2, Math.round(scale * .06F));
        for (int y = -depth; y <= depth; y++) {
            float extent = radius * (float) Math.sqrt(Math.max(0, 1 - (float) y * y / (depth * depth)));
            int alpha = Math.round(46F * opacity * (1 - Math.abs(y) / (depth + 1F)));
            g.fill(Math.round(center - extent), Math.round(floor) + y,
                    Math.round(center + extent), Math.round(floor) + y + 1, alpha << 24);
        }
    }
    private void centered(GuiGraphicsExtractor g, Component text, float x, float y, float scale, int color) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale);
        g.text(font, text, -font.width(text) / 2, 0, color, true); g.pose().popMatrix();
    }
    private void drawArtwork(GuiGraphicsExtractor g) {
        if (artworkWidth <= 0) return;
        if (artworkBounds == null || artworkScreenWidth != width || artworkScreenHeight != height) {
            var role = RoleIntroCatalog.byTag(payload.role());
            var description = Component.translatableWithFallback(role.descriptionKey(), role.descriptionFallback());
            float descriptionScale = 1.08F * uiScale();
            int lines = font.split(description, Math.max(30, (int) (width * .82F / descriptionScale))).size();
            double descriptionBottom = height * .35 + lines * 12 * descriptionScale;
            artworkBounds = RoleIntroArtwork.bounds(width, height, artworkWidth, artworkHeight, descriptionBottom);
            artworkScreenWidth = width; artworkScreenHeight = height;
        }
        var box = artworkBounds;
        if (box.width() > 0 && box.height() > 0)
            // Destination size and source texture size are separate; preserve the icon aspect ratio.
            g.blit(RenderPipelines.GUI_TEXTURED,ARTWORK,box.left(),box.top(),0,0,
                    box.width(),box.height(),artworkWidth,artworkHeight,artworkWidth,artworkHeight,
                    alphaColor(0xFFFFFF,RoleIntroAct.keepRoleArtwork(payload.theme())?1:textOpacity));
    }

    private void drawGroupTitle(GuiGraphicsExtractor g, String key, float center, int rgb, float alpha, float available) {
        Component title = Component.translatableWithFallback("intro.goosetools.act2."+key,RoleIntroAct.fallback(key));
        float scale = Math.min(1.82F*uiScale(),width*available/Math.max(1,font.width(title)));
        centered(g,title,width*center,height*.255F,scale,alphaColor(rgb,alpha));
    }
    private void drawSecondBackdrop(GuiGraphicsExtractor g, float progress) {
        if (progress <= .001F) return;
        if (payload.theme() == RoleIntroPolicy.DUCK) {
            int size = Math.round(height*.46F);
            blitMotif(g,DAGGER,25,Math.round((width-size)*.5F),Math.round(height*.35F),size,.16F*progress);
        } else if (payload.theme() == RoleIntroPolicy.LOVER) {
            float[][] hearts = {{.22F,.45F,.045F,.14F},{.35F,.57F,.065F,.13F},{.5F,.46F,.10F,.12F},
                    {.68F,.43F,.05F,.16F},{.78F,.62F,.065F,.12F}};
            for (float[] heart : hearts) {
                int size = Math.round(height*heart[2]);
                blitMotif(g,HEART,32,Math.round(width*heart[0]-size*.5F),Math.round(height*heart[1]-size*.5F),size,heart[3]*progress);
            }
        }
    }
    private void blitMotif(GuiGraphicsExtractor g, Identifier image, int source, int x, int y, int size, float alpha) {
        g.blit(RenderPipelines.GUI_TEXTURED,image,x,y,0,0,size,size,source,source,source,source,alphaColor(0xFFFFFF,alpha));
    }

    private void prepareArtwork() {
        var sprite = RoleIntroArtwork.sprite(payload.role());
        if (sprite.isEmpty()) return;
        String file = sprite.get();
        // Honor the selected resource pack, with the identical bundled image as a fallback.
        var preferred = Identifier.fromNamespaceAndPath("minecraft", "duck".equals(file)
                ? "textures/item/ggd/duck.png" : "textures/gui/sprites/custom/ggdrole/" + file + ".png");
        var fallback = Identifier.fromNamespaceAndPath("goosetools", "textures/gui/role_intro/" + file + ".png");
        for (var location : List.of(preferred, fallback)) {
            var resource = minecraft.getResourceManager().getResource(location);
            if (resource.isEmpty()) continue;
            NativeImage image = null;
            try (var input = resource.get().open()) {
                image = NativeImage.read(input);
                int imageWidth = image.getWidth(), imageHeight = image.getHeight();
                int tint = RoleIntroScene.stageTint(payload.theme(), payload.role());
                for (int y = 0; y < imageHeight; y++) for (int x = 0; x < imageWidth; x++)
                    image.setPixel(x, y, RoleIntroArtwork.litPixel(image.getPixel(x, y), tint));
                minecraft.getTextureManager().register(ARTWORK, new DynamicTexture(() -> "GooseTools role artwork", image));
                image = null; // The texture manager owns the image from this point.
                artworkWidth = imageWidth; artworkHeight = imageHeight;
                return;
            } catch (java.io.IOException | RuntimeException error) {
                GooseTools.LOGGER.warn("Unable to load role intro artwork {}: {}", location, error.getMessage());
            } finally {
                if (image != null) image.close();
            }
        }
    }

    private void ensureStage() {
        if (textureReady) return;
        ensureLight(0);
        textureReady = true;
        if (!spectator()) prepareArtwork();
    }
    /** Generate only reached frames, once per opening; no per-frame texture uploads. */
    private void ensureLight(int frame) {
        Identifier texture = lightTexture(frame);
        if (lightTextures.contains(texture)) return;
        NativeImage image = new NativeImage(320,180,false);
        for (int y=0;y<180;y++) for(int x=0;x<320;x++) image.setPixel(x,y,
                RoleIntroScene.stagePixel(payload.theme(),payload.role(),x/319.0,y/179.0,frame/(float)(LIGHT_FRAMES-1),smallTeammates));
        minecraft.getTextureManager().register(texture,new DynamicTexture(() -> "GooseTools opening light",image));
        lightTextures.add(texture);
    }
}
