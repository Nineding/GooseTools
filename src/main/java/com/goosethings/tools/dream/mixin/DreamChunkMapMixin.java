package com.goosethings.tools.dream.mixin;
import com.goosethings.tools.dream.DreamAvatarServer;
import com.goosethings.tools.dream.DreamChunkTracking;
import com.goosethings.tools.dream.DreamTrackedViewer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkMap.class) public abstract class DreamChunkMapMixin implements DreamChunkTracking {
 @Shadow @Final private Int2ObjectMap<?> entityMap;
 @Invoker("updateChunkTracking") public abstract void dream$updateChunkTracking(ServerPlayer player);
 @Invoker("updatePlayerStatus") public abstract void dream$addViewer(ServerPlayer player, boolean added);
 public void dream$removeViewer(ServerPlayer player) {
  dream$addViewer(player, false);
  for(Object tracked:entityMap.values())((DreamTrackedViewer)tracked).dream$removePlayer(player);
 }
 @Inject(method={"move","updateChunkTracking"},at=@At("HEAD"),cancellable=true)
 private void dream$suspend(ServerPlayer player,CallbackInfo callback) {
  if(DreamAvatarServer.isSeatedOwner(player))callback.cancel();
 }
}
