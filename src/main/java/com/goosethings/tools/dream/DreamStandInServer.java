package com.goosethings.tools.dream;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server authority for viewer-private dream visuals. The server keeps only
 * immutable snapshots and sends client-only RemotePlayer descriptions; it
 * never creates a visible vanilla mannequin for this system.
 */
public final class DreamStandInServer {
    private static final String DEAD_BODY_TAG = "deadbody";
    private static final String PROFESSIONAL_CORPSE_TAG = "ProfessionalKillCorpse";
    private static final String CORPSE_VISIBLE_SCORE = "DreamCorpseVisible";
    private static final String PROF_CORPSE_VISIBLE_SCORE = "DreamProfCorpseVisible";
    private static final int MAX_PLAYER_SEAT = 20;
    private static final int PREPARE_TIMEOUT_TICKS = 40;
    private static final int RETIRE_TICKS = 40;

    private static final Map<UUID, PlayerSnapshot> SNAPSHOTS = new HashMap<>();
    private static final List<CorpseSnapshot> CORPSES = new ArrayList<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, PlayerSnapshot> PENDING_WAKES = new HashMap<>();
    private static final Map<UUID, RetiringProxy> RETIRING = new HashMap<>();
    private static final Map<UUID, GooseToolsPayloads.DreamSceneS2C> LAST_SCENES =
            new HashMap<>();

    private static long revision;
    private static boolean dirty;

    private DreamStandInServer() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DreamStandInServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_SCENES.remove(handler.player.getUUID());
            dirty = true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            SNAPSHOTS.remove(playerId);
            SESSIONS.remove(playerId);
            PENDING_WAKES.remove(playerId);
            RETIRING.remove(playerId);
            LAST_SCENES.remove(playerId);
            dirty = true;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearState());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("dream")
                .then(Commands.literal("snapshot")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> snapshot(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayers(context, "players")))))
                .then(Commands.literal("prepare")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("lucid")
                                        .executes(context -> prepare(
                                                context.getSource().getServer(),
                                                EntityArgument.getPlayer(context, "player"),
                                                DreamType.LUCID)))
                                .then(Commands.literal("raven")
                                        .executes(context -> prepare(
                                                context.getSource().getServer(),
                                                EntityArgument.getPlayer(context, "player"),
                                                DreamType.RAVEN)))))
                .then(Commands.literal("commit")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> commit(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> cancel(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("prepare-wake")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> prepareWake(
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("commit-wake")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> commitWake(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("cancel-wake")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> cancelWake(
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("abandon")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> abandon(
                                        context.getSource().getServer(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("cleanup")
                        .executes(context -> cleanup(context.getSource().getServer())));
    }

    private static int snapshot(MinecraftServer server, Iterable<ServerPlayer> players) {
        clearState();
        int count = 0;
        for (ServerPlayer player : players) {
            SNAPSHOTS.put(player.getUUID(), PlayerSnapshot.capture(player));
            count++;
        }
        snapshotCorpses(server);
        dirty = true;
        syncAll(server);
        return count;
    }

    private static int prepare(MinecraftServer server, ServerPlayer player, DreamType type) {
        PlayerSnapshot destination = SNAPSHOTS.get(player.getUUID());
        if (destination == null || SESSIONS.containsKey(player.getUUID())
                || !player.entityTags().contains("inTalk")) {
            return 0;
        }
        ProxySnapshot proxy = ProxySnapshot.capture(player);
        Session session = new Session(
                player.getUUID(), type, proxy, server.getTickCount(), false);
        SESSIONS.put(player.getUUID(), session);
        PENDING_WAKES.remove(player.getUUID());
        RETIRING.remove(player.getUUID());
        dirty = true;
        syncAll(server);
        return 1;
    }

    private static int commit(MinecraftServer server, ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return 0;
        }
        session.active = true;
        dirty = true;
        syncAll(server);
        return 1;
    }

    private static int cancel(MinecraftServer server, ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.active) {
            return 0;
        }
        SESSIONS.remove(player.getUUID());
        dirty = true;
        syncAll(server);
        return 1;
    }

    private static int prepareWake(ServerPlayer player) {
        if (!SESSIONS.containsKey(player.getUUID())) {
            return 0;
        }
        PENDING_WAKES.put(player.getUUID(), PlayerSnapshot.capture(player));
        return 1;
    }

    private static int commitWake(MinecraftServer server, ServerPlayer player) {
        PlayerSnapshot wake = PENDING_WAKES.remove(player.getUUID());
        Session session = SESSIONS.remove(player.getUUID());
        if (wake == null || session == null) {
            if (session != null) {
                SESSIONS.put(player.getUUID(), session);
            }
            return 0;
        }
        SNAPSHOTS.put(player.getUUID(), wake);
        RETIRING.put(player.getUUID(), new RetiringProxy(
                session.proxy, server.getTickCount() + RETIRE_TICKS));
        dirty = true;
        syncAll(server);
        return 1;
    }

    private static int cancelWake(ServerPlayer player) {
        return PENDING_WAKES.remove(player.getUUID()) == null ? 0 : 1;
    }

    private static int abandon(MinecraftServer server, ServerPlayer player) {
        PENDING_WAKES.remove(player.getUUID());
        RETIRING.remove(player.getUUID());
        Session removed = SESSIONS.remove(player.getUUID());
        if (removed == null) {
            return 0;
        }
        dirty = true;
        syncAll(server);
        return 1;
    }

    private static int cleanup(MinecraftServer server) {
        int count = SESSIONS.size() + RETIRING.size();
        clearState();
        dirty = true;
        syncAll(server);
        return Math.max(1, count);
    }

    private static void tick(MinecraftServer server) {
        boolean changed = false;
        int tick = server.getTickCount();

        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                SESSIONS.remove(session.playerId);
                changed = true;
                continue;
            }
            if (!session.active && tick - session.preparedAt > PREPARE_TIMEOUT_TICKS
                    && !player.entityTags().contains("inDream")) {
                SESSIONS.remove(session.playerId);
                changed = true;
                continue;
            }
            if (session.active && player.entityTags().contains("dreamRemote")) {
                updateRemoteProxy(server, player, session);
            }
        }

        for (Map.Entry<UUID, RetiringProxy> entry : List.copyOf(RETIRING.entrySet())) {
            if (tick >= entry.getValue().expiresAt) {
                RETIRING.remove(entry.getKey());
                changed = true;
            }
        }

        dirty |= changed;
        if (dirty || tick % 20 == 0) {
            syncAll(server);
        }
    }

    private static void updateRemoteProxy(
            MinecraftServer server, ServerPlayer player, Session session) {
        float swingProgress = player.isSwinging() ? player.getSwingAnimation(1.0F) : -1.0F;
        boolean newSwing = player.isSwinging()
                && (session.lastSwingProgress < 0.0F || swingProgress < session.lastSwingProgress);
        session.lastSwingProgress = swingProgress;
        if (newSwing) {
            session.swingSequence++;
            LivingEntity.SwingDescription swing = player.getCurrentSwing();
            session.offHand = swing != null && swing.hand() == InteractionHand.OFF_HAND;
        }

        ProxySnapshot next = session.proxy.withMotion(player);
        boolean transformChanged = !next.sameMotion(session.proxy);
        if (!transformChanged && !newSwing) {
            return;
        }
        session.proxy = next;
        GooseToolsPayloads.DreamMotionS2C payload = new GooseToolsPayloads.DreamMotionS2C(
                meetingFakeId(player.getUUID()),
                next.yRot, next.xRot, next.bodyRot, next.headRot, next.pose.name(),
                session.swingSequence, session.offHand);
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer.getUUID().equals(player.getUUID())
                    || !MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(
                            viewer, GooseToolsPayloads.DreamMotionS2C.TYPE)) {
                continue;
            }
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void syncAll(MinecraftServer server) {
        long nextRevision = ++revision;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(viewer, GooseToolsPayloads.DreamSceneS2C.TYPE)) {
                continue;
            }
            GooseToolsPayloads.DreamSceneS2C scene = new GooseToolsPayloads.DreamSceneS2C(
                    nextRevision, buildScene(server, viewer));
            GooseToolsPayloads.DreamSceneS2C previous = LAST_SCENES.get(viewer.getUUID());
            if (sameStandIns(previous, scene)) {
                continue;
            }
            ServerPlayNetworking.send(viewer, scene);
            LAST_SCENES.put(viewer.getUUID(), scene);
        }
        Set<UUID> online = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
        LAST_SCENES.keySet().removeIf(id -> !online.contains(id));
        dirty = false;
    }

    private static boolean sameStandIns(GooseToolsPayloads.DreamSceneS2C previous,
                                        GooseToolsPayloads.DreamSceneS2C next) {
        return previous != null && previous.standIns().equals(next.standIns());
    }

    private static List<GooseToolsPayloads.DreamStandIn> buildScene(
            MinecraftServer server, ServerPlayer viewer) {
        List<GooseToolsPayloads.DreamStandIn> result = new ArrayList<>();
        List<Session> sessions = SESSIONS.values().stream()
                .sorted(Comparator.comparing(session -> session.playerId))
                .toList();
        for (Session session : sessions) {
            // The entering player's own client needs the chair proxy before the real body is
            // moved as well. That makes the hand-off complete for F5 and client-side source
            // suppression instead of relying on another viewer to be present.
            result.add(meetingStandIn(session.playerId, session.proxy, false));
        }
        RETIRING.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> !entry.getKey().equals(viewer.getUUID()))
                .forEach(entry -> result.add(meetingStandIn(
                        entry.getKey(), entry.getValue().proxy, true)));

        Session ownSession = SESSIONS.get(viewer.getUUID());
        if (ownSession == null) {
            return List.copyOf(result);
        }

        SNAPSHOTS.values().stream()
                .sorted(Comparator.comparing(snapshot -> snapshot.playerId))
                .filter(snapshot -> !snapshot.playerId.equals(viewer.getUUID()))
                .filter(snapshot -> !SESSIONS.containsKey(snapshot.playerId))
                .map(snapshot -> mapBody(viewer, ownSession.type, snapshot))
                .forEach(result::add);

        if (readFlag(server, CORPSE_VISIBLE_SCORE, true)) {
            boolean showProfessional = readFlag(server, PROF_CORPSE_VISIBLE_SCORE, false);
            CORPSES.stream()
                    .filter(corpse -> !corpse.professional || showProfessional)
                    .sorted(Comparator.comparing(corpse -> corpse.sourceId))
                    .map(corpse -> dreamCorpse(viewer, ownSession.type, corpse))
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private static GooseToolsPayloads.DreamStandIn meetingStandIn(
            UUID playerId, ProxySnapshot proxy, boolean retiring) {
        return standIn(
                meetingFakeId(playerId), playerId, playerId, proxy.playerName,
                proxy.level, GooseToolsPayloads.DreamStandIn.MEETING_PROXY, retiring,
                proxy.position, proxy.yRot, proxy.xRot, proxy.bodyRot, proxy.headRot,
                proxy.pose, proxy.equipment);
    }

    private static GooseToolsPayloads.DreamStandIn mapBody(
            ServerPlayer viewer, DreamType type, PlayerSnapshot snapshot) {
        boolean lucid = type == DreamType.LUCID;
        UUID appearanceId = lucid ? viewer.getUUID() : snapshot.playerId;
        List<ItemStack> equipment = lucid
                ? viewerArmour(snapshot.equipment, viewer) : snapshot.equipment;
        return standIn(
                privateFakeId(viewer.getUUID(), snapshot.playerId, "body"),
                snapshot.playerId, appearanceId, snapshot.playerName,
                snapshot.level, GooseToolsPayloads.DreamStandIn.MAP_BODY, false,
                snapshot.position, snapshot.yRot, snapshot.xRot,
                snapshot.bodyRot, snapshot.headRot, snapshot.pose, equipment);
    }

    private static GooseToolsPayloads.DreamStandIn dreamCorpse(
            ServerPlayer viewer, DreamType type, CorpseSnapshot corpse) {
        boolean lucid = type == DreamType.LUCID;
        UUID appearanceId = lucid ? viewer.getUUID() : corpse.sourceId;
        List<ItemStack> equipment = lucid
                ? viewerArmour(corpse.equipment, viewer) : corpse.equipment;
        return standIn(
                privateFakeId(viewer.getUUID(), corpse.sourceId, "corpse"),
                corpse.sourceId, appearanceId, corpse.playerName,
                corpse.level, GooseToolsPayloads.DreamStandIn.DREAM_CORPSE, false,
                corpse.position, corpse.yRot, corpse.xRot,
                corpse.bodyRot, corpse.headRot, Pose.SLEEPING, equipment);
    }

    private static GooseToolsPayloads.DreamStandIn standIn(
            UUID fakeId, UUID sourceId, UUID appearanceId, String playerName,
            ServerLevel level, int kind, boolean retiring, Vec3 position,
            float yRot, float xRot, float bodyRot, float headRot, Pose pose,
            List<ItemStack> equipment) {
        return new GooseToolsPayloads.DreamStandIn(
                fakeId, sourceId, appearanceId, playerName,
                level.dimension().identifier().toString(), kind, retiring,
                position.x, position.y, position.z,
                yRot, xRot, bodyRot, headRot, pose.name(), copyEquipment(equipment));
    }

    private static UUID meetingFakeId(UUID playerId) {
        return namedId("meeting", playerId, playerId);
    }

    private static UUID privateFakeId(UUID viewerId, UUID sourceId, String kind) {
        return namedId(kind, viewerId, sourceId);
    }

    private static UUID namedId(String kind, UUID first, UUID second) {
        String value = "goosetools:dream:" + kind + ':' + first + ':' + second;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void snapshotCorpses(MinecraftServer server) {
        CORPSES.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mannequin mannequin)
                        || !entity.entityTags().contains(DEAD_BODY_TAG)) {
                    continue;
                }
                GameProfile profile;
                try {
                    profile = mannequin.getProfile().partialProfile();
                } catch (RuntimeException exception) {
                    continue;
                }
                if (profile == null || profile.id() == null) {
                    continue;
                }
                CORPSES.add(new CorpseSnapshot(
                        profile.id(), profile.name(), level, mannequin.position(),
                        mannequin.getYRot(), mannequin.getXRot(), mannequin.yBodyRot,
                        mannequin.getYHeadRot(), captureEquipment(mannequin),
                        mannequin.entityTags().contains(PROFESSIONAL_CORPSE_TAG)));
            }
        }
    }

    private static List<ItemStack> captureEquipment(LivingEntity entity) {
        List<ItemStack> equipment = new ArrayList<>(EquipmentSlot.values().length);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = slot.isArmor() ? entity.getItemBySlot(slot).copy() : ItemStack.EMPTY;
            equipment.add(stack);
        }
        return List.copyOf(equipment);
    }

    private static List<ItemStack> viewerArmour(
            List<ItemStack> source, ServerPlayer viewer) {
        List<ItemStack> equipment = new ArrayList<>(EquipmentSlot.values().length);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = slot.isArmor()
                    ? viewer.getItemBySlot(slot).copy()
                    : source.get(slot.ordinal()).copy();
            equipment.add(stack);
        }
        return List.copyOf(equipment);
    }

    private static List<ItemStack> copyEquipment(List<ItemStack> source) {
        List<ItemStack> result = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            result.add(stack.copy());
        }
        return List.copyOf(result);
    }

    private static boolean readFlag(MinecraftServer server, String holder, boolean fallback) {
        Objective objective = server.getScoreboard().getObjective("ggdadv");
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = server.getScoreboard().getPlayerScoreInfo(
                ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value() == 1;
    }

    private static Pose normalizePose(Pose pose) {
        return switch (pose) {
            case CROUCHING, SWIMMING, FALL_FLYING, SLEEPING, STANDING -> pose;
            case SPIN_ATTACK -> Pose.FALL_FLYING;
            default -> Pose.STANDING;
        };
    }

    private static String seatTag(ServerPlayer player) {
        for (int seat = 1; seat <= MAX_PLAYER_SEAT; seat++) {
            if (player.entityTags().contains("p" + seat)) {
                return "p" + seat;
            }
        }
        return "unknown";
    }

    private static void clearState() {
        SNAPSHOTS.clear();
        CORPSES.clear();
        SESSIONS.clear();
        PENDING_WAKES.clear();
        RETIRING.clear();
        LAST_SCENES.clear();
    }

    private enum DreamType {
        LUCID,
        RAVEN
    }

    private record PlayerSnapshot(
            UUID playerId,
            String playerName,
            ServerLevel level,
            Vec3 position,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            Pose pose,
            List<ItemStack> equipment,
            String seatTag) {
        private static PlayerSnapshot capture(ServerPlayer player) {
            return new PlayerSnapshot(
                    player.getUUID(), player.getGameProfile().name(), player.level(),
                    player.position(), player.getYRot(), player.getXRot(),
                    player.yBodyRot, player.getYHeadRot(), normalizePose(player.getPose()),
                    captureEquipment(player), DreamStandInServer.seatTag(player));
        }
    }

    private record CorpseSnapshot(
            UUID sourceId,
            String playerName,
            ServerLevel level,
            Vec3 position,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            List<ItemStack> equipment,
            boolean professional) {
    }

    private static final class ProxySnapshot {
        private final String playerName;
        private final ServerLevel level;
        private final Vec3 position;
        private final float yRot;
        private final float xRot;
        private final float bodyRot;
        private final float headRot;
        private final Pose pose;
        private final List<ItemStack> equipment;

        private ProxySnapshot(String playerName, ServerLevel level, Vec3 position,
                              float yRot, float xRot, float bodyRot, float headRot,
                              Pose pose, List<ItemStack> equipment) {
            this.playerName = playerName;
            this.level = level;
            this.position = position;
            this.yRot = yRot;
            this.xRot = xRot;
            this.bodyRot = bodyRot;
            this.headRot = headRot;
            this.pose = pose;
            this.equipment = equipment;
        }

        private static ProxySnapshot capture(ServerPlayer player) {
            return new ProxySnapshot(
                    player.getGameProfile().name(), player.level(), player.position(),
                    player.getYRot(), player.getXRot(), player.yBodyRot,
                    player.getYHeadRot(), normalizePose(player.getPose()),
                    captureEquipment(player));
        }

        private ProxySnapshot withMotion(ServerPlayer player) {
            return new ProxySnapshot(
                    playerName, level, position, player.getYRot(), player.getXRot(),
                    player.yBodyRot, player.getYHeadRot(), normalizePose(player.getPose()),
                    equipment);
        }

        private boolean sameMotion(ProxySnapshot other) {
            return Float.compare(yRot, other.yRot) == 0
                    && Float.compare(xRot, other.xRot) == 0
                    && Float.compare(bodyRot, other.bodyRot) == 0
                    && Float.compare(headRot, other.headRot) == 0
                    && pose == other.pose;
        }
    }

    private static final class Session {
        private final UUID playerId;
        private final DreamType type;
        private final int preparedAt;
        private ProxySnapshot proxy;
        private boolean active;
        private float lastSwingProgress = -1.0F;
        private int swingSequence;
        private boolean offHand;

        private Session(UUID playerId, DreamType type, ProxySnapshot proxy,
                        int preparedAt, boolean active) {
            this.playerId = playerId;
            this.type = type;
            this.proxy = proxy;
            this.preparedAt = preparedAt;
            this.active = active;
        }
    }

    private record RetiringProxy(ProxySnapshot proxy, int expiresAt) {
    }
}
