package com.goosethings.tools.client.noclip;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.movement.AdventureNoClipService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Real integrated server, payloads, vanilla input and movement; opens only a new test save. */
public final class FlightRegression implements ClientModInitializer {
    private static final float[] SPEEDS = {0.5F, 0.4F, 2.0F, 1.0F};
    private boolean opened;
    private boolean finished;
    private int scenario;
    private int ticks;
    private int beforeEntityTick;
    private Vec3 horizontalStart;
    private double verticalStart;
    private double hoverStart;
    private CompletableFuture<Void> cancellationCheck;
    private boolean respawnPhase;
    private boolean requestedRespawn;
    private int respawnTicks;
    private LocalPlayer oldPlayer;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("goosetools.flightRegressionTest")) return;
        ClientTickEvents.START_CLIENT_TICK.register(this::beforeTick);
        ClientTickEvents.END_CLIENT_TICK.register(this::afterTick);
    }

    private void beforeTick(Minecraft mc) {
        if (mc.player != null) beforeEntityTick = mc.player.tickCount;
        if (finished || respawnPhase || mc.player == null || ticks < 16 || ticks > 85) return;
        mc.options.keyUp.setDown(ticks < 36);
        mc.options.keyJump.setDown((ticks >= 36 && ticks < 51) || ticks == 66 || ticks == 68);
        mc.options.keyShift.setDown(ticks >= 51 && ticks < 66);
        // Reproduce a stale grounded bit repeatedly, including while already airborne.
        mc.player.setOnGround(true);
    }

    private void afterTick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("flight-" + System.currentTimeMillis(),
                        new LevelSettings("Flight regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42L, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
            if (respawnPhase) {
                checkRespawn(mc);
                return;
            }
            // ReceivingLevelScreen completes the loaded handshake; do not dismiss it early.
            if (!mc.player.connection.hasClientLoaded()) return;
            if (mc.gui.screen() != null) mc.setScreenAndShow(null);
            // Client ticks run while chunks are still loading, before LocalPlayer can tick.
            if (ticks > 0 && mc.player.tickCount == beforeEntityTick) return;
            if (ticks == 0) {
                setup(mc, SPEEDS[scenario]);
            }
            if (ticks == 15) {
                horizontalStart = mc.player.position();
            }
            if (ticks >= 16 && ticks <= 85) {
                require(mc.player.getAbilities().flying, "flight canceled at tick " + ticks);
                require(!mc.player.onGround(), "stale ground contact at tick " + ticks
                        + " entityTick=" + mc.player.tickCount + " paused=" + mc.isPaused());
                require(mc.player.noPhysics, "no-clip missing");
                require(Math.abs(mc.player.getAbilities().getFlyingSpeed() - 0.05F * SPEEDS[scenario]) < 1E-6,
                        "wrong configured speed");
            }
            if (ticks == 35) {
                require(mc.player.position().subtract(horizontalStart).horizontalDistanceSqr() > 0.01,
                        "forward input did not move player");
                require(Math.abs(mc.player.getY() - horizontalStart.y) < 0.1, "fell while moving horizontally");
                verticalStart = mc.player.getY();
            }
            if (ticks == 50) {
                require(mc.player.getY() > verticalStart + 0.1, "jump did not ascend");
                verticalStart = mc.player.getY();
            }
            if (ticks == 65) {
                require(mc.player.getY() < verticalStart - 0.1, "shift did not descend");
            }
            if (ticks == 74) hoverStart = mc.player.getY();
            if (ticks == 70) {
                var server = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                // Invoke the real packet handler on its own thread and inspect immediately,
                // before the periodic FlyManager tick could hide a failed guard.
                cancellationCheck = server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    player.connection.handlePlayerAbilities(new ServerboundPlayerAbilitiesPacket(new Abilities()));
                    require(player.getAbilities().flying, "server accepted forced-flight cancellation");
                });
            }
            if (ticks == 85) {
                require(cancellationCheck.isDone(), "server cancellation check timed out");
                cancellationCheck.join();
                require(Math.abs(mc.player.getY() - hoverStart) < 0.1, "hover drift after double-tap jump");
                releaseKeys(mc);
                // Switch from forced flight to ordinary ghost permission, using the same command path.
                serverCommand(mc, "gt fly @s true " + SPEEDS[scenario] + " false", false);
            }
            if (ticks == 97) {
                mc.player.getAbilities().flying = false;
                mc.player.onUpdateAbilities();
            }
            if (ticks >= 99 && ticks < 106) {
                require(mc.player.getAbilities().mayfly, "ghost flight permission lost");
                require(!mc.player.getAbilities().flying, "ghost flight forcibly re-enabled");
            }
            if (ticks == 106) serverCommand(mc, "gt fly @s false", false);
            if (ticks == 120) {
                require(!mc.player.getAbilities().mayfly && !mc.player.getAbilities().flying,
                        "flight did not clean up");
                require(!mc.player.noPhysics, "no-clip did not clean up");
                require(Math.abs(mc.player.getAbilities().getFlyingSpeed() - 0.05F) < 1E-6,
                        "vanilla speed did not restore");
                GooseTools.LOGGER.info("Flight regression passed speed {}", SPEEDS[scenario]);
                if (++scenario == SPEEDS.length) {
                    respawnPhase = true;
                    oldPlayer = mc.player;
                    setup(mc, 0.5F);
                    return;
                }
                ticks = -1;
            }
            ticks++;
        } catch (Throwable failure) {
            GooseTools.LOGGER.error("Flight regression failed", failure);
            finish(mc, "FAIL scenario=" + scenario + " tick=" + ticks + " " + failure);
        }
    }

    private void checkRespawn(Minecraft mc) {
        if (!requestedRespawn) {
            if (++respawnTicks == 15) {
                require(mc.player.getAbilities().flying, "respawn test flight not active");
                serverCommand(mc, "kill @s", true);
            }
            if (mc.player.isDeadOrDying()) {
                mc.player.respawn();
                requestedRespawn = true;
                respawnTicks = 0;
            }
            require(respawnTicks < 100, "kill/respawn did not start");
            return;
        }
        if (mc.player == oldPlayer || !mc.player.connection.hasClientLoaded()) return;
        if (mc.gui.screen() != null) mc.setScreenAndShow(null);
        if (++respawnTicks < 15) return;
        require(!mc.player.getAbilities().mayfly && !mc.player.getAbilities().flying,
                "respawn retained role flight");
        require(!mc.player.noPhysics, "respawn retained no-clip");
        finish(mc, "PASS four role speeds: grounded start, horizontal hover, ascent/descent, double-tap lock, server cancellation guard, ghost toggle, cleanup, death/respawn");
    }

    private void setup(Minecraft mc, float speed) {
        releaseKeys(mc);
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayer(id);
            player.setGameMode(GameType.ADVENTURE);
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    player.level().setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                }
            }
            player.teleportTo(player.level(), 0.5, 101, 0.5, Set.of(), 0, 0, false);
            player.setDeltaMovement(Vec3.ZERO);
            player.setOnGround(true);
            AdventureNoClipService.setEnabled(player, true);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    "execute as " + player.getScoreboardName() + " run gt fly @s true " + speed + " true");
        });
    }

    private void serverCommand(Minecraft mc, String command, boolean noClip) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            var player = server.getPlayerList().getPlayer(id);
            AdventureNoClipService.setEnabled(player, noClip);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    "execute as " + player.getScoreboardName() + " run " + command);
        });
    }

    private static void releaseKeys(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        releaseKeys(mc);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result);
        } catch (Exception error) {
            GooseTools.LOGGER.error("Could not write flight regression result", error);
        }
        mc.stop();
    }
}
