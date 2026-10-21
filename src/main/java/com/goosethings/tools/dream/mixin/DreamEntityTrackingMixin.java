package com.goosethings.tools.dream.mixin;
import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamTrackedViewer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity") public abstract class DreamEntityTrackingMixin implements DreamTrackedViewer {
 @Shadow @Final private Entity entity;
 @Invoker("removePlayer") public abstract void dream$removePlayer(ServerPlayer player);
 @Inject(method="updatePlayer",at=@At("HEAD"),cancellable=true)
 private void dream$viewer(ServerPlayer player,CallbackInfo callback) {
  // The real owner and its detached viewer share a connection. Removing the owner
  // here also unpairs the viewer, repeatedly despawning nearby moving players.
  // begin/end explicitly transfer the subscription between these two viewers.
  if(DreamAvatarServer.isSeatedOwner(player)){callback.cancel();return;}
  if(entity.getUUID().equals(player.getUUID())){dream$removePlayer(player);callback.cancel();}
 }
}
