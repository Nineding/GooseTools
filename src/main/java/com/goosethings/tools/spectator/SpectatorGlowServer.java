package com.goosethings.tools.spectator;

import com.goosethings.tools.network.MandatoryHandshake;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Reuses a load index; never scans the world per player or changes shared glow flags/teams. */
public final class SpectatorGlowServer {
    private static final Map<UUID,Entity> CANDIDATES = new HashMap<>();
    private static final Map<UUID,SpectatorGlowPayload> SENT = new HashMap<>();
    private SpectatorGlowServer() {}
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((e,l) -> {
            if (!(e instanceof net.minecraft.world.entity.player.Player)
                    && e instanceof LivingEntity) CANDIDATES.put(e.getUUID(),e);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((e,l) -> CANDIDATES.remove(e.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { CANDIDATES.clear(); SENT.clear(); });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> SENT.remove(h.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(SpectatorGlowServer::tick);
    }
    private static boolean flag(MinecraftServer s, String key) {
        var objective=s.getScoreboard().getObjective("ggdadv");
        var value=objective==null?null:s.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(key),objective);
        return value!=null && value.value()==1;
    }
    public static SpectatorGlowPayload snapshot(MinecraftServer server, ServerPlayer viewer) {
        if (!SpectatorGlowPalette.observer(viewer.entityTags())) return SpectatorGlowPayload.EMPTY;
        Map<UUID,Integer> colors=new HashMap<>(); Set<UUID> living=new HashSet<>(), corpses=new HashSet<>();
        if (flag(server,"FullBloodDLC")) {
            boolean byFaction=flag(server,"DLCGlowByFaction");
            // Owned storage records preserve colours of disconnected meeting snapshots.
            var roles=server.getCommandStorage().get(net.minecraft.resources.Identifier.parse("ggd:role"));
            for(int seat=1;seat<=24;seat++) {
                var role=roles.getCompoundOrEmpty("p"+seat);
                if(!role.contains("UUID")) continue;
                var owner=net.minecraft.core.UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,role.get("UUID")).result();
                if(owner.isEmpty()) continue;
                int color=SpectatorGlowPalette.UNKNOWN;
                if(byFaction) color=switch(role.getStringOr("faction","")) {
                    case "good" -> 0x55FF55; case "evil" -> 0xFF5555;
                    case "neutral", "bird" -> SpectatorGlowPalette.role(role.getStringOr("role",""),0xFFFF55);
                    default -> SpectatorGlowPalette.UNKNOWN;
                };
                colors.put(owner.get(),color);
            }
            for(var player:server.getPlayerList().getPlayers()) {
                var tags=player.entityTags(); if(!tags.contains("players") || tags.contains("endGame")) continue;
                if(tags.contains("good") || tags.contains("evil") || tags.contains("neutral") || !colors.containsKey(player.getUUID()))
                    colors.put(player.getUUID(),SpectatorGlowPalette.player(tags,byFaction));
                if(!tags.contains("spectator") && !tags.contains("deadInMap")) living.add(player.getUUID());
            }
        }
        for(var e:CANDIDATES.values()) {
            if(!e.isRemoved() && e.level()==viewer.level() && SpectatorGlowPalette.corpse(e.entityTags())) {
                corpses.add(e.getUUID()); if(corpses.size()==SpectatorGlowPayload.LIMIT) break;
            }
        }
        return new SpectatorGlowPayload(colors,living,corpses);
    }
    private static void tick(MinecraftServer server) {
        if(server.getTickCount()%5!=0) return;
        for(var viewer:server.getPlayerList().getPlayers()) {
            if(!MandatoryHandshake.isVerified(viewer) || !ServerPlayNetworking.canSend(viewer,SpectatorGlowPayload.TYPE)) continue;
            if(!SpectatorGlowPalette.observer(viewer.entityTags()) && !SENT.containsKey(viewer.getUUID())) continue;
            var next=snapshot(server,viewer);
            if(!next.equals(SENT.getOrDefault(viewer.getUUID(),SpectatorGlowPayload.EMPTY))) ServerPlayNetworking.send(viewer,next);
            if(next.equals(SpectatorGlowPayload.EMPTY)) SENT.remove(viewer.getUUID()); else SENT.put(viewer.getUUID(),next);
        }
    }
}
