package com.goosethings.tools.client.punchy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.SkullBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/** Optional bridge to Expressions; owns no player, skin or world caches. */
public final class PunchyWornHeadFaces {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunchyWornHeadFaces.class);
    private static final ThreadLocal<Boolean> DRAWING_HEAD = new ThreadLocal<>();
    private static boolean lookedUp;
    private static Method facesEnabled;
    private static Method glintTexture;

    private PunchyWornHeadFaces() { }

    public static PlayerSkin wornSkin(AvatarRenderState state) {
        if (state.wornHeadType != SkullBlock.Types.PLAYER || state.wornHeadProfile == null) return null;
        // Use precisely the same resolved/fallback skin as the visible equipment, including disguises.
        return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(state.wornHeadProfile).playerSkin();
    }

    public static void submit(AvatarRenderState state, Runnable draw) {
        PlayerSkin headSkin = wornSkin(state);
        if (headSkin == null) {
            draw.run();
            return;
        }
        PlayerSkin bodySkin = state.skin;
        boolean bodyHat = state.showHat;
        Boolean previous = DRAWING_HEAD.get();
        try {
            state.skin = headSkin;
            // A skull's hat is independent of the wearer's skin-customization setting.
            state.showHat = true;
            DRAWING_HEAD.set(true);
            draw.run();
        } finally {
            state.skin = bodySkin;
            state.showHat = bodyHat;
            if (previous == null) DRAWING_HEAD.remove();
            else DRAWING_HEAD.set(previous);
        }
    }

    public static void scaleHead(PoseStack pose) {
        if (Boolean.TRUE.equals(DRAWING_HEAD.get())) {
            pose.scale(CustomHeadLayer.SKULL_SCALE, CustomHeadLayer.SKULL_SCALE, CustomHeadLayer.SKULL_SCALE);
        }
    }

    public static void scaleHat(PoseStack pose) {
        if (Boolean.TRUE.equals(DRAWING_HEAD.get())) {
            // Skull hats inflate by 0.25 px; player hats inflate by 0.5 px.
            // Match their dimensions around the head's centre, rather than the neck origin.
            pose.translate(0, -4 / 16.0F, 0);
            pose.scale(17 / 18.0F, 17 / 18.0F, 17 / 18.0F);
            pose.translate(0, 4 / 16.0F, 0);
        }
    }

    public static Identifier glintTexture(AvatarRenderState state, PlayerSkin headSkin) {
        Identifier original = headSkin.body().texturePath();
        if (!lookedUp) {
            lookedUp = true;
            if (FabricLoader.getInstance().isModLoaded("punchy_expressions")) {
                try {
                    facesEnabled = Class.forName("punchymoves.client.face.FaceSettings").getMethod("faces");
                    glintTexture = Class.forName("punchymoves.client.face.FaceGlints")
                            .getMethod("texture", AvatarRenderState.class, Identifier.class);
                } catch (ReflectiveOperationException | LinkageError error) {
                    LOGGER.warn("Punchy worn-head glints are unavailable for this Expressions version", error);
                }
            }
        }
        if (facesEnabled == null || glintTexture == null) return original;
        try {
            if (!(boolean) facesEnabled.invoke(null)) return original;
            Identifier[] result = {original};
            submit(state, () -> {
                try { result[0] = (Identifier) glintTexture.invoke(null, state, original); }
                catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
            return result[0];
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.warn("Punchy worn-head glints could not be resolved; keeping the equipment texture", error);
            facesEnabled = null;
            glintTexture = null;
            return original;
        }
    }
}
