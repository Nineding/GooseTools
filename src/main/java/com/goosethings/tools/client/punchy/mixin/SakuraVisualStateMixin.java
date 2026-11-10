package com.goosethings.tools.client.punchy.mixin;
import com.goosethings.tools.client.swordman.SakuraIaidoPose;
import com.goosethings.tools.client.swordman.SakuraVisualState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(AvatarRenderState.class)
abstract class SakuraVisualStateMixin implements SakuraVisualState {
    @Unique private SakuraIaidoPose.Frame goosetools$iaido;
    public SakuraIaidoPose.Frame goosetools$iaido(){return goosetools$iaido;}
    public void goosetools$iaido(SakuraIaidoPose.Frame frame){goosetools$iaido=frame;}
}
