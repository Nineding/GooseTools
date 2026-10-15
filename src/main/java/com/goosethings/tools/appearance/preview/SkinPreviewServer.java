package com.goosethings.tools.appearance.preview;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.MandatoryHandshake;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Optional website integration; disabled unless the server operator configures a snapshot directory. */
public final class SkinPreviewServer {
    private static final SkinPreviewTransfer TRANSFER=new SkinPreviewTransfer();
    private static final Set<UUID> REQUESTED=new HashSet<>();
    private static volatile Path directory;
    private SkinPreviewServer() {}
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path file=FabricLoader.getInstance().getConfigDir().resolve("goosetools/skin-preview.json");
            try {
                if(!Files.isRegularFile(file))return;
                var config=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if(!config.has("enabled")||!config.get("enabled").getAsBoolean())return;
                String path=config.get("directory").getAsString();
                if(path.isBlank())throw new IllegalArgumentException("A skin snapshot directory is required");
                Path selected=Path.of(path).toAbsolutePath().normalize();Files.createDirectories(selected);directory=selected;
                GooseTools.LOGGER.info("Website skin preview snapshots are enabled");
            } catch(Exception error) {GooseTools.LOGGER.warn("Website skin preview is disabled: {}",error.getMessage());}
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(directory==null||server.getTickCount()%20!=0)return;
            for(var player:server.getPlayerList().getPlayers()) {
                if(MandatoryHandshake.isVerified(player)&&!REQUESTED.contains(player.getUUID())&&ServerPlayNetworking.canSend(player,SkinPreviewPayloads.Request.TYPE)) {
                    ServerPlayNetworking.send(player,new SkinPreviewPayloads.Request());REQUESTED.add(player.getUUID());
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(SkinPreviewPayloads.Chunk.TYPE,(packet,context)->context.server().execute(()->{
            Path target=directory;
            if(target==null||!MandatoryHandshake.isVerified(context.player())||!REQUESTED.contains(context.player().getUUID()))return;
            TRANSFER.accept(context.player().getUUID(),packet.upload(),packet.total(),packet.index(),packet.count(),packet.slim(),packet.hash(),packet.bytes(),System.currentTimeMillis()).ifPresent(snapshot ->
                CompletableFuture.runAsync(()->{
                    try{SkinPreviewStore.save(target,snapshot,System.currentTimeMillis());}
                    catch(Exception error){GooseTools.LOGGER.debug("Skin preview snapshot was not saved: {}",error.getMessage());}
                },Util.nonCriticalIoPool()));
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{REQUESTED.remove(handler.player.getUUID());TRANSFER.clear(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{directory=null;REQUESTED.clear();TRANSFER.clear();});
    }
}
