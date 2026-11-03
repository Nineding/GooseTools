package com.goosethings.tools.client.training;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NPCs use a packaged skin, so an internet skin lookup is never required. */
@Mixin(AbstractClientPlayer.class)
abstract class TrainingNpcSkinMixin {
    private static final PlayerSkin TRAINING_SKIN=new PlayerSkin(
        new ClientAsset.ResourceTexture(Identifier.parse("minecraft:entity/player/slim/unknow"),
            Identifier.parse("minecraft:textures/entity/player/slim/unknow.png")),null,null,PlayerModelType.SLIM,false);
    @Inject(method="getSkin",at=@At("HEAD"),cancellable=true)
    private void goose$trainingSkin(CallbackInfoReturnable<PlayerSkin> ci) {
        var player=(AbstractClientPlayer)(Object)this;
        if(player.getGameProfile().name().startsWith("GT_NPC_"))ci.setReturnValue(TRAINING_SKIN);
    }
}
