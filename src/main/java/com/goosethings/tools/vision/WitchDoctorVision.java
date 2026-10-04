package com.goosethings.tools.vision;

import com.goosethings.tools.camera.CameraService;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

/** Server-authoritative curse sight checks, including active security-camera feeds. */
public final class WitchDoctorVision {
    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final double UNLIMITED_WITCH_DOCTOR_RANGE = 128.0D;
    private static final double UNLIMITED_BIRDWATCHER_RANGE = 64.0D;
    private static final double WITCH_DOCTOR_RANGE_MULTIPLIER = 2.0D;
    private static final double BIRDWATCHER_RANGE_MULTIPLIER = 2.0D;
    private static final double CONE_COSINE = Math.cos(Math.toRadians(45.0D));

    private WitchDoctorVision() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("witchdoctor")
                .then(Commands.literal("cansee")
                        .then(Commands.argument("viewer", EntityArgument.player())
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(WitchDoctorVision::executeCanSee))))
                .then(Commands.literal("target")
                        .then(Commands.argument("viewer", EntityArgument.player())
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("highlighted", BoolArgumentType.bool())
                                                .executes(WitchDoctorVision::executeTarget)))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("viewer", EntityArgument.player())
                                .executes(WitchDoctorVision::executeClear)));
    }

    private static int executeTarget(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer viewer = EntityArgument.getPlayer(context, "viewer");
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        if (!viewer.entityTags().contains("WitchDoctor")
                || !ServerPlayNetworking.canSend(viewer, GooseToolsPayloads.WitchDoctorTargetS2C.TYPE)) {
            return 0;
        }
        ServerPlayNetworking.send(viewer, new GooseToolsPayloads.WitchDoctorTargetS2C(
                true, target.getUUID().toString(), BoolArgumentType.getBool(context, "highlighted")));
        return 1;
    }

    private static int executeClear(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer viewer = EntityArgument.getPlayer(context, "viewer");
        if (ServerPlayNetworking.canSend(viewer, GooseToolsPayloads.WitchDoctorTargetS2C.TYPE)) {
            ServerPlayNetworking.send(viewer, new GooseToolsPayloads.WitchDoctorTargetS2C(false, "", false));
        }
        return 1;
    }

    private static int executeCanSee(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer viewer = EntityArgument.getPlayer(context, "viewer");
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        if (!viewer.entityTags().contains("WitchDoctor")
                || viewer.entityTags().contains("spectator")
                || viewer.entityTags().contains("inTalk")
                || viewer.entityTags().contains("endGame")
                || target.entityTags().contains("spectator")) {
            return 0;
        }
        boolean birdwatch = viewer.entityTags().contains("birdwatcherActive");
        boolean direct = canSeeDirectly(context.getSource().getServer(), viewer, target);
        boolean camera = CameraService.get().isWatchingTarget(viewer, target);
        return canSeeCode(direct, birdwatch, viewer.hasLineOfSight(target), camera);
    }

    /**
     * 0 = hidden, 1 = ordinary sight or camera, 2 = birdwatch through a blocking wall.
     * Curse progress treats any positive value as visible.
     */
    static int canSeeCode(boolean direct, boolean birdwatch, boolean vanillaLos, boolean camera) {
        if (direct) {
            return birdwatch && !vanillaLos ? 2 : 1;
        }
        return camera ? 1 : 0;
    }

    static boolean canSeeDirectly(MinecraftServer server, ServerPlayer viewer, ServerPlayer target) {
        if (viewer.level() != target.level()) {
            return false;
        }
        double range = directRange(server, viewer);
        if (viewer.entityTags().contains("birdwatcherActive")) {
            return BirdwatcherSightLine.canSee(viewer, target, range);
        }
        Vec3 delta = target.getEyePosition().subtract(viewer.getEyePosition());
        if (delta.lengthSqr() > range * range || !insideHorizontalCone(delta, viewer.getLookAngle())) {
            return false;
        }
        return viewer.hasLineOfSight(target);
    }

    static boolean insideHorizontalCone(Vec3 delta, Vec3 look) {
        double deltaLength = Math.hypot(delta.x, delta.z);
        double lookLength = Math.hypot(look.x, look.z);
        if (deltaLength <= 1.0E-6D || lookLength <= 1.0E-6D) {
            return true;
        }
        return (delta.x * look.x + delta.z * look.z) / (deltaLength * lookLength) >= CONE_COSINE;
    }

    private static double directRange(MinecraftServer server, ServerPlayer viewer) {
        boolean limitedVision = readSetting(server, "FullBloodDLC", 0) == 1
                && readSetting(server, "DLCVisionFog", 0) == 1;
        return sightRange(
                limitedVision,
                readSetting(server, "DLCVisionRange", VisionSync.DEFAULT_CLEAR_RADIUS),
                viewer.entityTags().contains("birdwatcherActive"));
    }

    static double sightRange(boolean limitedVision, int configuredRange, boolean birdwatcherActive) {
        if (!limitedVision) {
            return birdwatcherActive ? UNLIMITED_BIRDWATCHER_RANGE : UNLIMITED_WITCH_DOCTOR_RANGE;
        }
        double witchDoctorRange = Math.clamp(configuredRange,
                VisionSync.MIN_CLEAR_RADIUS,
                VisionSync.MAX_CLEAR_RADIUS) * WITCH_DOCTOR_RANGE_MULTIPLIER;
        return birdwatcherActive
                ? witchDoctorRange * BIRDWATCHER_RANGE_MULTIPLIER
                : witchDoctorRange;
    }

    private static int readSetting(MinecraftServer server, String holder, int fallback) {
        Objective objective = server.getScoreboard().getObjective(SETTINGS_OBJECTIVE);
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = server.getScoreboard()
                .getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value();
    }
}
