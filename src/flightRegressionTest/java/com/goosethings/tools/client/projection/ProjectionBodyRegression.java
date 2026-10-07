package com.goosethings.tools.client.projection;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.projection.ProjectionBodyServer;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.TeamColor;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Runs the actual client entity lifecycle and renderer in an isolated flat save. */
public final class ProjectionBodyRegression implements ClientModInitializer {
    private boolean opened;
    private boolean finished;
    private int ticks;
    private int scenario;
    private long revision = 1_000_000L;
    private RemotePlayer cameraTarget;
    private RemotePlayer remoteSource;
    private GooseToolsPayloads.ProjectionBody state;
    private Entity originalBody;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("goosetools.projectionRegressionTest")) return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (finished || !mc.isGameLoadFinished()) return;
        try {
            if (!opened) {
                opened = true;
                mc.options.pauseOnLostFocus = false;
                Files.deleteIfExists(mc.gameDirectory.toPath().resolve("projection-result.txt"));
                mc.createWorldOpenFlows().createFreshLevel("projection-" + System.currentTimeMillis(),
                        new LevelSettings("Projection regression", GameType.ADVENTURE,
                                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                                true, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42L, false, false),
                        provider -> provider.lookupOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.gui.screen());
                return;
            }
            if (mc.level == null || mc.player == null
                    || !mc.player.connection.hasClientLoaded() || mc.player.tickCount < 20) return;
            if (mc.gui.screen() != null) mc.setScreenAndShow(null);
            if (ticks == 0) setup(mc);
            if (ticks == 5) {
                originalBody = verifyBody(mc);
                // Test camera hand-off with a real non-local entity. A LocalPlayer
                // body would be skipped by LevelExtractor in exactly this state.
                require(mc.getCameraEntity() != mc.player, "possession camera did not switch");
                require(!(originalBody instanceof net.minecraft.client.player.LocalPlayer),
                        "owner body still relies on the skipped LocalPlayer");
            }
            if (ticks == 8) {
                mc.level.removeEntity(originalBody.getId(), Entity.RemovalReason.DISCARDED);
            }
            if (ticks == 6) {
                verifyHighlights(mc);
            }
            if (ticks == 11) {
                require(verifyBody(mc) != originalBody, "unloaded clone was not rebuilt");
                applyScene(List.of(withPhase(GooseToolsPayloads.ProjectionBody.RETURNING)));
                Entity source = source(mc);
                require(!ProjectionBodyClient.shouldSuppressSource(source), "return authority stayed hidden");
                require(ProjectionBodyClient.shouldSuppressBody(findBody(mc)),
                        "return did not remove overlapping non-spectator copy");
            }
            if (ticks == 14) {
                applyScene(List.of());
                require(findBody(mc) == null, "body survived cleanup");
                require(!ProjectionBodyClient.shouldSuppressSource(source(mc)), "source gate survived cleanup");
                mc.setCameraEntity(mc.player);
                mc.level.removeEntity(cameraTarget.getId(), Entity.RemovalReason.DISCARDED);
                if (remoteSource != null) mc.level.removeEntity(remoteSource.getId(), Entity.RemovalReason.DISCARDED);
                GooseTools.LOGGER.info("Projection regression passed {} {}",
                        scenario < 2 ? "owner" : "observer", state.pose());
                if (++scenario == 4) {
                    finish(mc, "PASS owner and simulated remote observer: standing/crouching, camera hand-off, "
                            + "overlapping Adventure authority, full render state, equipment, unload recovery, return, cleanup, "
                            + "Sensor/Stalker outline colours and removal on all four projection kinds");
                    return;
                }
                ticks = 0;
                return;
            }
            ticks++;
        } catch (Throwable error) {
            GooseTools.LOGGER.error("Projection regression failed", error);
            finish(mc, "FAIL " + error);
        }
    }

    private void setup(Minecraft mc) throws Exception {
        Vec3 position = mc.player.position();
        remoteSource = scenario < 2 ? null : new RemotePlayer(mc.level,
                new GameProfile(UUID.randomUUID(), "EsperObserverSource"));
        if (remoteSource != null) {
            remoteSource.setId(-1_800_000_000 - scenario);
            remoteSource.absSnapTo(position.x, position.y, position.z, 0, 0);
            mc.level.addEntity(remoteSource);
        }
        cameraTarget = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "PossessionCamera"));
        cameraTarget.setId(-1_700_000_000 - scenario);
        cameraTarget.absSnapTo(position.x + 4, position.y, position.z, 90, 0);
        mc.level.addEntity(cameraTarget);
        mc.setCameraEntity(cameraTarget);
        Entity source = source(mc);
        List<ItemStack> equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(slot == EquipmentSlot.CHEST ? new ItemStack(Items.DIAMOND_CHESTPLATE) : ItemStack.EMPTY);
        }
        Pose pose = scenario % 2 == 0 ? Pose.STANDING : Pose.CROUCHING;
        state = new GooseToolsPayloads.ProjectionBody(source.getUUID(),
                ProjectionBodyServer.fakeBodyId(source.getUUID()), source.getScoreboardName(),
                mc.level.dimension().identifier().toString(), GooseToolsPayloads.ProjectionBody.ESPER,
                GooseToolsPayloads.ProjectionBody.ACTIVE, false,
                position.x, position.y, position.z, 0, 0, 0, 0, pose.name(), equipment);
        applyScene(List.of(state));
    }

    private Entity source(Minecraft mc) {
        return remoteSource == null ? mc.player : remoteSource;
    }

    private Entity findBody(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(state.fakeBodyId())) return entity;
        }
        return null;
    }

    private Entity verifyBody(Minecraft mc) {
        Entity body = findBody(mc);
        require(body != null, "dedicated body is absent");
        require(body.getPose().name().equals(state.pose()), "entry pose was lost");
        require(body.position().distanceToSqr(new Vec3(state.x(), state.y(), state.z())) < 1E-6,
                "body left its entry position");
        require(!ProjectionBodyClient.shouldSuppressBody(body), "active body was hidden by overlapping authority");
        require(ProjectionBodyClient.shouldSuppressSource(source(mc)), "duplicate authority was not hidden");
        AvatarRenderState renderState = (AvatarRenderState) mc.getEntityRenderDispatcher().extractEntity(body, 1.0F);
        require(!renderState.isInvisible && !renderState.isInvisibleToPlayer && !renderState.isSpectator,
                "body inherited invisible/spectator rendering");
        require(renderState.isCrouching == state.pose().equals("CROUCHING"), "crouching render pose was lost");
        require(renderState.skin != null, "body skin is absent");
        require(((net.minecraft.world.entity.LivingEntity) body).getItemBySlot(EquipmentSlot.CHEST)
                .is(Items.DIAMOND_CHESTPLATE), "snapshot equipment was lost");
        return body;
    }

    private GooseToolsPayloads.ProjectionBody withPhase(int phase) {
        return new GooseToolsPayloads.ProjectionBody(state.sourcePlayerId(), state.fakeBodyId(),
                state.sourceName(), state.dimension(), state.kind(), phase, false,
                state.x(), state.y(), state.z(), state.yRot(), state.xRot(), state.bodyRot(),
                state.headRot(), state.pose(), state.equipment());
    }

    private void verifyHighlights(Minecraft mc) throws Exception {
        var scoreboard = mc.level.getScoreboard();
        var team = scoreboard.addPlayerTeam("gt_g_regression");
        GooseToolsPayloads.ProjectionBody original = state;
        for (int kind : new int[]{GooseToolsPayloads.ProjectionBody.SNIPER,
                GooseToolsPayloads.ProjectionBody.ASTRAL,
                GooseToolsPayloads.ProjectionBody.ESPER,
                GooseToolsPayloads.ProjectionBody.MIME}) {
            for (boolean pin : remoteSource == null ? new boolean[]{false} : new boolean[]{false, true}) {
                if (pin && mc.level.getPlayerByUUID(original.sourcePlayerId()) == null) {
                    // Moving Mime deliberately discards the retained authority.
                    // A later pinned scenario needs a fresh network-player spawn.
                    int entityId = remoteSource.getId();
                    remoteSource = new RemotePlayer(mc.level,
                            new GameProfile(original.sourcePlayerId(), original.sourceName()));
                    remoteSource.setId(entityId);
                    remoteSource.absSnapTo(original.x(), original.y(), original.z(), 0, 0);
                    mc.level.addEntity(remoteSource);
                }
                state = new GooseToolsPayloads.ProjectionBody(original.sourcePlayerId(), original.fakeBodyId(),
                        original.sourceName(), original.dimension(), kind, original.phase(), pin,
                        original.x(), original.y(), original.z(), original.yRot(), original.xRot(),
                        original.bodyRot(), original.headRot(), original.pose(), original.equipment());
                applyScene(List.of(state));
                // Rendering must survive hidden-authority metadata without a glow bit.
                Entity body = pin ? source(mc) : findBody(mc);
                require(body != null, "highlight body is absent");
                for (TeamColor color : new TeamColor[]{TeamColor.AQUA, TeamColor.LIGHT_PURPLE}) {
                    team.setColor(java.util.Optional.of(color));
                    scoreboard.addPlayerToTeam(original.sourceName(), team);
                    body.setGlowingTag(false);
                    body.setInvisible(true);
                    AvatarRenderState rendered = (AvatarRenderState)
                            mc.getEntityRenderDispatcher().extractEntity(body, 1.0F);
                    require(rendered.outlineColor == (0xFF000000 | color.rgb()),
                            "projection outline missing/wrong colour: " + kind + " pin=" + pin);
                    scoreboard.removePlayerFromTeam(original.sourceName(), team);
                    body.setGlowingTag(false);
                    rendered = (AvatarRenderState) mc.getEntityRenderDispatcher().extractEntity(body, 1.0F);
                    require(rendered.outlineColor == 0, "projection outline survived clear");
                    body.setInvisible(false);
                }
            }
        }
        scoreboard.removePlayerTeam(team);
        state = original;
        applyScene(List.of(state));
        originalBody = findBody(mc);
    }

    private void applyScene(List<GooseToolsPayloads.ProjectionBody> bodies) throws Exception {
        // Invoke the registered receiver's handler on the actual render thread.
        Method apply = ProjectionBodyClient.class.getDeclaredMethod("applyScene", GooseToolsPayloads.ProjectionBodiesS2C.class);
        apply.setAccessible(true);
        apply.invoke(null, new GooseToolsPayloads.ProjectionBodiesS2C(++revision, bodies));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("projection-result.txt"), result);
        } catch (Exception error) {
            GooseTools.LOGGER.error("Could not write projection result", error);
        }
        mc.stop();
    }
}
