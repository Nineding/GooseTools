package com.goosethings.tools.regions;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Read-only world-data adapter. A saved world continues to work without the editor installed. */
public final class RegionDataServer {
    private static RegionDataServer current;
    private final MinecraftServer server;private String json=new RegionCatalog(1,List.of(),Set.of()).json();
    private String token="",failed="";private Object sourceIdentity;
    private RegionDataServer(MinecraftServer server){this.server=server;refresh();}
    public static void register(){
        RegionPackets.register();
        ServerLifecycleEvents.SERVER_STARTED.register(s->current=new RegionDataServer(s));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->current=null);
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{if(current!=null)current.send(h.player);});
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(current==null)return;
            Object source=FabricLoader.getInstance().getObjectShare().get("mapregioneditor:server_catalog");
            if(source instanceof Supplier<?> supplier){Object value=supplier.get();if(value instanceof String next&&!next.isEmpty()&&value!=current.sourceIdentity){current.sourceIdentity=value;current.accept(next,"editor:"+next.hashCode());}}
            else if(s.getTickCount()%20==0)current.refresh();
        });
        // Keep the optional datapack entry parseable when the editor is not installed.
        if(!FabricLoader.getInstance().isModLoaded("mapregioneditor"))CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(Commands.literal("ggd-region")
            .requires(s->s.getEntity() instanceof ServerPlayer p&&p.entityTags().contains("Debugger"))
            .executes(c->{c.getSource().sendFailure(Component.translatableWithFallback("region.mapregioneditor.missing","Install Map Region Editor on the client and server to edit world regions."));return 0;})));
    }
    private void refresh(){
        Path folder=server.getWorldPath(LevelResource.ROOT).resolve("data"),file=folder.resolve("map_regions.json");
        if(!Files.exists(file))file=folder.resolve("goosetools_regions.json");
        try{
            if(!Files.exists(file)){if(!token.equals("empty")){token="empty";accept(new RegionCatalog(1,List.of(),Set.of()).json(),token);}return;}
            String next=file+":"+Files.getLastModifiedTime(file)+":"+Files.size(file);if(next.equals(token))return;token=next;
            if(Files.size(file)>RegionCatalog.MAX_JSON*3L)throw new IllegalArgumentException("Region file exceeds size limit");
            accept(Files.readString(file),next);
        }catch(Exception error){if(!failed.equals(token)){failed=token;GooseTools.LOGGER.warn("Could not read saved region data; retaining the last valid snapshot",error);}}
    }
    private void accept(String text,String key){
        if(text.equals(json))return;
        try{String next=RegionCatalog.parse(text).json();if(!next.equals(json)){json=next;server.getPlayerList().getPlayers().forEach(this::send);}failed="";}
        catch(RuntimeException error){if(!failed.equals(key)){failed=key;GooseTools.LOGGER.warn("Rejected saved region snapshot; retaining the last valid snapshot",error);}}
    }
    private void send(ServerPlayer player){if(ServerPlayNetworking.canSend(player,RegionPackets.Catalog.TYPE))ServerPlayNetworking.send(player,new RegionPackets.Catalog(json));}
}
