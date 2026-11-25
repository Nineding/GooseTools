package com.goosethings.tools.client.broodkeeper;

import com.goosethings.tools.broodkeeper.BroodkeeperChick;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BroodkeeperChickRenderer extends EntityRenderer<BroodkeeperChick, BroodkeeperChickRenderer.State> {
    private static final ItemStack BODY = part("body");
    private static final ItemStack TAIL = part("tail");
    private static final ItemStack LEFT_WING = part("left_wing");
    private static final ItemStack RIGHT_WING = part("right_wing");
    private static final ItemStack LEFT_LEG = part("left_leg");
    private static final ItemStack RIGHT_LEG = part("right_leg");
    private static final float[] IDLE_BOB = {0, -0.2f};
    private static final float[] IDLE_TAIL = {0, 8, 0, -5};
    private static final float[] STEP_LEG = {0, -35, 0, 35};
    private static final float[] STEP_BOB = {0, 0.25f, 0, 0.25f};
    private static final float[] STEP_PITCH = {0, 2, 0, 2};
    private static final float[] STEP_ROLL = {0, 5, 0, -5};
    private static final float[] STEP_TAIL_PITCH = {0, 10, 0, 10};
    private static final float[] STEP_TAIL_ROLL = {0, 6, 0, -6};
    private static final float[] FLAP = {10, 85};

    public static void register() {
        EntityRendererRegistry.register(BroodkeeperChick.type(), BroodkeeperChickRenderer::new);
    }

    private BroodkeeperChickRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static final class State extends EntityRenderState {
        final ItemStackRenderState body = new ItemStackRenderState();
        final ItemStackRenderState tail = new ItemStackRenderState();
        final ItemStackRenderState leftWing = new ItemStackRenderState();
        final ItemStackRenderState rightWing = new ItemStackRenderState();
        final ItemStackRenderState leftLeg = new ItemStackRenderState();
        final ItemStackRenderState rightLeg = new ItemStackRenderState();
        boolean carried;
        float yaw;
        float gait;
        float gaitWeight;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected AABB getBoundingBoxForCulling(BroodkeeperChick chick, float partial) {
        AABB bounds = super.getBoundingBoxForCulling(chick, partial);
        LivingEntity holder = carrier(chick);
        if (holder == null) return bounds;
        Vec3 point = carriedPosition(holder, bodyYaw(holder, partial), partial);
        return bounds.move(point.subtract(chick.position()));
    }

    @Override
    public void extractRenderState(BroodkeeperChick chick, State state, float partial) {
        super.extractRenderState(chick, state, partial);
        state.carried = chick.carrierId() >= 0;
        state.yaw = chick.getYRot(partial);
        state.gait = chick.walkAnimation.position(partial) / 12;
        state.gaitWeight = Mth.clamp(chick.walkAnimation.speed(partial) * 3, 0, 1);
        LivingEntity holder = carrier(chick);
        if (holder != null) {
            state.yaw = bodyYaw(holder, partial);
            Vec3 point = carriedPosition(holder, state.yaw, partial);
            state.x = point.x;
            state.y = point.y;
            state.z = point.z;
        }
        updateModels(chick, state);
    }

    private static LivingEntity carrier(BroodkeeperChick chick) {
        return chick.carrierId() >= 0 && chick.level().getEntity(chick.carrierId()) instanceof LivingEntity holder
                ? holder : null;
    }

    private static float bodyYaw(LivingEntity holder, float partial) {
        return Mth.rotLerp(partial, holder.yBodyRotO, holder.yBodyRot);
    }

    private static Vec3 carriedPosition(LivingEntity holder, float yaw, float partial) {
        Vec3 offset = Vec3.directionFromRotation(0, yaw).scale(0.48);
        return holder.getPosition(partial).add(offset).add(0, holder.getEyeHeight() - 0.64, 0);
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            poses.rotateDegrees(Axis.YP, 180 - state.yaw);
            draw(state, poses, collector);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }

    public static void submitHeld(BroodkeeperChick chick, float age, int light, PoseStack poses,
            SubmitNodeCollector collector) {
        State state = new State();
        state.carried = true;
        state.ageInTicks = age;
        state.lightCoords = light;
        state.outlineColor = EntityRenderState.NO_OUTLINE;
        updateModels(chick, state);
        poses.pushPose();
        poses.rotateDegrees(Axis.YP, 180);
        draw(state, poses, collector);
        poses.popPose();
    }

    private static ItemStack part(String name) {
        ItemStack item = new ItemStack(Items.PAPER);
        item.set(DataComponents.ITEM_MODEL, Identifier.parse("ggd:broodkeeper/parts/" + name));
        return item;
    }

    private static void updateModels(BroodkeeperChick chick, State state) {
        var models = Minecraft.getInstance().getItemModelResolver();
        models.updateForNonLiving(state.body, BODY, ItemDisplayContext.FIXED, chick);
        models.updateForNonLiving(state.tail, TAIL, ItemDisplayContext.FIXED, chick);
        models.updateForNonLiving(state.leftWing, LEFT_WING, ItemDisplayContext.FIXED, chick);
        models.updateForNonLiving(state.rightWing, RIGHT_WING, ItemDisplayContext.FIXED, chick);
        models.updateForNonLiving(state.leftLeg, LEFT_LEG, ItemDisplayContext.FIXED, chick);
        models.updateForNonLiving(state.rightLeg, RIGHT_LEG, ItemDisplayContext.FIXED, chick);
    }

    private static void draw(State state, PoseStack poses, SubmitNodeCollector collector) {
        float idle = state.ageInTicks / 40;
        float weight = state.carried ? 0 : state.gaitWeight;
        float gait = state.gait;
        float yaw = (float) Math.sin(idle * 2 * Math.PI) * 6 * (1 - weight);
        float bob = Mth.lerp(weight, sample(idle, IDLE_BOB, true), sample(gait, STEP_BOB, true));
        float pitch = sample(gait, STEP_PITCH, true) * weight;
        float roll = sample(gait, STEP_ROLL, true) * weight;
        float wings = 0;
        if (state.carried) {
            float struggle = state.ageInTicks % 64;
            wings = struggle < 10 ? sample(struggle / 5, FLAP, false) * 0.43f : 0;
            roll = wings * 0.14f * (float) Math.sin(struggle);
        }

        poses.pushPose();
        pivot(poses, 0, 3.5f + bob, 0.5f);
        rotate(poses, pitch, yaw, roll);
        submit(state.body, state, poses, collector);
        float tailPitch = Mth.lerp(weight, sample(idle, IDLE_TAIL, true), sample(gait, STEP_TAIL_PITCH, true));
        limb(state.tail, state, poses, collector, 0, 2.5f, 3, 22.5f + tailPitch,
                sample(gait, STEP_TAIL_ROLL, true) * weight);
        limb(state.leftWing, state, poses, collector, 3, 2, 0, 0, -wings);
        limb(state.rightWing, state, poses, collector, -3, 2, 0, 0, wings);
        poses.popPose();

        float leg = sample(gait, STEP_LEG, false) * weight;
        float tuck = state.carried ? 20 : 0;
        limb(state.leftLeg, state, poses, collector, 1.5f, 2, 0.5f, tuck + leg, 0);
        limb(state.rightLeg, state, poses, collector, -1.5f, 2, 0.5f, tuck - leg, 0);
    }

    private static float sample(float cycle, float[] keys, boolean smooth) {
        float index = (cycle - Mth.floor(cycle)) * keys.length;
        int start = Mth.floor(index);
        float blend = index - start;
        int end = (start + 1) % keys.length;
        if (!smooth) return Mth.lerp(blend, keys[start], keys[end]);
        float a = keys[(start + keys.length - 1) % keys.length];
        float b = keys[start];
        float c = keys[end];
        float d = keys[(end + 1) % keys.length];
        return 0.5f * ((2 * b) + (-a + c) * blend
                + (2 * a - 5 * b + 4 * c - d) * blend * blend
                + (-a + 3 * b - 3 * c + d) * blend * blend * blend);
    }

    private static void limb(ItemStackRenderState mesh, State state, PoseStack poses, SubmitNodeCollector collector,
            float x, float y, float z, float pitch, float roll) {
        poses.pushPose();
        pivot(poses, x, y, z);
        rotate(poses, pitch, 0, roll);
        submit(mesh, state, poses, collector);
        poses.popPose();
    }

    private static void pivot(PoseStack poses, float x, float y, float z) {
        poses.translate(x / 16, y / 16, z / 16);
    }

    private static void rotate(PoseStack poses, float pitch, float yaw, float roll) {
        if (roll != 0) poses.rotateDegrees(Axis.ZP, roll);
        if (yaw != 0) poses.rotateDegrees(Axis.YP, yaw);
        if (pitch != 0) poses.rotateDegrees(Axis.XP, pitch);
    }

    private static void submit(ItemStackRenderState mesh, State state, PoseStack poses, SubmitNodeCollector collector) {
        mesh.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
