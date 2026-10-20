package com.goosethings.tools.dream;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.Level;

/** Unspawned native humanoid physics, shared by the authoritative actor and local replay. */
public class DreamGroundBody extends Mannequin {
    public DreamGroundBody(Level level) { super(level); }

    @Override protected boolean isLocalClientAuthoritative() { return true; }

    public void stepGround(DreamAvatarPackets.Input input, double speed) {
        int buttons=input.buttons();
        boolean crouching=(buttons&2)!=0;
        setYRot(input.yaw());setXRot(input.pitch());
        setPose(crouching?Pose.CROUCHING:Pose.STANDING);
        setSprinting((buttons&4)!=0);
        xxa=input.sideways()*(crouching?.3F:1);zza=input.forward()*(crouching?.3F:1);
        setJumping((buttons&1)!=0);
        setSpeed((float)speed*((buttons&4)!=0?1.3F:1));
        tick();
    }
}
