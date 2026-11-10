package com.goosethings.tools.client.punchy.mixin;

import com.goosethings.tools.client.swordman.SakuraIaidoClient;
import com.goosethings.tools.client.swordman.SakuraIaidoRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Punchy may submit hands in a separate pass, including before vanilla's HEAD callbacks. */
@Pseudo
@Mixin(targets="punchy.client.render.PunchyArmRenderer",remap=false)
abstract class PunchyIaidoHandsMixin {
    @Inject(method="renderFirstPerson",at=@At("HEAD"),cancellable=true,require=0)
    private static void goosetools$ownIaidoHands(CallbackInfo ci) {
        if(SakuraIaidoClient.active() && SakuraIaidoRenderer.ready()
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) ci.cancel();
    }
}
