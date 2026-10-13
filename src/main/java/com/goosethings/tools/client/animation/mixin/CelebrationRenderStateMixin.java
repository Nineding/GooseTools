package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.CelebrationState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class CelebrationRenderStateMixin implements CelebrationState {
    @Unique private int goosetools$action=-1;
    @Unique private float goosetools$seconds;
    @Override public void goosetools$celebration(int action,float seconds) {goosetools$action=action;goosetools$seconds=seconds;}
    @Override public int goosetools$action() {return goosetools$action;}
    @Override public float goosetools$seconds() {return goosetools$seconds;}
}
