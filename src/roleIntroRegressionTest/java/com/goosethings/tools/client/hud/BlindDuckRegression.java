package com.goosethings.tools.client.hud;

import com.goosethings.tools.intro.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Real server packets, codec and GPU: default/off, blind Ducks, blind Lovers, restored Lovers. */
final class BlindDuckRegression {
    private boolean world, done, requested, captured, saved;
    private int warmup, scenario;
    private long since;
    private CompletableFuture<Void> pending;
    private final List<ServerPlayer> bots = new ArrayList<>();
    void tick(Minecraft mc) {
        if (done || !mc.isGameLoadFinished()) return;
        try {
            if (!world) {
                world = true; mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(2);
                mc.createWorldOpenFlows().createFreshLevel("blind-duck-" + System.currentTimeMillis(),
                        new LevelSettings("Blind Duck verification", GameType.CREATIVE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                        p -> p.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)
                                .value().createWorldDimensions(), null);
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null
                    || !mc.player.connection.hasClientLoaded() || ++warmup < 40) return;
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (scenario == 4) { finish(mc, "PASS real server protocol 50; anonymous UUID/name/equipment sanitization; no anonymous name aliases; unknown wide skin and black head; local black leather outfit and question-mark asset; Duck and Lover GPU rendering; lover retained; disabled restores real appearance; four screenshots"); return; }
            if (!requested) {
                requested = true; captured = false; saved = false; since = System.nanoTime();
                pending = CompletableFuture.runAsync(() -> fixture(mc), mc.getSingleplayerServer()); return;
            }
            require(System.nanoTime() - since < 18_000_000_000L, "opening timed out " + scenario);
            if (!(mc.gui.screen() instanceof RoleIntroScreen screen)) return;
            if (System.nanoTime() - since < 5_900_000_000L) return;
            if (!captured) {
                verify(mc, screen);
                captured = true;
                int shot = scenario;
                Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                    try (image) { image.writeToFile(mc.gameDirectory.toPath().resolve("blind-duck-" + shot + ".png")); saved = true; }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
                return;
            }
            if (saved) {
                RoleIntroScreen.apply(RoleIntroPayload.CLOSE);
                scenario++; requested = false;
            }
        } catch (Throwable error) { error.printStackTrace(); finish(mc, "FAIL " + error); }
    }

    private void fixture(Minecraft mc) {
        try {
            var server = mc.getSingleplayerServer(); var viewer = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (bots.isEmpty()) {
                command(server, viewer, "scoreboard objectives add ggdadv dummy");
                command(server, viewer, "scoreboard objectives add ready dummy");
                for (int seat : new int[]{2, 5, 7}) bots.add(bot(server, seat));
                viewer.addTag("players"); viewer.addTag("evil"); viewer.addTag("Spy"); viewer.addTag("p9");
                viewer.addTag("ggdRoleShowLocked");
                viewer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                command(server, viewer, "scoreboard players set @s ready 1");
                command(server, viewer, "scoreboard players set FullBloodDLC ggdadv 1");
                var root = Path.of(System.getProperty("goosetools.blindDuckPack"));
                var load = Files.readAllLines(root.resolve("datapack/data/mainsys/function/load.mcfunction"));
                var init = load.stream().filter(line -> line.startsWith("execute unless score BlindDuckMode ")).findFirst().orElseThrow();
                command(server, viewer, init); require(score(server) == 0, "missing setting defaults off");
                command(server, viewer, "scoreboard players set BlindDuckMode ggdadv 1");
                command(server, viewer, init); require(score(server) == 1, "reload preserves enabled setting");
                command(server, viewer, "scoreboard players set BlindDuckMode ggdadv 99");
                command(server, viewer, init); require(score(server) == 0, "invalid old value defaults off");
                var summon = Files.readAllLines(root.resolve("datapack/data/mainsys/function/gamestartprogress/ggd/showevil/do_summon_blind.mcfunction"))
                        .stream().filter(line -> line.startsWith("$summon")).findFirst().orElseThrow();
                command(server, viewer, summon.substring(1).replace("$(x)", "0").replace("$(y)", "80").replace("$(z)", "0"));
                var mannequin = java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                        .filter(entity -> entity.entityTags().contains("blindDuckDisplay")).findFirst().orElseThrow();
                require(mannequin instanceof net.minecraft.world.entity.decoration.Mannequin, "legacy anonymous mannequin NBT parses");
                mannequin.discard();
            }
            boolean blind = scenario == 1 || scenario == 2;
            command(server, viewer, "scoreboard players set BlindDuckMode ggdadv " + (blind ? 1 : 0));
            var menu = Files.readAllLines(Path.of(System.getProperty("goosetools.blindDuckPack"))
                    .resolve("datapack/data/lobby/function/invreplace/menu_main.mcfunction"));
            menu.stream().filter(line -> line.startsWith("execute ") && line.contains(" inventory.3 with "))
                    .forEach(line -> command(server, viewer, line));
            require(viewer.getInventory().getItem(12).is(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getValue(Identifier.withDefaultNamespace("light_blue_stained_glass_pane"))), "inventory.3 setting parses");
            require(viewer.getInventory().getItem(12).getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false) == blind, "setting state matches glint");
            if (scenario >= 2) {
                viewer.addTag("Lover"); viewer.addTag("lover_with_2");
                bots.getFirst().addTag("Lover"); bots.getFirst().addTag("lover_with_9");
            }
            command(server, viewer, "goosetools role-intro open @s");
            var visible = RoleIntroServer.appearancePlayers(server, viewer);
            require(visible.size() == (blind ? scenario == 2 ? 2 : 1 : 4), "anonymous allies must have no opening name aliases");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void verify(Minecraft mc, RoleIntroScreen screen) throws Exception {
        var payload = screen.payload(); boolean blind = scenario == 1 || scenario == 2;
        require(payload.roster().size() == 4, "roster count preserved");
        require(payload.theme() == (scenario >= 2 ? RoleIntroPolicy.LOVER : RoleIntroPolicy.DUCK), "stage theme");
        require(payload.roster().stream().filter(RoleIntroPayload.Appearance::anonymous).count() == (blind ? scenario == 2 ? 2 : 3 : 0), "mask count");
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), mc.level.registryAccess());
        try { RoleIntroPayload.CODEC.encode(buffer, payload); var copy = RoleIntroPayload.CODEC.decode(buffer);
            require(copy.roster().stream().filter(RoleIntroPayload.Appearance::anonymous).count() == payload.roster().stream().filter(RoleIntroPayload.Appearance::anonymous).count(), "wire anonymity flag roundtrip");
        } finally { buffer.release(); }
        var field = RoleIntroScreen.class.getDeclaredField("models"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var models = (Map<UUID, net.minecraft.client.player.RemotePlayer>) field.get(screen);
        Set<UUID> headIds = new HashSet<>();
        for (var appearance : payload.roster()) {
            var model = models.get(appearance.id()); require(model != null, "model rendered");
            if (appearance.anonymous()) {
                require(bots.stream().noneMatch(bot -> bot.getUUID().equals(appearance.id())), "real UUID leaked");
                require(appearance.name().equals("Unknown"), "real name leaked");
                require(appearance.equipment().stream().allMatch(ItemStack::isEmpty), "wardrobe leaked");
                require(model.getSkin().body().texturePath().equals(Identifier.withDefaultNamespace("textures/entity/player/slim/unknow.png")), "resource skin not applied");
                require(model.getSkin().cape() == null && model.getSkin().elytra() == null, "cape leaked");
                require(model.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.WIDE, "anonymous arms are still slim");
                for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    var item = model.getItemBySlot(slot);
                    require(item.get(net.minecraft.core.component.DataComponents.DYED_COLOR).rgb() == 0, "anonymous armor is not black");
                    require(item.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).assetId().orElseThrow()
                            .identifier().equals(Identifier.withDefaultNamespace("blind_duck")), "question-mark equipment asset missing");
                }
                var head = model.getItemBySlot(EquipmentSlot.HEAD);
                require(head.is(Items.PLAYER_HEAD), "anonymous head missing");
                var profile = head.get(net.minecraft.core.component.DataComponents.PROFILE);
                require(profile.skinPatch().body().isPresent(), "head's black skin override missing");
                require(headIds.add(profile.partialProfile().id()), "anonymous heads share a UUID");
            } else require(!model.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "known wardrobe lost");
        }
        require(mc.getResourceManager().getResource(Identifier.withDefaultNamespace("textures/entity/player/slim/unknow.png")).isPresent(), "unknown skin missing from resource pack");
        require(mc.getResourceManager().getResource(Identifier.withDefaultNamespace("equipment/blind_duck.json")).isPresent(), "question-mark equipment definition missing");
    }

    private static int score(MinecraftServer server) {
        return server.getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("BlindDuckMode"),
                server.getScoreboard().getObjective("ggdadv")).value();
    }
    private static void command(MinecraftServer server, ServerPlayer player, String text) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), text);
    }
    private static ServerPlayer bot(MinecraftServer server, int seat) {
        var profile = new GameProfile(UUID.randomUUID(), "KnownDuck" + seat);
        var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault()) {
            @Override public boolean hasDisconnected() { return false; }
        };
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public boolean isConnected() { return true; }
            @Override public boolean isMemoryConnection() { return true; }
            @Override public void flushChannel() { }
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener) { }
            @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) { }
        };
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false));
        player.setPos(3 + seat, 80, 3); player.addTag("players"); player.addTag("p" + seat);
        player.addTag(seat == 5 ? "good" : "evil"); if (seat == 5) player.addTag("Mimic");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        server.getPlayerList().getPlayers().add(player); server.getPlayerList().getPlayersByUUID().put(player.getUUID(), player);
        server.overworld().addNewPlayer(player); player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        command(server, player, "scoreboard players set @s ready 1"); return player;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void finish(Minecraft mc, String result) {
        done = true; try { Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result); } catch (Exception ignored) { }
        com.goosethings.tools.GooseTools.LOGGER.info(result); mc.stop();
    }
}
