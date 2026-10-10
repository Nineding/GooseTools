package com.goosethings.tools.dream;
import net.minecraft.server.level.ServerPlayer;
public interface DreamChunkTracking {void dream$updateChunkTracking(ServerPlayer player); void dream$addViewer(ServerPlayer player, boolean added); void dream$removeViewer(ServerPlayer player);}
