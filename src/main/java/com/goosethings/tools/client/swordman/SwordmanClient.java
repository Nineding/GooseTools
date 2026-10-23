package com.goosethings.tools.client.swordman;

import com.goosethings.tools.swordman.SwordmanPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class SwordmanClient {
    private static final Map<HumanoidRenderState,UUID> ENTITIES=new WeakHashMap<>();
    private static final Map<UUID,Snapshot> RECENT=new HashMap<>();
    private static final Map<UUID,float[]> FROZEN=new HashMap<>();
    private static boolean held;
    private static long sequence,tick;
    private static Vec3 motionTarget, motionPrevious;
    private static net.minecraft.client.multiplayer.ClientLevel motionLevel;
    private static long motionTick;
    private SwordmanClient() { }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            tick++;
            if(mc.player==null||mc.level==null){clear();return;}
            applyMotion(mc);
            snapshotLocal(mc);
            var data=mc.player.getMainHandItem().get(DataComponents.CUSTOM_DATA);
            boolean canHold=mc.gui.screen()==null&&data!=null&&data.copyTag().getBooleanOr("SwordmanSkill",false);
            boolean down=canHold&&mc.options.keyUse.isDown();
            if((down!=held||down&&tick%10==0)&&ClientPlayNetworking.canSend(SwordmanPackets.Input.TYPE))
                ClientPlayNetworking.send(new SwordmanPackets.Input(down,!canHold,++sequence));
            held=down;
            RECENT.entrySet().removeIf(e->tick-e.getValue().tick>40);
        });
        ClientPlayNetworking.registerGlobalReceiver(SwordmanPackets.Corpse.TYPE,(packet,context)->context.client().execute(()->{
            if(!packet.frozen()){FROZEN.remove(packet.corpse());return;}
            if(packet.parts().length==36){FROZEN.put(packet.corpse(),packet.parts());return;}
            FROZEN.putIfAbsent(packet.corpse(),new float[0]);
            Snapshot source=RECENT.get(packet.owner());
            if(source!=null&&tick-source.tick<=10) {
                if(FROZEN.get(packet.corpse()).length==0)FROZEN.put(packet.corpse(),source.parts);
                if(ClientPlayNetworking.canSend(SwordmanPackets.PoseReply.TYPE))
                    ClientPlayNetworking.send(new SwordmanPackets.PoseReply(packet.corpse(),source.parts));
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(SwordmanPackets.Motion.TYPE,(packet,context)->context.client().execute(()->{
            Minecraft mc=context.client();
            if (!packet.active() || mc.player==null || mc.level==null) { clearMotion(); return; }
            if (motionTarget==null || motionLevel!=mc.level) motionPrevious=mc.player.position();
            motionLevel=mc.level; motionTarget=new Vec3(packet.x(),packet.y(),packet.z()); motionTick=tick;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->clear());
    }
    public static void bind(Entity entity,HumanoidRenderState state) { ENTITIES.put(state,entity.getUUID()); }
    static boolean isDashing() { return motionTarget!=null; }
    private static void applyMotion(Minecraft mc) {
        if (motionTarget==null) return;
        if (motionLevel!=mc.level || tick-motionTick>20) { clearMotion(); return; }
        var player=mc.player;
        player.setPos(motionTarget);
        // Preserve the last approved sample for vanilla camera/model interpolation between frames.
        // Repeated vanilla teleport packets reset these fields and turn a smooth curve into 20 Hz snaps.
        player.xo=motionPrevious.x; player.yo=motionPrevious.y; player.zo=motionPrevious.z;
        player.setDeltaMovement(Vec3.ZERO); player.fallDistance=0;
        motionPrevious=motionTarget;
    }
    private static void clearMotion() { motionTarget=null; motionPrevious=null; motionLevel=null; }
    /** First-person players also contribute a recent pose, even when nobody else was rendering them. */
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void snapshotLocal(Minecraft client) {
        if(client.player.isSpectator())return;
        net.minecraft.client.renderer.entity.EntityRenderer renderer=client.getEntityRenderDispatcher().getRenderer(client.player);
        if(renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer avatar) {
            var state=(net.minecraft.client.renderer.entity.state.AvatarRenderState)renderer.createRenderState(client.player,1F);
            bind(client.player,state);
            avatar.getModel().setupAnim(state);
        }
    }
    public static void apply(HumanoidRenderState state,ModelPart... parts) {
        UUID id=ENTITIES.get(state);if(id==null||parts.length!=6)return;
        float[] pose=FROZEN.get(id);
        if(pose!=null&&pose.length==36) {
            for(int i=0;i<6;i++) {
                int j=i*6;parts[i].setPos(pose[j],pose[j+1],pose[j+2]);
                parts[i].setRotation(pose[j+3],pose[j+4],pose[j+5]);
            }
        } else {
            float[] values=new float[36];
            for(int i=0;i<6;i++) {
                int j=i*6;var p=parts[i];values[j]=p.x;values[j+1]=p.y;values[j+2]=p.z;
                values[j+3]=p.xRot;values[j+4]=p.yRot;values[j+5]=p.zRot;
            }
            if(pose!=null)FROZEN.put(id,values);
            else RECENT.put(id,new Snapshot(values,tick));
        }
    }
    private static void clear(){held=false;clearMotion();ENTITIES.clear();RECENT.clear();FROZEN.clear();}
    private record Snapshot(float[] parts,long tick) { }
}
