package com.goosethings.tools.client.animation;

import com.goosethings.tools.broodkeeper.BroodkeeperChick;
import com.goosethings.tools.client.broodkeeper.BroodkeeperChickRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelType;

public final class BroodkeeperPose {
    private static PlayerModel slim;
    private static PlayerModel wide;

    private BroodkeeperPose() {}

    private static boolean isCarriedBy(BroodkeeperChick chick, Entity owner) {
        return !chick.isInvisible() && chick.carrierId() == owner.getId();
    }

    public static boolean hideLocalChick(Entity entity) {
        Minecraft game = Minecraft.getInstance();
        return game.player != null && game.options.getCameraType().isFirstPerson()
                && entity instanceof BroodkeeperChick chick && isCarriedBy(chick, game.player);
    }

    private static BroodkeeperChick chick(Entity owner) {
        if (owner == null) return null;
        var matches = owner.level().getEntitiesOfClass(BroodkeeperChick.class,
                owner.getBoundingBox().inflate(3), chick -> isCarriedBy(chick, owner));
        return matches.isEmpty() ? null : matches.getFirst();
    }

    public static boolean carrying(AvatarRenderState state) {
        Minecraft game = Minecraft.getInstance();
        return game.level != null && chick(game.level.getEntity(state.id)) != null;
    }

    public static void pose(PlayerModel model, AvatarRenderState state) {
        if (!carrying(state)) return;
        float bob = 0.035f * (float) Math.sin(state.ageInTicks * 0.19f);
        model.rightArm.setRotation(-1.19f + bob, -0.14f, 0.20f);
        model.leftArm.setRotation(-1.19f + bob, 0.14f, -0.20f);
    }

    public static boolean firstPerson(PoseStack poses, SubmitNodeCollector collector, AvatarRenderState state) {
        Minecraft game = Minecraft.getInstance();
        if (state == null || game.player == null || !game.options.getCameraType().isFirstPerson()) return false;
        BroodkeeperChick chick = chick(game.player);
        if (chick == null) return false;

        boolean thin = state.skin.model() == PlayerModelType.SLIM;
        if (thin && slim == null) slim = new PlayerModel(game.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        if (!thin && wide == null) wide = new PlayerModel(game.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        PlayerModel model = thin ? slim : wide;
        float bob = 0.018f * (float) Math.sin(state.ageInTicks * 0.19f);
        poses.pushPose();
        poses.translate(0, -0.49f + bob, -0.64f);
        BroodkeeperChickRenderer.submitHeld(chick, state.ageInTicks, state.lightCoords, poses, collector);
        poses.popPose();

        poses.pushPose();
        poses.translate(0, -0.55f + bob, -0.35f);
        poses.scale(-1, -1, 1);
        collector.submitCustomGeometry(poses, RenderTypes.entityCutout(state.skin.body().texturePath()), (frame, output) -> {
            PoseStack arms = new PoseStack();
            arms.last().pose().set(frame.pose());
            arms.last().normal().set(frame.normal());
            model.rightArm.setPos(-5, 1.5f, 0);
            model.leftArm.setPos(5, 1.5f, 0);
            model.rightArm.setRotation(-1.06f, 0.24f, 0.19f);
            model.leftArm.setRotation(-1.06f, -0.24f, -0.19f);
            model.rightSleeve.visible = state.showRightSleeve;
            model.leftSleeve.visible = state.showLeftSleeve;
            model.rightArm.render(arms, output, state.lightCoords, OverlayTexture.NO_OVERLAY);
            model.leftArm.render(arms, output, state.lightCoords, OverlayTexture.NO_OVERLAY);
        });
        poses.popPose();
        return true;
    }
}
