package com.goosethings.tools.client.outfit;

import com.goosethings.tools.outfit.OutfitSnapshot;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Changes rendering only: inventory, server interaction targets and stored wardrobes survive. */
public final class OutfitClient {
    private static boolean enabled=true;
    private static Map<UUID,OutfitSnapshot.Corpse> corpses=Map.of();
    private static final Map<UUID,Body> MODELS=new HashMap<>();
    private static final Map<HumanoidRenderState,Rotations[]> POSES=new WeakHashMap<>();
    private OutfitClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OutfitSnapshot.TYPE,(p,c)->c.client().execute(()->apply(p)));
        ClientPlayConnectionEvents.JOIN.register((h,s,c)->clear());
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());
    }
    public static void apply(OutfitSnapshot p) {
        enabled=p.enabled();var rows=new HashMap<UUID,OutfitSnapshot.Corpse>();p.corpses().forEach(row->rows.put(row.entity(),row));corpses=Map.copyOf(rows);
        MODELS.keySet().retainAll(corpses.keySet());if(enabled)POSES.clear();
    }
    public static boolean enabled() { return enabled; }
    public static void clear() { enabled=true;corpses=Map.of();MODELS.clear();POSES.clear(); }
    public static void filter(AvatarRenderState state) {
        POSES.remove(state);
        if(enabled)return;
        state.headEquipment=state.chestEquipment=state.legsEquipment=state.feetEquipment=ItemStack.EMPTY;
        state.headItem.clear();state.heldOnHead.clear();state.wornHeadType=null;state.wornHeadProfile=null;
    }
    public static EntityRenderState replace(Entity entity, EntityRenderState original, float partial) {
        if(original.isInvisible||!(entity instanceof ArmorStand stand)||com.goosethings.tools.client.vision.VisionFogState.shouldHideEntityVisual(entity))return original;
        var row=corpses.get(entity.getUUID());var mc=Minecraft.getInstance();if(row==null||mc.level==null)return original;
        Body body=MODELS.get(entity.getUUID());
        if(body==null||body.level()!=mc.level||!body.owner.equals(row.owner())) {body=new Body(mc.level,row);MODELS.put(entity.getUUID(),body);}
        for(var slot:EquipmentSlot.values()) body.setItemSlot(slot,enabled&&slot.isArmor()?stand.getItemBySlot(slot).copy():ItemStack.EMPTY);
        body.tickCount=(int)original.ageInTicks;body.setId(stand.getId());body.absSnapTo(original.x,original.y,original.z,stand.getYRot(),0);body.yBodyRotO=body.yBodyRot=stand.getYRot();body.yHeadRotO=body.yHeadRot=stand.getYRot();
        AvatarRenderState state=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(body).createRenderState(body,partial);
        state.x=original.x;state.y=original.y;state.z=original.z;state.lightCoords=original.lightCoords;
        if(original instanceof LivingEntityRenderState living){state.scale=living.scale;state.ageScale=living.ageScale;}
        state.outlineColor=original.outlineColor;state.isInvisible=false;state.isInvisibleToPlayer=false;
        state.nameTag=null;state.scoreText=null;state.shadowPieces.clear();state.deathTime=0;state.walkAnimationSpeed=0;
        state.bodyRot=stand.getYRot();state.yRot=0;state.xRot=0;state.isPassenger=true;
        // Detached corpses do not tick ClientAvatarState's cloak physics. A static corpse
        // must not derive cloth motion from stale cloak positions and the frame partial tick.
        state.capeFlap=0;state.capeLean=0;state.capeLean2=0;
        state.id=stand.getId();
        POSES.put(state,new Rotations[]{stand.getHeadPose(),stand.getBodyPose(),stand.getLeftArmPose(),stand.getRightArmPose(),stand.getLeftLegPose(),stand.getRightLegPose()});
        return state;
    }
    public static void pose(HumanoidRenderState state, ModelPart head,ModelPart body,ModelPart leftArm,ModelPart rightArm,ModelPart leftLeg,ModelPart rightLeg) {
        var rotations=POSES.get(state);if(rotations==null)return;
        ModelPart[] parts={head,body,leftArm,rightArm,leftLeg,rightLeg};
        for(int i=0;i<parts.length;i++){var r=rotations[i];parts[i].setRotation(net.minecraft.util.Mth.wrapDegrees(r.x())*(float)Math.PI/180,net.minecraft.util.Mth.wrapDegrees(r.y())*(float)Math.PI/180,net.minecraft.util.Mth.wrapDegrees(r.z())*(float)Math.PI/180);}
        // Bend the torso around its hip, then carry the neck and shoulders with it.
        // Merely rotating each vanilla part leaves a visible gap above the seated legs.
        var rotation=new org.joml.Quaternionf().rotationZYX(body.zRot,body.yRot,body.xRot);
        var hipOffset=new org.joml.Vector3f(0,12,0).rotate(rotation);
        var origin=new org.joml.Vector3f(0,12,0).sub(hipOffset);
        body.setPos(origin.x,origin.y,origin.z);head.setPos(origin.x,origin.y,origin.z);
        var leftShoulder=new org.joml.Vector3f(5,2,0).rotate(rotation).add(origin);
        var rightShoulder=new org.joml.Vector3f(-5,2,0).rotate(rotation).add(origin);
        leftArm.setPos(leftShoulder.x,leftShoulder.y,leftShoulder.z);rightArm.setPos(rightShoulder.x,rightShoulder.y,rightShoulder.z);
    }
    private static final class Body extends RemotePlayer {
        final UUID owner;
        Body(ClientLevel level,OutfitSnapshot.Corpse row) {
            super(level,new GameProfile(row.entity(),row.accountName()));owner=row.owner();noPhysics=true;setNoGravity(true);
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,(byte)0x7f);
        }
        @Override public PlayerSkin getSkin() {
            if(owner==null)return super.getSkin();
            var skin=com.goosethings.tools.client.hud.MeetingAlertTrueSkinCache.get(owner);
            if(skin!=null)return skin;
            var mc=Minecraft.getInstance();var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(owner);
            return info==null?DefaultPlayerSkin.get(owner):info.getSkin();
        }
    }
}
