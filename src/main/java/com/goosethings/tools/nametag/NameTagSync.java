package com.goosethings.tools.nametag;

import com.goosethings.tools.marker.PlayerMarkerPolicy;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.goosethings.tools.network.MandatoryHandshake;
import com.goosethings.tools.player.RoomOrderManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;

/** Builds privacy-filtered nametag state separately for every observing player. */
public final class NameTagSync {
    public static final int GRAVY = 1;
    public static final int CLOWN_BALLOON_ONE = 1 << 1;
    public static final int CLOWN_BALLOON_TWO = 1 << 2;
    public static final int PIGEON_INFECTED = 1 << 3;
    public static final int LOVER = 1 << 4;
    public static final int GUARD_SHIELD = 1 << 5;
    public static final int WITCH_DOCTOR_CURSE = 1 << 6;
    private static final String SETTINGS_OBJECTIVE = "ggdadv";
    private static final Map<UUID, GooseToolsPayloads.NameTagSnapshotS2C> LAST_SENT = new HashMap<>();
    private static final Map<UUID, Integer> LAST_COLOUR = new HashMap<>();
    private static final Map<UUID, VisualIdentity> DISGUISE_IDENTITIES = new HashMap<>();
    private static final NameTagAttachmentRepository ATTACHMENTS = new NameTagAttachmentRepository(
            FabricLoader.getInstance().getConfigDir().resolve("goosetools/nametag_attachments.json"));

    private NameTagSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NameTagSync::tick);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                reloadAttachments();
            } catch (IOException | RuntimeException exception) {
                com.goosethings.tools.GooseTools.LOGGER.error(
                        "Cannot load nametag_attachments.json; retaining the last valid snapshot", exception);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                LAST_SENT.remove(handler.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST_SENT.remove(handler.player.getUUID());
            DISGUISE_IDENTITIES.remove(handler.player.getUUID());
            NameTagVisibilityOverrides.removePlayer(handler.player.getUUID());
            NameTagIconAssignments.removePlayer(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_SENT.clear();
            LAST_COLOUR.clear();
            DISGUISE_IDENTITIES.clear();
            NameTagVisibilityOverrides.clear();
            NameTagIconAssignments.clear();
        });
    }

    public static int hide(Collection<ServerPlayer> viewers, Collection<ServerPlayer> targets) {
        return NameTagVisibilityOverrides.hide(playerIds(viewers), playerIds(targets));
    }

    public static int show(Collection<ServerPlayer> viewers, Collection<ServerPlayer> targets) {
        return NameTagVisibilityOverrides.show(playerIds(viewers), playerIds(targets));
    }

    public static int clearHidden(Collection<ServerPlayer> viewers) {
        return NameTagVisibilityOverrides.clearViewers(playerIds(viewers));
    }

    public static int setIcon(Collection<ServerPlayer> targets,
                              String slot,
                              String texture,
                              float width,
                              float height,
                              int rgb,
                              int order) {
        int changed = NameTagIconAssignments.set(
                playerIds(targets), slot, texture, width, height, rgb, order);
        if (changed > 0) {
            LAST_SENT.clear();
        }
        return changed;
    }

    public static int removeIcon(Collection<ServerPlayer> targets, String slot) {
        int changed = NameTagIconAssignments.remove(playerIds(targets), slot);
        if (changed > 0) {
            LAST_SENT.clear();
        }
        return changed;
    }

    public static int clearIcons(Collection<ServerPlayer> targets) {
        int changed = NameTagIconAssignments.clear(playerIds(targets));
        if (changed > 0) {
            LAST_SENT.clear();
        }
        return changed;
    }

    public static int reloadAttachments() throws IOException {
        NameTagAttachmentConfig config = ATTACHMENTS.reload();
        LAST_SENT.clear();
        return config.attachments().size();
    }

    private static void tick(MinecraftServer server) {
        if ((server.getTickCount() & 1) != 0) {
            return;
        }
        boolean fullBlood = readScore(server, SETTINGS_OBJECTIVE, "FullBloodDLC", 0) == 1;
        boolean roleVisible = readScore(server, SETTINGS_OBJECTIVE, "DLCRoleVisible", 0) == 1;
        NameTagAttachmentConfig attachmentConfig = ATTACHMENTS.current();
        List<ServerPlayer> onlinePlayers = List.copyOf(server.getPlayerList().getPlayers());
        Map<Integer, ServerPlayer> playersBySeat = new HashMap<>();
        for (ServerPlayer player : onlinePlayers) {
            int seat = DisguiseIdentityPolicy.playerSeat(player.entityTags());
            if (seat > 0) {
                playersBySeat.putIfAbsent(seat, player);
            }
        }
        int activeLoverCount = (int) onlinePlayers.stream()
                .filter(player -> LoverVisualPolicy.isActiveLover(player.entityTags()))
                .count();
        List<ServerPlayer> targets = onlinePlayers.stream()
                .filter(player -> RoomOrderManager.orderOf(server, player) > 0)
                .sorted(Comparator.comparingInt(player -> RoomOrderManager.orderOf(server, player)))
                .toList();
        Map<UUID, VisualIdentity> identities = new HashMap<>();
        for (ServerPlayer target : onlinePlayers) {
            identities.put(target.getUUID(), identityOf(server, target, playersBySeat, fullBlood));
        }
        List<GooseToolsPayloads.NameTagAlias> aliases = dreamAliases(server, playersBySeat);
        for (ServerPlayer viewer : onlinePlayers) {
            if (!MandatoryHandshake.isVerified(viewer)
                    || !ServerPlayNetworking.canSend(viewer, GooseToolsPayloads.NameTagSnapshotS2C.TYPE)) {
                continue;
            }
            boolean spectatorStatusView = NameTagSpectatorStatusPolicy.revealAll(
                    fullBlood, roleVisible, viewer.entityTags());
            List<ServerPlayer> viewerTargets = targets;
            boolean alreadyOrdered = targets.contains(viewer);
            if (NameTagTargetPolicy.includeUnorderedSelf(
                    alreadyOrdered, viewer.isSpectator(), viewer.entityTags())) {
                viewerTargets = new ArrayList<>(targets);
                viewerTargets.add(viewer);
            }
            List<GooseToolsPayloads.NameTagEntry> entries = new ArrayList<>(viewerTargets.size());
            for (ServerPlayer target : viewerTargets) {
                // Keep the real body as the entry/visibility owner, but mirror the
                // viewer's effective identity and complete label with its skin.
                ServerPlayer labelPlayer = fullBlood && AppearanceMirrorBridge.isMirrored(
                        viewer.getUUID(), target.getUUID()) ? viewer : target;
                VisualIdentity identity = identities.get(labelPlayer.getUUID());
                int markerCode = markerCode(server, viewer, labelPlayer, fullBlood);
                boolean markerNameTagVisible = markerCode != 0
                        && PlayerMarkerPolicy.showBesideNameTag(labelPlayer.entityTags());
                boolean samePlayer = viewer.getUUID().equals(target.getUUID());
                if (NameTagVisibilityOverrides.isHidden(
                        viewer.getUUID(), target.getUUID())
                        || NameTagVisibilityPolicy.concealIdentity(
                        samePlayer,
                        viewer.entityTags(), identity.tags(), target.entityTags())) {
                    // Keep a managed entry so the client still suppresses the
                    // vanilla nametag. An empty name is the protocol-compatible
                    // sentinel for drawing no custom plate or attachments.
                    entries.add(new GooseToolsPayloads.NameTagEntry(
                            target.getUUID(), identity.playerId(), target.getScoreboardName(),
                            "", 0, 0, 0, markerCode, false, List.of()));
                    continue;
                }
                int flags = fullBlood ? attachmentFlags(
                        viewer, labelPlayer, identity, activeLoverCount, spectatorStatusView) : 0;
                if (NameTagAttachmentPolicy.showGuardShield(
                        viewer == labelPlayer, viewer.entityTags(), labelPlayer.entityTags(), spectatorStatusView)) {
                    flags |= GUARD_SHIELD;
                }
                entries.add(new GooseToolsPayloads.NameTagEntry(
                        target.getUUID(),
                        identity.playerId(),
                        target.getScoreboardName(),
                        identity.name(),
                        identity.rgb(),
                        identity.serialNumber(),
                        flags,
                        markerCode,
                        markerNameTagVisible,
                        dataDrivenAttachments(
                                viewer, labelPlayer, identity, fullBlood,
                                spectatorStatusView, attachmentConfig)));
            }
            GooseToolsPayloads.NameTagSnapshotS2C snapshot =
                    new GooseToolsPayloads.NameTagSnapshotS2C(entries, aliases);
            if (snapshot.equals(LAST_SENT.get(viewer.getUUID()))) {
                continue;
            }
            ServerPlayNetworking.send(viewer, snapshot);
            LAST_SENT.put(viewer.getUUID(), snapshot);
        }
    }

    private static int markerCode(MinecraftServer server,
                                  ServerPlayer viewer,
                                  ServerPlayer target,
                                  boolean fullBlood) {
        if (!fullBlood || !PlayerMarkerPolicy.viewerCanSeeMarkers(viewer.entityTags())) {
            return 0;
        }
        int seat = DisguiseIdentityPolicy.playerSeat(target.entityTags());
        if (seat <= 0) {
            seat = RoomOrderManager.orderOf(server, target);
        }
        String objective = PlayerMarkerPolicy.objectiveForSeat(seat);
        if (objective.isEmpty()) {
            return 0;
        }
        return PlayerMarkerPolicy.validCodeOrZero(
                readScore(server, objective, viewer.getScoreboardName(), 0));
    }

    private static List<UUID> playerIds(Collection<ServerPlayer> players) {
        return players.stream().map(ServerPlayer::getUUID).toList();
    }

    private static List<GooseToolsPayloads.NameTagAlias> dreamAliases(
            MinecraftServer server,
            Map<Integer, ServerPlayer> playersBySeat) {
        List<GooseToolsPayloads.NameTagAlias> aliases = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mannequin)
                        || !entity.entityTags().contains("ggdDreamEntity")) {
                    continue;
                }
                int sourceSeat = PlayerMarkerPolicy.dreamSourceSeat(entity.entityTags());
                ServerPlayer source = playersBySeat.get(sourceSeat);
                if (source == null) {
                    continue;
                }
                aliases.add(new GooseToolsPayloads.NameTagAlias(
                        entity.getUUID(), source.getUUID()));
                if (aliases.size() == GooseToolsPayloads.NameTagSnapshotS2C.MAX_ALIASES) {
                    return List.copyOf(aliases);
                }
            }
        }
        return List.copyOf(aliases);
    }

    private static List<GooseToolsPayloads.NameTagIcon> dataDrivenAttachments(
            ServerPlayer viewer,
            ServerPlayer renderedPlayer,
            VisualIdentity identity,
            boolean fullBlood,
            boolean spectatorStatusView,
            NameTagAttachmentConfig config) {
        List<GooseToolsPayloads.NameTagIcon> result = new ArrayList<>();
        result.addAll(NameTagIconAssignments.iconsForIdentity(
                renderedPlayer.getUUID(), identity.playerId()));
        if (result.size() == GooseToolsPayloads.NameTagSnapshotS2C.MAX_ATTACHMENTS_PER_ENTRY) {
            return List.copyOf(result);
        }
        Set<String> viewerTags = viewer.entityTags();
        Set<String> renderedTags = renderedPlayer.entityTags();
        boolean samePlayer = viewer.getUUID().equals(renderedPlayer.getUUID());
        for (NameTagAttachmentConfig.Attachment attachment : config.attachments()) {
            if (!attachment.visible(
                    samePlayer, viewerTags, identity.tags(), renderedTags,
                    fullBlood, spectatorStatusView)) {
                continue;
            }
            result.add(new GooseToolsPayloads.NameTagIcon(
                    attachment.texture(), attachment.width(), attachment.height(), attachment.rgb()));
            if (result.size() == GooseToolsPayloads.NameTagSnapshotS2C.MAX_ATTACHMENTS_PER_ENTRY) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private static VisualIdentity identityOf(MinecraftServer server,
                                             ServerPlayer renderedPlayer,
                                             Map<Integer, ServerPlayer> playersBySeat,
                                             boolean fullBlood) {
        Set<String> renderedTags = renderedPlayer.entityTags();
        if (!DisguiseIdentityPolicy.isActiveDisguise(renderedTags)) {
            DISGUISE_IDENTITIES.remove(renderedPlayer.getUUID());
            return snapshotOf(server, renderedPlayer, fullBlood);
        }
        int stolenSeat = DisguiseIdentityPolicy.stolenSeat(renderedTags);
        ServerPlayer source = playersBySeat.get(stolenSeat);
        if (source != null && source != renderedPlayer) {
            VisualIdentity identity = snapshotOf(server, source, fullBlood);
            DISGUISE_IDENTITIES.put(renderedPlayer.getUUID(), identity);
            return identity;
        }
        VisualIdentity cached = DISGUISE_IDENTITIES.get(renderedPlayer.getUUID());
        return cached != null ? cached : snapshotOf(server, renderedPlayer, fullBlood);
    }

    private static VisualIdentity snapshotOf(MinecraftServer server,
                                             ServerPlayer player,
                                             boolean fullBlood) {
        return new VisualIdentity(
                player.getUUID(),
                player.getGameProfile().name(),
                colourOf(server, player, fullBlood),
                RoomOrderManager.orderOf(server, player),
                Set.copyOf(player.entityTags()),
                DisguiseIdentityPolicy.playerSeat(player.entityTags()),
                readScore(server, "ggdGravyBounty", player.getScoreboardName(), 0),
                readScore(server, "heliumLevel", player.getScoreboardName(), 0));
    }

    private static int attachmentFlags(ServerPlayer viewer,
                                       ServerPlayer renderedPlayer,
                                       VisualIdentity identity,
                                       int activeLoverCount,
                                       boolean spectatorStatusView) {
        Set<String> viewerTags = viewer.entityTags();
        Set<String> renderedTags = renderedPlayer.entityTags();
        Set<String> identityTags = identity.tags();
        int flags = 0;
        if (NameTagAttachmentPolicy.showGravyBounty(
                viewerTags, identityTags, renderedTags,
                identity.gravyBounty(), spectatorStatusView)) {
            flags |= GRAVY;
        }
        int clownBalloonLevel = NameTagAttachmentPolicy.clownBalloonLevel(
                viewerTags, identityTags, renderedTags,
                identity.heliumLevel(), spectatorStatusView);
        if (clownBalloonLevel == 1) {
            flags |= CLOWN_BALLOON_ONE;
        } else if (clownBalloonLevel >= 2) {
            flags |= CLOWN_BALLOON_TWO;
        }
        if (NameTagAttachmentPolicy.showPigeonInfected(
                viewer.getUUID().equals(renderedPlayer.getUUID()),
                viewerTags,
                identityTags,
                renderedTags,
                spectatorStatusView)) {
            flags |= PIGEON_INFECTED;
        }
        if (LoverVisualPolicy.show(
                viewer.getUUID().equals(renderedPlayer.getUUID()),
                viewer.getUUID().equals(identity.playerId()),
                viewerTags,
                renderedTags,
                identityTags,
                DisguiseIdentityPolicy.playerSeat(viewerTags),
                identity.seat(),
                activeLoverCount,
                spectatorStatusView)) {
            flags |= LOVER;
        }
        if (NameTagAttachmentPolicy.showWitchDoctorCurse(
                spectatorStatusView, renderedTags)) {
            flags |= WITCH_DOCTOR_CURSE;
        }
        return flags;
    }

    private record VisualIdentity(
            UUID playerId,
            String name,
            int rgb,
            int serialNumber,
            Set<String> tags,
            int seat,
            int gravyBounty,
            int heliumLevel) {
    }

    private static int colourOf(MinecraftServer server, ServerPlayer player, boolean fullBlood) {
        if (fullBlood) {
            int outfit = readScore(server, "ggdYuik", player.getScoreboardName(), 0);
            int custom = readScore(server, "ggdYuikColor", player.getScoreboardName(), 0);
            Integer wardrobe = WardrobeColourResolver.resolve(outfit, custom);
            if (wardrobe != null) {
                LAST_COLOUR.put(player.getUUID(), wardrobe);
                return wardrobe;
            }
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Integer resolved = resolveColour(chest);
        if (resolved != null) {
            LAST_COLOUR.put(player.getUUID(), resolved);
            return resolved;
        }
        return LAST_COLOUR.getOrDefault(player.getUUID(), 0xffffff);
    }

    static Integer resolveColour(ItemStack chest) {
        DyedItemColor dyed = chest.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            return dyed.rgb() & 0x00ffffff;
        }
        if (chest.is(Items.COPPER_CHESTPLATE)) return 0xc87847;
        if (chest.is(Items.IRON_CHESTPLATE)) return 0xd8d8d8;
        if (chest.is(Items.GOLDEN_CHESTPLATE)) return 0xfcee4b;
        if (chest.is(Items.DIAMOND_CHESTPLATE)) return 0x4aedd9;
        if (chest.is(Items.NETHERITE_CHESTPLATE)) return 0x443a3b;
        return null;
    }

    private static int readScore(MinecraftServer server, String objectiveName,
                                 String holder, int fallback) {
        Objective objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            return fallback;
        }
        ReadOnlyScoreInfo score = server.getScoreboard()
                .getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return score == null ? fallback : score.value();
    }
}
