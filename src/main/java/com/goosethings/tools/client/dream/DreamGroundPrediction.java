package com.goosethings.tools.client.dream;

import com.goosethings.tools.dream.DreamAvatarPackets;
import com.goosethings.tools.dream.DreamGroundBody;
import com.goosethings.tools.dream.DreamJumpState;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** Replays unacknowledged controls with the same native collision/jump physics as the server. */
final class DreamGroundPrediction {
    private final ArrayDeque<DreamAvatarPackets.Input> pending=new ArrayDeque<>();
    private DreamGroundBody physics;
    private Vec3 correction=Vec3.ZERO;
    private long tick=-1,epoch=-1;
    private boolean walking;
    private double speed;

    boolean active() {return physics!=null && walking;}

    void accept(DreamAvatarPackets.Ground state,DreamRemotePlayer model) {
        if(state.tick()<=tick)return;
        tick=state.tick();
        while(!pending.isEmpty() && pending.peekFirst().sequence()<=state.sequence())pending.removeFirst();
        boolean snap=physics==null || epoch!=state.epoch() || walking!=state.walking();
        if(physics==null || physics.level()!=model.level())physics=new PhysicsBody(model.level());
        physics.absSnapTo(state.position().x,state.position().y,state.position().z);
        physics.setDeltaMovement(state.velocity());physics.setOnGround(state.onGround());
        physics.fallDistance=state.fallDistance();
        ((DreamJumpState)physics).dream$setJumpDelay(state.jumpDelay());
        physics.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(state.stepHeight());
        speed=state.speed();epoch=state.epoch();walking=state.walking() && !model.dreamRiding;
        if(!walking) {pending.clear();correction=Vec3.ZERO;return;}
        for(var input:pending) {
            if(!physics.level().hasChunkAt(physics.blockPosition()))break;
            physics.stepGround(input,speed);
        }
        Vec3 error=model.position().subtract(physics.position());
        correction=snap || error.lengthSqr()>16 || physics.horizontalCollision?Vec3.ZERO:error;
        if(physics.onGround())correction=new Vec3(correction.x,0,correction.z);
        model.getInterpolation().cancel();model.setPos(physics.position().add(correction));
        if(snap)model.setOldPosAndRot();
    }

    void tick(DreamAvatarPackets.Input input,DreamRemotePlayer model) {
        if(model.dreamRiding) {walking=false;pending.clear();correction=Vec3.ZERO;}
        if(!active() || pending.size()>=64)return;
        pending.addLast(input);
        if(!physics.level().hasChunkAt(physics.blockPosition()))return;
        physics.stepGround(input,speed);
        correction=physics.horizontalCollision?Vec3.ZERO:correction.scale(.65);
        model.setPos(physics.position().add(correction));model.setPose(physics.getPose());
    }

    /** Replaying history must not emit repeated footsteps or push visible client entities. */
    private static final class PhysicsBody extends DreamGroundBody {
        PhysicsBody(net.minecraft.world.level.Level level) {super(level);}
        @Override public boolean isPushable() {return false;}
        @Override public boolean canSpawnSprintParticle() {return false;}
        @Override protected void doWaterSplashEffect() {}
        @Override protected MovementEmission getMovementEmission() {return MovementEmission.NONE;}
    }
}
