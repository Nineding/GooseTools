package com.goosethings.tools.client.animation.mixin;

import com.goosethings.tools.client.animation.CelebrationState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class CelebrationRenderStateMixin implements CelebrationState, com.goosethings.tools.client.animation.IntroOpacity, com.goosethings.tools.client.animation.ResultOutline {
    @Unique private int goosetools$resultOutline;
    @Override public int goosetools$resultOutline() { return goosetools$resultOutline; }
    @Override public void goosetools$resultOutline(int color) { goosetools$resultOutline = color; }
    @Unique private float goosetools$introOpacity=1;
    @Override public void goosetools$introOpacity(float alpha) {goosetools$introOpacity=alpha;}
    @Override public float goosetools$introOpacity() {return goosetools$introOpacity;}
    @Unique private int goosetools$action=-1;
    @Unique private float goosetools$seconds;
    @Override public void goosetools$celebration(int action,float seconds) {goosetools$action=action;goosetools$seconds=seconds;}
    @Override public int goosetools$action() {return goosetools$action;}
    @Override public float goosetools$seconds() {return goosetools$seconds;}
}
