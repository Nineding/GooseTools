package com.goosethings.tools.intro;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Items;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dedicated-side class loading and the real datapack toggle/macro call chain. */
public final class BlindDuckServerSmoke implements DedicatedServerModInitializer {
    private boolean done, prepared;
    private int readyTick;
    @Override public void onInitializeServer() {
        if (!Boolean.getBoolean("goosetools.blindDuckServerSmoke")) return;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (done || server.getTickCount() < 10) return;
            if (!prepared) {
                exec(server, "forceload add 0 0");
                server.overworld().getChunk(0, 0);
                readyTick = server.getTickCount() + 20;
                prepared = true; return;
            }
            if (server.getTickCount() < readyTick) return;
            done = true;
            String result;
            try {
                check(com.goosethings.tools.GooseTools.PROTOCOL_VERSION == 50, "protocol");
                for (String id : new String[]{"lobby:invreplace/toggle/blind_duck_mode",
                        "lobby:invreplace/menu_main", "lobby:settings_display/ggd/update",
                        "lobby:settings_display/ggd/apply", "lobby:settings_display/ggd/apply_turn",
                        "lobby:settings_display/ggd/apply_dlc", "lobby:settings_display/ggd/apply_dlc_turn",
                        "mainsys:gamestartprogress/ggd/showevil/do_summon",
                        "mainsys:gamestartprogress/ggd/showevil/do_summon_blind",
                        "mainsys:gamestartprogress/ggd/lover/show_duck_teammates",
                        "ggd:faction_markers/tick", "ggd:faction_markers/render_duck"})
                    check(server.getFunctions().get(Identifier.parse(id)).isPresent(), "function failed to parse: " + id);
                exec(server, "scoreboard objectives add ggdadv dummy");
                exec(server, "scoreboard players set BlindDuckMode ggdadv 0");
                exec(server, "function lobby:invreplace/toggle/blind_duck_mode");
                check(score(server) == 1, "enable toggle");
                exec(server, "function lobby:invreplace/toggle/blind_duck_mode");
                check(score(server) == 0, "disable toggle");
                exec(server, "scoreboard players set BlindDuckMode ggdadv 1");
                exec(server, "team add mannequinDisplay");
                exec(server, "team modify mannequinDisplay nametagVisibility never");
                server.overworld().getChunk(0, 0);
                exec(server, "kill @e[tag=blindDuckDisplay]");
                exec(server, "data modify storage ggd:temp macro_args set value {x:\"0\",y:\"80\",z:\"0\",set:\"p2\",UUID:[I;1,2,3,4]}");
                exec(server, "function mainsys:gamestartprogress/ggd/showevil/do_summon with storage ggd:temp macro_args");
                Mannequin found = null;
                for (var entity : server.overworld().getAllEntities())
                    if (entity.entityTags().contains("blindDuckDisplay")) found = (Mannequin) entity;
                check(found != null, "legacy blind branch spawned no model");
                check(!new java.util.UUID(4294967298L, 12884901892L).equals(found.getProfile().partialProfile().id()), "real profile leaked");
                check(found.getTeam() != null && found.getTeam().getName().equals("mannequinDisplay"), "display name suppression team");
                check(found.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE), "legacy black leather chest missing");
                check(found.getItemBySlot(EquipmentSlot.LEGS).is(Items.LEATHER_LEGGINGS), "legacy black leggings missing");
                check(found.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS), "legacy black boots missing");
                for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    var armor = found.getItemBySlot(slot);
                    check(armor.get(DataComponents.DYED_COLOR).rgb() == 0, "legacy armor is not black");
                    check(armor.get(DataComponents.EQUIPPABLE).assetId().orElseThrow().identifier()
                            .equals(Identifier.withDefaultNamespace("blind_duck")), "legacy question-mark asset missing");
                }
                var head = found.getItemBySlot(EquipmentSlot.HEAD);
                check(head.is(Items.PLAYER_HEAD), "legacy head missing");
                check(head.get(DataComponents.PROFILE).partialProfile().id().equals(found.getUUID()), "legacy head UUID is not unique to its model");
                check(head.get(DataComponents.PROFILE).skinPatch().body().isPresent(), "legacy head's black texture override missing");
                var local = BlindDuckAppearance.equipment(UUID.randomUUID());
                check(local.get(EquipmentSlot.HEAD.ordinal()).get(DataComponents.PROFILE).skinPatch().body().isPresent(), "dedicated-side outfit codec");
                verifyParticles(server);
                result = "PASS dedicated server protocol 50 without client class loading; real setting toggle; legacy black leather/head and unique profile; fixed question-mark asset; zero Duck/Mimic/stand-in particles in blind mode; disabled restores particles";
            } catch (Throwable error) { error.printStackTrace(); result = "FAIL " + error; }
            try { Files.writeString(Path.of(System.getProperty("goosetools.blindDuckServerResult")), result); }
            catch (Exception error) { throw new RuntimeException(error); }
            com.goosethings.tools.GooseTools.LOGGER.info(result); server.halt(false);
        });
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void exec(MinecraftServer server, String text) {
        if (text.startsWith("function mainsys:gamestartprogress/ggd/showevil/")) {
            net.minecraft.commands.Commands.executeCommandInContext(server.createCommandSourceStack(), context -> {
                context.tracer(new net.minecraft.commands.execution.TraceCallbacks() {
                    public void onCommand(int depth, String command) { com.goosethings.tools.GooseTools.LOGGER.info("BLIND TRACE {} {}", depth, command); }
                    public void onReturn(int depth, String command, int result) { com.goosethings.tools.GooseTools.LOGGER.info("BLIND RETURN {} {} -> {}", depth, command, result); }
                    public void onError(String message) { com.goosethings.tools.GooseTools.LOGGER.error("BLIND ERROR {}", message); }
                    public void onCall(int depth, Identifier id, int count) { }
                    public void close() { }
                });
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), text);
            });
            return;
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), text);
    }
    private static int score(MinecraftServer server) {
        return server.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("BlindDuckMode"),
                server.getScoreboard().getObjective("ggdadv")).value();
    }

    private static final class RecordingConnection extends Connection {
        int particles;
        RecordingConnection() { super(PacketFlow.SERVERBOUND); }
        @Override public boolean isConnected() { return true; }
        @Override public boolean isMemoryConnection() { return true; }
        @Override public void flushChannel() { }
        @Override public void send(Packet<?> packet) { if (packet instanceof ClientboundLevelParticlesPacket) particles++; }
        @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener) { send(packet); }
        @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) { send(packet); }
    }
    private static ServerPlayer bot(MinecraftServer server, int seat, String faction, RecordingConnection connection) {
        var profile = new GameProfile(UUID.randomUUID(), "ParticleCheck" + seat);
        var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault()) {
            @Override public boolean hasDisconnected() { return false; }
        };
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setPos(4 + seat, 80, 4); player.addTag("players"); player.addTag("gamingGGD"); player.addTag(faction); player.addTag("p" + seat);
        server.getPlayerList().getPlayers().add(player); server.getPlayerList().getPlayersByUUID().put(player.getUUID(), player);
        server.overworld().addNewPlayer(player); player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }
    private static void verifyParticles(MinecraftServer server) {
        var a = new RecordingConnection(); var b = new RecordingConnection(); var m = new RecordingConnection();
        var duck = bot(server, 1, "evil", a); bot(server, 2, "evil", b); bot(server, 3, "Mimic", m);
        exec(server, "scoreboard players set BlindDuckMode ggdadv 0");
        exec(server, "function ggd:faction_markers/tick");
        check(a.particles >= 2 && b.particles >= 2, "disabled mode does not show Duck and Mimic particles");
        check(m.particles == 0, "Mimic receives Duck particles");
        a.particles = b.particles = m.particles = 0;
        exec(server, "scoreboard players set BlindDuckMode ggdadv 1");
        exec(server, "function ggd:faction_markers/tick");
        for (String state : List.of("sniperScoped", "esperPossessing", "mimeControlling", "parasiteInside")) {
            duck.addTag(state); duck.addTag("parasiteMeetingVisible");
            exec(server, "summon marker 6 80 4 {Tags:[\"sniperDecoy\",\"esperBody\",\"mimeBody\",\"blindParticleStandIn\"]}");
            exec(server, "function ggd:faction_markers/tick");
            exec(server, "execute as @a[tag=p1] at @s run function ggd:faction_markers/render_duck");
            duck.removeTag(state); duck.removeTag("parasiteMeetingVisible");
            exec(server, "kill @e[tag=blindParticleStandIn]");
        }
        check(a.particles + b.particles + m.particles == 0, "blind mode leaked faction particle packets");
        check(!duck.entityTags().contains("ggdFactionMarkerOrigin"), "particle state leaked across ticks");
        exec(server, "scoreboard players set BlindDuckMode ggdadv 0");
        exec(server, "function ggd:faction_markers/tick");
        check(a.particles >= 2 && b.particles >= 2 && m.particles == 0, "turning blind mode off did not restore original particle rules");
    }
}
