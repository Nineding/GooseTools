package com.goosethings.tools.dream.mixin;

import com.goosethings.tools.dream.DreamAvatarServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The seated player's empty native input must not undo the mirrored dream pose. */
@Mixin(Player.class)
public abstract class DreamMeetingPoseMixin {
    @Inject(method="updatePlayerPose",at=@At("HEAD"),cancellable=true)
    private void dream$meetingPose(CallbackInfo callback) {
        if ((Object)this instanceof ServerPlayer player) {
            Pose pose = DreamAvatarServer.meetingPose(player);
            if (pose != null) {
                player.setPose(pose);
                callback.cancel();
            }
        }
    }
}
