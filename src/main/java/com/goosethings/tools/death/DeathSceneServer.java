package com.goosethings.tools.death;

import com.goosethings.tools.intro.PresentationAppearance;
import com.goosethings.tools.intro.RoleIntroPayload.Appearance;
import com.goosethings.tools.network.MandatoryHandshake;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

public final class DeathSceneServer {
    private record Pending(DeathSceneKind kind, int expires) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Set<UUID> PLAYED = new HashSet<>();
    private static long sequence;
    private DeathSceneServer() {}
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeathSceneServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> { PENDING.remove(h.player.getUUID()); PLAYED.remove(h.player.getUUID()); });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { PENDING.clear(); PLAYED.clear(); });
    }
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        var root = Commands.literal("death-scene");
        for (String action : List.of("prepare", "preview")) {
            var targets = Commands.argument("players", EntityArgument.players());
            for (var kind : DeathSceneKind.values()) targets.then(Commands.literal(kind.id()).executes(c -> {
                var players = EntityArgument.getPlayers(c, "players"); int sent = 0;
                for (var player : players) {
                    if (action.equals("preview")) { if (send(c.getSource().getServer(), player, kind)) sent++; }
                    else {
                        var old = PENDING.get(player.getUUID());
                        var selected = DeathSceneKind.prefer(old != null && old.expires() > c.getSource().getServer().getTickCount() ? old.kind() : null, kind);
                        PENDING.put(player.getUUID(), new Pending(selected, c.getSource().getServer().getTickCount() + 100)); sent++;
                    }
                }
                return sent;
            }));
            root.then(Commands.literal(action).then(targets));
        }
        root.then(Commands.literal("play").then(Commands.argument("players", EntityArgument.players()).executes(c -> {
            int count = 0;
            for (var p : EntityArgument.getPlayers(c, "players")) {
                var pending = PENDING.remove(p.getUUID());
                if (p.entityTags().contains("inTutorial") || !p.entityTags().contains("gamingGGD") || !dead(p) || !PLAYED.add(p.getUUID())) continue;
                var kind = pending != null && pending.expires() > c.getSource().getServer().getTickCount() ? pending.kind() : DeathSceneKind.UNKNOWN;
                if (p.entityTags().contains("loverChainVictim")) kind = DeathSceneKind.LOVER;
                if (p.entityTags().contains("carrierPairChainVictim")) kind = DeathSceneKind.PARASITE;
                if (send(c.getSource().getServer(), p, kind)) count++;
            }
            return count;
        })));
        root.then(Commands.literal("clear").then(Commands.argument("players", EntityArgument.players()).executes(c -> {
            for (var p : EntityArgument.getPlayers(c, "players")) {
                PENDING.remove(p.getUUID()); PLAYED.remove(p.getUUID());
                if (ServerPlayNetworking.canSend(p, DeathScenePayload.TYPE)) ServerPlayNetworking.send(p, new DeathScenePayload(++sequence, DeathSceneKind.UNKNOWN, null));
            }
            return 1;
        })));
        return root;
    }
    private static boolean dead(ServerPlayer p) { return p.entityTags().contains("spectator") || p.entityTags().contains("deadInMap"); }
    private static boolean send(MinecraftServer s, ServerPlayer p, DeathSceneKind kind) {
        var objective = s.getScoreboard().getObjective("ggdadv");
        var score = objective == null ? null : s.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly("FullBloodDLC"), objective);
        if (score == null || score.value() != 1 || !MandatoryHandshake.isVerified(p) || !ServerPlayNetworking.canSend(p, DeathScenePayload.TYPE)) return false;
        String seat = p.entityTags().stream().filter(t -> t.matches("p(?:[1-9]|1[0-9]|2[01])")).findFirst().orElse("deathNoSeat");
        var appearance = new Appearance(p.getUUID(), p.getGameProfile().name(), false, PresentationAppearance.equipment(s, p, seat));
        ServerPlayNetworking.send(p, new DeathScenePayload(++sequence, kind, appearance));
        return true;
    }
    private static void tick(MinecraftServer s) {
        PENDING.entrySet().removeIf(e -> e.getValue().expires() <= s.getTickCount());
        for (var p : s.getPlayerList().getPlayers()) {
            if (!dead(p)) PLAYED.remove(p.getUUID()); // Phoenix rebirth permits a new, real death.
            if (!p.entityTags().contains("gamingGGD") || p.entityTags().contains("endGame")) PENDING.remove(p.getUUID());
        }
    }
}
