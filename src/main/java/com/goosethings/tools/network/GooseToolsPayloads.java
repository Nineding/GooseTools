package com.goosethings.tools.network;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** All play-stage payloads used by GooseTools. */
public final class GooseToolsPayloads {
    public static final int MAX_PAGE_ID_LENGTH = 96;
    public static final int MAX_HASH_LENGTH = 64;
    public static final int MAX_CHUNK_BYTES = 24 * 1024;
    public static final int AI_DEBUG_MAX_JSON_BYTES = 4 * 1024 * 1024;
    public static final int AI_DEBUG_MAX_COMPRESSED_BYTES = 1024 * 1024;

    private GooseToolsPayloads() {
    }

    public static void registerTypes() {
        PayloadTypeRegistry.clientboundPlay().register(HelloS2C.TYPE, HelloS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HelloC2S.TYPE, HelloC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WebBundleStartS2C.TYPE, WebBundleStartS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WebBundleChunkS2C.TYPE, WebBundleChunkS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WebOpenS2C.TYPE, WebOpenS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WebCloseS2C.TYPE, WebCloseS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(WebRequestC2S.TYPE, WebRequestC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BroadcastHudS2C.TYPE, BroadcastHudS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MeetingAlertS2C.TYPE, MeetingAlertS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisionStateS2C.TYPE, VisionStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlackoutAssistStateS2C.TYPE, BlackoutAssistStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GamePresenceStateS2C.TYPE, GamePresenceStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenClientSettingsS2C.TYPE, OpenClientSettingsS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BirdwatcherStateS2C.TYPE, BirdwatcherStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WitchDoctorTargetS2C.TYPE, WitchDoctorTargetS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NameTagSnapshotS2C.TYPE, NameTagSnapshotS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DreamSceneS2C.TYPE, DreamSceneS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DreamMotionS2C.TYPE, DreamMotionS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ProjectionBodiesS2C.TYPE, ProjectionBodiesS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ProjectionBodyMotionS2C.TYPE, ProjectionBodyMotionS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TaskMarkersS2C.TYPE, TaskMarkersS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiReportStartS2C.TYPE, AiReportStartS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiReportChunkS2C.TYPE, AiReportChunkS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiReportClearS2C.TYPE, AiReportClearS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiProgressS2C.TYPE, AiProgressS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiNameVoteOpenS2C.TYPE, AiNameVoteOpenS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiNameVoteCloseS2C.TYPE, AiNameVoteCloseS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AiNameVoteC2S.TYPE, AiNameVoteC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiDebugStartS2C.TYPE, AiDebugStartS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AiDebugChunkS2C.TYPE, AiDebugChunkS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AimClaimC2S.TYPE, AimClaimC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MimeControlS2C.TYPE, MimeControlS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MimeControllerViewS2C.TYPE, MimeControllerViewS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AdventureNoClipS2C.TYPE, AdventureNoClipS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ForcedFlightS2C.TYPE, ForcedFlightS2C.CODEC);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, path));
    }

    /** A zero multiplier releases the lock; positive values authorize active flight. */
    public record ForcedFlightS2C(float speedMultiplier) implements CustomPacketPayload {
        public static final Type<ForcedFlightS2C> TYPE = GooseToolsPayloads.type("forced_flight_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, ForcedFlightS2C> CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, ForcedFlightS2C::speedMultiplier,
                        ForcedFlightS2C::new);

        public ForcedFlightS2C {
            if (!Float.isFinite(speedMultiplier)
                    || (speedMultiplier != 0.0F && (speedMultiplier < 0.1F || speedMultiplier > 10.0F))) {
                throw new IllegalArgumentException("Invalid forced flight multiplier");
            }
        }

        @Override
        public Type<ForcedFlightS2C> type() {
            return TYPE;
        }
    }

    public record AdventureNoClipS2C(boolean enabled) implements CustomPacketPayload {
        public static final Type<AdventureNoClipS2C> TYPE =
                GooseToolsPayloads.type("adventure_noclip_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AdventureNoClipS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, AdventureNoClipS2C::enabled,
                        AdventureNoClipS2C::new);

        @Override
        public Type<AdventureNoClipS2C> type() {
            return TYPE;
        }
    }

    public record MimeControlS2C(boolean active, UUID controllerId)
            implements CustomPacketPayload {
        public static final Type<MimeControlS2C> TYPE =
                GooseToolsPayloads.type("mime_control_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, MimeControlS2C> CODEC =
                StreamCodec.of(MimeControlS2C::write, MimeControlS2C::read);

        public MimeControlS2C {
            if (controllerId == null) {
                throw new IllegalArgumentException("Mime controller UUID is required");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, MimeControlS2C payload) {
            buffer.writeBoolean(payload.active());
            buffer.writeUUID(payload.controllerId());
        }

        private static MimeControlS2C read(RegistryFriendlyByteBuf buffer) {
            return new MimeControlS2C(buffer.readBoolean(), buffer.readUUID());
        }

        @Override
        public Type<MimeControlS2C> type() {
            return TYPE;
        }
    }

    /** Private controller-only view: hide the target locally and wear the target's skin. */
    public record MimeControllerViewS2C(boolean active, UUID targetId, String targetName)
            implements CustomPacketPayload {
        public static final Type<MimeControllerViewS2C> TYPE =
                GooseToolsPayloads.type("mime_controller_view_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, MimeControllerViewS2C> CODEC =
                StreamCodec.of(MimeControllerViewS2C::write, MimeControllerViewS2C::read);

        public MimeControllerViewS2C {
            if (targetId == null) {
                throw new IllegalArgumentException("Mime controller-view target UUID is required");
            }
            targetName = targetName == null ? "" : targetName;
            if (targetName.length() > 64) {
                throw new IllegalArgumentException("Mime controller-view target name is too long");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, MimeControllerViewS2C payload) {
            buffer.writeBoolean(payload.active());
            buffer.writeUUID(payload.targetId());
            buffer.writeUtf(payload.targetName(), 64);
        }

        private static MimeControllerViewS2C read(RegistryFriendlyByteBuf buffer) {
            return new MimeControllerViewS2C(
                    buffer.readBoolean(), buffer.readUUID(), buffer.readUtf(64));
        }

        @Override
        public Type<MimeControllerViewS2C> type() {
            return TYPE;
        }
    }

    public record HelloS2C(int protocol, String version) implements CustomPacketPayload {
        public static final Type<HelloS2C> TYPE = GooseToolsPayloads.type("hello_s2c_v6");
        public static final StreamCodec<RegistryFriendlyByteBuf, HelloS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HelloS2C::protocol,
                ByteBufCodecs.stringUtf8(32), HelloS2C::version,
                HelloS2C::new);

        @Override
        public Type<HelloS2C> type() {
            return TYPE;
        }
    }

    public record HelloC2S(int protocol, String version, String soundPhysicsVersion) implements CustomPacketPayload {
        public static final Type<HelloC2S> TYPE = GooseToolsPayloads.type("hello_c2s_v6");
        public static final StreamCodec<RegistryFriendlyByteBuf, HelloC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HelloC2S::protocol,
                ByteBufCodecs.stringUtf8(32), HelloC2S::version,
                ByteBufCodecs.stringUtf8(64), HelloC2S::soundPhysicsVersion,
                HelloC2S::new);

        @Override
        public Type<HelloC2S> type() {
            return TYPE;
        }
    }

    public record WebBundleStartS2C(
            long transferId,
            String hash,
            int compressedSize,
            int chunkCount,
            String pageId) implements CustomPacketPayload {
        public static final Type<WebBundleStartS2C> TYPE = GooseToolsPayloads.type("web_bundle_start_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, WebBundleStartS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, WebBundleStartS2C::transferId,
                ByteBufCodecs.stringUtf8(MAX_HASH_LENGTH), WebBundleStartS2C::hash,
                ByteBufCodecs.VAR_INT, WebBundleStartS2C::compressedSize,
                ByteBufCodecs.VAR_INT, WebBundleStartS2C::chunkCount,
                ByteBufCodecs.stringUtf8(MAX_PAGE_ID_LENGTH), WebBundleStartS2C::pageId,
                WebBundleStartS2C::new);

        @Override
        public Type<WebBundleStartS2C> type() {
            return TYPE;
        }
    }

    public record WebBundleChunkS2C(long transferId, int index, byte[] bytes)
            implements CustomPacketPayload {
        public static final Type<WebBundleChunkS2C> TYPE = GooseToolsPayloads.type("web_bundle_chunk_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, WebBundleChunkS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, WebBundleChunkS2C::transferId,
                ByteBufCodecs.VAR_INT, WebBundleChunkS2C::index,
                ByteBufCodecs.byteArray(MAX_CHUNK_BYTES), WebBundleChunkS2C::bytes,
                WebBundleChunkS2C::new);

        @Override
        public Type<WebBundleChunkS2C> type() {
            return TYPE;
        }
    }

    public record WebOpenS2C(String hash, String pageId) implements CustomPacketPayload {
        public static final Type<WebOpenS2C> TYPE = GooseToolsPayloads.type("web_open_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, WebOpenS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_HASH_LENGTH), WebOpenS2C::hash,
                ByteBufCodecs.stringUtf8(MAX_PAGE_ID_LENGTH), WebOpenS2C::pageId,
                WebOpenS2C::new);

        @Override
        public Type<WebOpenS2C> type() {
            return TYPE;
        }
    }

    public record WebCloseS2C() implements CustomPacketPayload {
        public static final WebCloseS2C INSTANCE = new WebCloseS2C();
        public static final Type<WebCloseS2C> TYPE = GooseToolsPayloads.type("web_close_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, WebCloseS2C> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<WebCloseS2C> type() {
            return TYPE;
        }
    }

    public record BroadcastHudS2C(boolean active, String speakerUuid) implements CustomPacketPayload {
        public static final Type<BroadcastHudS2C> TYPE = GooseToolsPayloads.type("broadcast_hud_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastHudS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, BroadcastHudS2C::active,
                ByteBufCodecs.stringUtf8(64), BroadcastHudS2C::speakerUuid,
                BroadcastHudS2C::new);

        @Override
        public Type<BroadcastHudS2C> type() {
            return TYPE;
        }
    }

    /** A server-authoritative meeting alert with immutable player appearance snapshots. */
    public record MeetingAlertS2C(
            int kind,
            MeetingAppearance caller,
            MeetingAppearance victim) implements CustomPacketPayload {
        public static final int REPORT = 0;
        public static final int BELL = 1;
        public static final int SACRIFICE = 2;
        public static final Type<MeetingAlertS2C> TYPE = GooseToolsPayloads.type("meeting_alert_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, MeetingAlertS2C> CODEC =
                StreamCodec.of(MeetingAlertS2C::write, MeetingAlertS2C::read);

        public MeetingAlertS2C {
            if (kind != REPORT && kind != BELL && kind != SACRIFICE) {
                throw new IllegalArgumentException("Unknown meeting alert kind: " + kind);
            }
            if (caller == null) {
                throw new IllegalArgumentException("Meeting alert caller is required");
            }
            if (kind == REPORT && victim == null) {
                throw new IllegalArgumentException("Reported-body alert requires a victim");
            }
            if (kind != REPORT && victim != null) {
                throw new IllegalArgumentException("Bell alerts cannot carry a victim");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, MeetingAlertS2C payload) {
            buffer.writeByte(payload.kind());
            payload.caller().write(buffer);
            buffer.writeBoolean(payload.victim() != null);
            if (payload.victim() != null) {
                payload.victim().write(buffer);
            }
        }

        private static MeetingAlertS2C read(RegistryFriendlyByteBuf buffer) {
            int kind = buffer.readUnsignedByte();
            MeetingAppearance caller = MeetingAppearance.read(buffer);
            MeetingAppearance victim = buffer.readBoolean() ? MeetingAppearance.read(buffer) : null;
            return new MeetingAlertS2C(kind, caller, victim);
        }

        @Override
        public Type<MeetingAlertS2C> type() {
            return TYPE;
        }
    }

    public record MeetingAppearance(UUID playerId, String playerName, List<ItemStack> equipment) {
        private static final int EQUIPMENT_COUNT = EquipmentSlot.values().length;

        public MeetingAppearance {
            if (playerId == null) {
                throw new IllegalArgumentException("Meeting appearance player UUID is required");
            }
            playerName = playerName == null ? "" : playerName;
            if (playerName.length() > 64) {
                throw new IllegalArgumentException("Meeting appearance player name is too long");
            }
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            if (equipment.size() != EQUIPMENT_COUNT || equipment.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException("Meeting appearance must contain every equipment slot");
            }
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(playerId);
            buffer.writeUtf(playerName, 64);
            for (ItemStack item : equipment) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item);
            }
        }

        private static MeetingAppearance read(RegistryFriendlyByteBuf buffer) {
            UUID playerId = buffer.readUUID();
            String playerName = buffer.readUtf(64);
            List<ItemStack> equipment = new ArrayList<>(EQUIPMENT_COUNT);
            for (int index = 0; index < EQUIPMENT_COUNT; index++) {
                equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            return new MeetingAppearance(playerId, playerName, equipment);
        }
    }

    /** Per-player limited-vision state derived from the authoritative game settings. */
    public record VisionStateS2C(
            boolean active,
            float clearRadius,
            float fullFogRadius,
            boolean horizontalCylinder)
            implements CustomPacketPayload {
        public static final Type<VisionStateS2C> TYPE = GooseToolsPayloads.type("vision_state_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, VisionStateS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, VisionStateS2C::active,
                ByteBufCodecs.FLOAT, VisionStateS2C::clearRadius,
                ByteBufCodecs.FLOAT, VisionStateS2C::fullFogRadius,
                ByteBufCodecs.BOOL, VisionStateS2C::horizontalCylinder,
                VisionStateS2C::new);

        @Override
        public Type<VisionStateS2C> type() {
            return TYPE;
        }
    }

    /** Power-sabotage lifecycle and per-viewer eligibility for client-only accessibility aids. */
    public record BlackoutAssistStateS2C(
            boolean blackoutActive,
            boolean eligible,
            boolean inLobby)
            implements CustomPacketPayload {
        public static final Type<BlackoutAssistStateS2C> TYPE =
                GooseToolsPayloads.type("blackout_assist_state_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, BlackoutAssistStateS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, BlackoutAssistStateS2C::blackoutActive,
                        ByteBufCodecs.BOOL, BlackoutAssistStateS2C::eligible,
                        ByteBufCodecs.BOOL, BlackoutAssistStateS2C::inLobby,
                        BlackoutAssistStateS2C::new);

        @Override
        public Type<BlackoutAssistStateS2C> type() {
            return TYPE;
        }
    }

    /** Spoiler-free map and match phase for optional local Rich Presence integrations. */
    public record GamePresenceStateS2C(int phaseCode, int mapId)
            implements CustomPacketPayload {
        public static final Type<GamePresenceStateS2C> TYPE =
                GooseToolsPayloads.type("game_presence_state_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, GamePresenceStateS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, GamePresenceStateS2C::phaseCode,
                        ByteBufCodecs.VAR_INT, GamePresenceStateS2C::mapId,
                        GamePresenceStateS2C::new);

        public GamePresenceStateS2C {
            if (phaseCode < 0 || phaseCode > 7) {
                throw new IllegalArgumentException("Unknown game presence phase: " + phaseCode);
            }
            if (mapId < -1 || mapId > 10_000) {
                throw new IllegalArgumentException("Invalid game presence map id: " + mapId);
            }
        }

        @Override
        public Type<GamePresenceStateS2C> type() {
            return TYPE;
        }
    }

    /** Opens the local GooseTools settings screen for the receiving player only. */
    public record OpenClientSettingsS2C() implements CustomPacketPayload {
        public static final OpenClientSettingsS2C INSTANCE = new OpenClientSettingsS2C();
        public static final Type<OpenClientSettingsS2C> TYPE =
                GooseToolsPayloads.type("open_client_settings_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenClientSettingsS2C> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<OpenClientSettingsS2C> type() {
            return TYPE;
        }
    }

    /** Authoritative per-viewer Birdwatcher state. Range and cone constants stay client-defined. */
    public record BirdwatcherStateS2C(boolean active, boolean limitedVision)
            implements CustomPacketPayload {
        public static final Type<BirdwatcherStateS2C> TYPE =
                GooseToolsPayloads.type("birdwatcher_state_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, BirdwatcherStateS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, BirdwatcherStateS2C::active,
                        ByteBufCodecs.BOOL, BirdwatcherStateS2C::limitedVision,
                        BirdwatcherStateS2C::new);

        @Override
        public Type<BirdwatcherStateS2C> type() {
            return TYPE;
        }
    }

    /** Private Witch Doctor target state sent only to the owning Witch Doctor client. */
    public record WitchDoctorTargetS2C(boolean active, String targetUuid, boolean highlighted)
            implements CustomPacketPayload {
        public static final Type<WitchDoctorTargetS2C> TYPE =
                GooseToolsPayloads.type("witch_doctor_target_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, WitchDoctorTargetS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, WitchDoctorTargetS2C::active,
                        ByteBufCodecs.stringUtf8(36), WitchDoctorTargetS2C::targetUuid,
                        ByteBufCodecs.BOOL, WitchDoctorTargetS2C::highlighted,
                        WitchDoctorTargetS2C::new);

        @Override
        public Type<WitchDoctorTargetS2C> type() {
            return TYPE;
        }
    }

    /**
     * A complete, per-viewer nametag snapshot. Private attachment flags are filtered on
     * the server before this packet is built, so clients never receive hidden role data.
     */
    public record NameTagSnapshotS2C(
            List<NameTagEntry> entries,
            List<NameTagAlias> aliases) implements CustomPacketPayload {
        public static final int MAX_ENTRIES = 64;
        public static final int MAX_ALIASES = 128;
        public static final int MAX_ATTACHMENTS_PER_ENTRY = 16;
        public static final int MAX_ATTACHMENT_TEXTURE_LENGTH = 256;
        public static final Type<NameTagSnapshotS2C> TYPE =
                GooseToolsPayloads.type("nametag_snapshot_s2c_v4");
        public static final StreamCodec<RegistryFriendlyByteBuf, NameTagSnapshotS2C> CODEC =
                StreamCodec.of(NameTagSnapshotS2C::write, NameTagSnapshotS2C::read);

        public NameTagSnapshotS2C {
            entries = List.copyOf(entries);
            aliases = List.copyOf(aliases);
            if (entries.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("Too many nametag entries: " + entries.size());
            }
            if (aliases.size() > MAX_ALIASES) {
                throw new IllegalArgumentException("Too many nametag aliases: " + aliases.size());
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, NameTagSnapshotS2C payload) {
            buffer.writeVarInt(payload.entries.size());
            for (NameTagEntry entry : payload.entries) {
                buffer.writeUUID(entry.playerId());
                buffer.writeUUID(entry.identityPlayerId());
                buffer.writeUtf(entry.scoreboardName(), 64);
                buffer.writeUtf(entry.name(), 64);
                buffer.writeInt(entry.rgb());
                buffer.writeVarInt(entry.serialNumber());
                buffer.writeVarInt(entry.attachmentFlags());
                buffer.writeVarInt(entry.markerCode());
                buffer.writeBoolean(entry.markerNameTagVisible());
                buffer.writeVarInt(entry.attachments().size());
                for (NameTagIcon icon : entry.attachments()) {
                    buffer.writeUtf(icon.texture(), MAX_ATTACHMENT_TEXTURE_LENGTH);
                    buffer.writeFloat(icon.width());
                    buffer.writeFloat(icon.height());
                    buffer.writeInt(icon.rgb());
                }
            }
            buffer.writeVarInt(payload.aliases.size());
            for (NameTagAlias alias : payload.aliases) {
                buffer.writeUUID(alias.entityId());
                buffer.writeUUID(alias.playerId());
            }
        }

        private static NameTagSnapshotS2C read(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid nametag entry count: " + size);
            }
            List<NameTagEntry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                UUID playerId = buffer.readUUID();
                UUID identityPlayerId = buffer.readUUID();
                String scoreboardName = buffer.readUtf(64);
                String name = buffer.readUtf(64);
                int rgb = buffer.readInt() & 0x00ffffff;
                int serialNumber = Math.clamp(buffer.readVarInt(), 0, 21);
                int attachmentFlags = buffer.readVarInt();
                int markerCode = buffer.readVarInt();
                boolean markerNameTagVisible = buffer.readBoolean();
                int attachmentCount = buffer.readVarInt();
                if (attachmentCount < 0 || attachmentCount > MAX_ATTACHMENTS_PER_ENTRY) {
                    throw new IllegalArgumentException("Invalid nametag attachment count: " + attachmentCount);
                }
                List<NameTagIcon> attachments = new ArrayList<>(attachmentCount);
                for (int attachment = 0; attachment < attachmentCount; attachment++) {
                    attachments.add(new NameTagIcon(
                            buffer.readUtf(MAX_ATTACHMENT_TEXTURE_LENGTH),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readInt()));
                }
                entries.add(new NameTagEntry(
                        playerId,
                        identityPlayerId,
                        scoreboardName,
                        name,
                        rgb,
                        serialNumber,
                        attachmentFlags,
                        markerCode,
                        markerNameTagVisible,
                        attachments));
            }
            int aliasCount = buffer.readVarInt();
            if (aliasCount < 0 || aliasCount > MAX_ALIASES) {
                throw new IllegalArgumentException("Invalid nametag alias count: " + aliasCount);
            }
            List<NameTagAlias> aliases = new ArrayList<>(aliasCount);
            for (int index = 0; index < aliasCount; index++) {
                aliases.add(new NameTagAlias(buffer.readUUID(), buffer.readUUID()));
            }
            return new NameTagSnapshotS2C(entries, aliases);
        }

        @Override
        public Type<NameTagSnapshotS2C> type() {
            return TYPE;
        }
    }

    public record NameTagEntry(
            UUID playerId,
            UUID identityPlayerId,
            String scoreboardName,
            String name,
            int rgb,
            int serialNumber,
            int attachmentFlags,
            int markerCode,
            boolean markerNameTagVisible,
            List<NameTagIcon> attachments) {
        public NameTagEntry {
            identityPlayerId = identityPlayerId == null ? playerId : identityPlayerId;
            scoreboardName = scoreboardName == null ? "" : scoreboardName;
            name = name == null ? "" : name;
            if (scoreboardName.length() > 64 || name.length() > 64) {
                throw new IllegalArgumentException("Nametag entry text exceeds 64 characters");
            }
            rgb &= 0x00ffffff;
            serialNumber = Math.clamp(serialNumber, 0, 21);
            markerCode = com.goosethings.tools.marker.PlayerMarkerPolicy.validCodeOrZero(markerCode);
            markerNameTagVisible &= markerCode != 0;
            attachments = attachments == null ? List.of() : List.copyOf(attachments);
            if (attachments.size() > NameTagSnapshotS2C.MAX_ATTACHMENTS_PER_ENTRY) {
                throw new IllegalArgumentException("Too many nametag attachments: " + attachments.size());
            }
        }
    }

    /** Maps a viewer-private dream mannequin entity back to its original player. */
    public record NameTagAlias(UUID entityId, UUID playerId) {
        public NameTagAlias {
            if (entityId == null || playerId == null) {
                throw new IllegalArgumentException("Nametag aliases require both UUIDs");
            }
        }
    }

    public record NameTagIcon(String texture, float width, float height, int rgb) {
        public NameTagIcon {
            if (texture == null || texture.isBlank()
                    || texture.length() > NameTagSnapshotS2C.MAX_ATTACHMENT_TEXTURE_LENGTH) {
                throw new IllegalArgumentException("Invalid nametag attachment texture");
            }
            if (!Float.isFinite(width) || !Float.isFinite(height)
                    || width < 1.0F || width > 64.0F
                    || height < 1.0F || height > 64.0F) {
                throw new IllegalArgumentException("Invalid nametag attachment dimensions");
            }
            rgb &= 0x00ffffff;
        }
    }

    /** Complete viewer-private set of client-rendered dream stand-ins. */
    public record DreamSceneS2C(long revision, List<DreamStandIn> standIns)
            implements CustomPacketPayload {
        public static final int MAX_STAND_INS = 64;
        public static final Type<DreamSceneS2C> TYPE =
                GooseToolsPayloads.type("dream_scene_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, DreamSceneS2C> CODEC =
                StreamCodec.of(DreamSceneS2C::write, DreamSceneS2C::read);

        public DreamSceneS2C {
            standIns = standIns == null ? List.of() : List.copyOf(standIns);
            if (standIns.size() > MAX_STAND_INS) {
                throw new IllegalArgumentException("Too many dream stand-ins: " + standIns.size());
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, DreamSceneS2C payload) {
            buffer.writeVarLong(payload.revision());
            buffer.writeVarInt(payload.standIns().size());
            for (DreamStandIn standIn : payload.standIns()) {
                standIn.write(buffer);
            }
        }

        private static DreamSceneS2C read(RegistryFriendlyByteBuf buffer) {
            long revision = buffer.readVarLong();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_STAND_INS) {
                throw new IllegalArgumentException("Invalid dream stand-in count: " + count);
            }
            List<DreamStandIn> standIns = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                standIns.add(DreamStandIn.read(buffer));
            }
            return new DreamSceneS2C(revision, standIns);
        }

        @Override
        public Type<DreamSceneS2C> type() {
            return TYPE;
        }
    }

    /** Immutable appearance and placement for one client-only RemotePlayer. */
    public record DreamStandIn(
            UUID fakeId,
            UUID sourcePlayerId,
            UUID appearancePlayerId,
            String sourceName,
            String dimension,
            int kind,
            boolean retiring,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            String pose,
            List<ItemStack> equipment) {
        public static final int MEETING_PROXY = 0;
        public static final int MAP_BODY = 1;
        public static final int DREAM_CORPSE = 2;
        private static final int EQUIPMENT_COUNT = EquipmentSlot.values().length;

        public DreamStandIn {
            if (fakeId == null || sourcePlayerId == null || appearancePlayerId == null) {
                throw new IllegalArgumentException("Dream stand-in UUIDs are required");
            }
            sourceName = sourceName == null ? "" : sourceName;
            dimension = dimension == null ? "" : dimension;
            pose = pose == null ? "STANDING" : pose;
            if (sourceName.length() > 64 || dimension.length() > 128 || pose.length() > 32) {
                throw new IllegalArgumentException("Dream stand-in text field is too long");
            }
            if (kind < MEETING_PROXY || kind > DREAM_CORPSE) {
                throw new IllegalArgumentException("Unknown dream stand-in kind: " + kind);
            }
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yRot) || !Float.isFinite(xRot)
                    || !Float.isFinite(bodyRot) || !Float.isFinite(headRot)) {
                throw new IllegalArgumentException("Dream stand-in transform must be finite");
            }
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            if (equipment.size() != EQUIPMENT_COUNT
                    || equipment.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException(
                        "Dream stand-in must contain every equipment slot");
            }
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(fakeId);
            buffer.writeUUID(sourcePlayerId);
            buffer.writeUUID(appearancePlayerId);
            buffer.writeUtf(sourceName, 64);
            buffer.writeUtf(dimension, 128);
            buffer.writeByte(kind);
            buffer.writeBoolean(retiring);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeFloat(yRot);
            buffer.writeFloat(xRot);
            buffer.writeFloat(bodyRot);
            buffer.writeFloat(headRot);
            buffer.writeUtf(pose, 32);
            for (ItemStack item : equipment) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item);
            }
        }

        private static DreamStandIn read(RegistryFriendlyByteBuf buffer) {
            UUID fakeId = buffer.readUUID();
            UUID sourcePlayerId = buffer.readUUID();
            UUID appearancePlayerId = buffer.readUUID();
            String sourceName = buffer.readUtf(64);
            String dimension = buffer.readUtf(128);
            int kind = buffer.readUnsignedByte();
            boolean retiring = buffer.readBoolean();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            float yRot = buffer.readFloat();
            float xRot = buffer.readFloat();
            float bodyRot = buffer.readFloat();
            float headRot = buffer.readFloat();
            String pose = buffer.readUtf(32);
            List<ItemStack> equipment = new ArrayList<>(EQUIPMENT_COUNT);
            for (int index = 0; index < EQUIPMENT_COUNT; index++) {
                equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            return new DreamStandIn(fakeId, sourcePlayerId, appearancePlayerId,
                    sourceName, dimension, kind, retiring,
                    x, y, z, yRot, xRot, bodyRot, headRot, pose, equipment);
        }
    }

    /** Lightweight pose update for a remotely controlled meeting proxy. */
    public record DreamMotionS2C(
            UUID fakeId,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            String pose,
            int swingSequence,
            boolean offHand) implements CustomPacketPayload {
        public static final Type<DreamMotionS2C> TYPE =
                GooseToolsPayloads.type("dream_motion_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, DreamMotionS2C> CODEC =
                StreamCodec.of(DreamMotionS2C::write, DreamMotionS2C::read);

        public DreamMotionS2C {
            pose = pose == null ? "STANDING" : pose;
            if (fakeId == null || pose.length() > 32
                    || !Float.isFinite(yRot) || !Float.isFinite(xRot)
                    || !Float.isFinite(bodyRot) || !Float.isFinite(headRot)) {
                throw new IllegalArgumentException("Invalid dream proxy motion");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, DreamMotionS2C payload) {
            buffer.writeUUID(payload.fakeId());
            buffer.writeFloat(payload.yRot());
            buffer.writeFloat(payload.xRot());
            buffer.writeFloat(payload.bodyRot());
            buffer.writeFloat(payload.headRot());
            buffer.writeUtf(payload.pose(), 32);
            buffer.writeVarInt(payload.swingSequence());
            buffer.writeBoolean(payload.offHand());
        }

        private static DreamMotionS2C read(RegistryFriendlyByteBuf buffer) {
            return new DreamMotionS2C(
                    buffer.readUUID(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readUtf(32),
                    buffer.readVarInt(),
                    buffer.readBoolean());
        }

        @Override
        public Type<DreamMotionS2C> type() {
            return TYPE;
        }
    }

    /** Continuous animation state for a retained body driven by a remote projection. */
    public record ProjectionBodyMotionS2C(
            UUID fakeBodyId,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            String pose,
            int swingSequence,
            boolean offHand,
            float walkAnimationPosition,
            float walkAnimationSpeed) implements CustomPacketPayload {
        public static final Type<ProjectionBodyMotionS2C> TYPE =
                GooseToolsPayloads.type("projection_body_motion_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, ProjectionBodyMotionS2C> CODEC =
                StreamCodec.of(ProjectionBodyMotionS2C::write, ProjectionBodyMotionS2C::read);

        public ProjectionBodyMotionS2C {
            pose = pose == null ? "STANDING" : pose;
            if (fakeBodyId == null || pose.length() > 32
                    || !Float.isFinite(yRot) || !Float.isFinite(xRot)
                    || !Float.isFinite(bodyRot) || !Float.isFinite(headRot)
                    || !Float.isFinite(walkAnimationPosition)
                    || !Float.isFinite(walkAnimationSpeed)) {
                throw new IllegalArgumentException("Invalid projection-body motion");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer,
                                  ProjectionBodyMotionS2C payload) {
            buffer.writeUUID(payload.fakeBodyId());
            buffer.writeFloat(payload.yRot());
            buffer.writeFloat(payload.xRot());
            buffer.writeFloat(payload.bodyRot());
            buffer.writeFloat(payload.headRot());
            buffer.writeUtf(payload.pose(), 32);
            buffer.writeVarInt(payload.swingSequence());
            buffer.writeBoolean(payload.offHand());
            buffer.writeFloat(payload.walkAnimationPosition());
            buffer.writeFloat(payload.walkAnimationSpeed());
        }

        private static ProjectionBodyMotionS2C read(RegistryFriendlyByteBuf buffer) {
            return new ProjectionBodyMotionS2C(
                    buffer.readUUID(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readUtf(32),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat());
        }

        @Override
        public Type<ProjectionBodyMotionS2C> type() {
            return TYPE;
        }
    }

    /**
     * Complete viewer-private set of bodies whose original player render must stay
     * at the skill-entry position while the authoritative player controls a remote
     * projection. A full snapshot makes packet loss self-healing through the
     * server heartbeat.
     */
    public record ProjectionBodiesS2C(long revision, List<ProjectionBody> bodies)
            implements CustomPacketPayload {
        public static final int MAX_BODIES = 32;
        public static final Type<ProjectionBodiesS2C> TYPE =
                GooseToolsPayloads.type("projection_bodies_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, ProjectionBodiesS2C> CODEC =
                StreamCodec.of(ProjectionBodiesS2C::write, ProjectionBodiesS2C::read);

        public ProjectionBodiesS2C {
            bodies = bodies == null ? List.of() : List.copyOf(bodies);
            if (bodies.size() > MAX_BODIES) {
                throw new IllegalArgumentException("Too many projected bodies: " + bodies.size());
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer,
                                  ProjectionBodiesS2C payload) {
            buffer.writeVarLong(payload.revision());
            buffer.writeVarInt(payload.bodies().size());
            for (ProjectionBody body : payload.bodies()) {
                body.write(buffer);
            }
        }

        private static ProjectionBodiesS2C read(RegistryFriendlyByteBuf buffer) {
            long revision = buffer.readVarLong();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_BODIES) {
                throw new IllegalArgumentException("Invalid projected-body count: " + count);
            }
            List<ProjectionBody> bodies = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                bodies.add(ProjectionBody.read(buffer));
            }
            return new ProjectionBodiesS2C(revision, bodies);
        }

        @Override
        public Type<ProjectionBodiesS2C> type() {
            return TYPE;
        }
    }

    /** Immutable body snapshot plus the rendering policy chosen for one viewer. */
    public record ProjectionBody(
            UUID sourcePlayerId,
            UUID fakeBodyId,
            String sourceName,
            String dimension,
            int kind,
            int phase,
            boolean pinOriginal,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            float bodyRot,
            float headRot,
            String pose,
            List<ItemStack> equipment) {
        public static final int ASTRAL = 0;
        public static final int SNIPER = 1;
        public static final int ESPER = 2;
        public static final int MIME = 3;
        public static final int DREAM_LUCID = 4;
        public static final int DREAM_RAVEN = 5;

        public static final int PREPARED = 0;
        public static final int ACTIVE = 1;
        public static final int RETURNING = 2;

        private static final int EQUIPMENT_COUNT = EquipmentSlot.values().length;

        public ProjectionBody {
            if (sourcePlayerId == null || fakeBodyId == null) {
                throw new IllegalArgumentException("Projected-body UUIDs are required");
            }
            sourceName = sourceName == null ? "" : sourceName;
            dimension = dimension == null ? "" : dimension;
            pose = pose == null ? "STANDING" : pose;
            if (sourceName.length() > 64 || dimension.length() > 128 || pose.length() > 32) {
                throw new IllegalArgumentException("Projected-body text field is too long");
            }
            if (kind < ASTRAL || kind > DREAM_RAVEN) {
                throw new IllegalArgumentException("Unknown projection kind: " + kind);
            }
            if (phase < PREPARED || phase > RETURNING) {
                throw new IllegalArgumentException("Unknown projection phase: " + phase);
            }
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yRot) || !Float.isFinite(xRot)
                    || !Float.isFinite(bodyRot) || !Float.isFinite(headRot)) {
                throw new IllegalArgumentException("Projected-body transform must be finite");
            }
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            if (equipment.size() != EQUIPMENT_COUNT
                    || equipment.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException(
                        "Projected body must contain every equipment slot");
            }
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(sourcePlayerId);
            buffer.writeUUID(fakeBodyId);
            buffer.writeUtf(sourceName, 64);
            buffer.writeUtf(dimension, 128);
            buffer.writeByte(kind);
            buffer.writeByte(phase);
            buffer.writeBoolean(pinOriginal);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeFloat(yRot);
            buffer.writeFloat(xRot);
            buffer.writeFloat(bodyRot);
            buffer.writeFloat(headRot);
            buffer.writeUtf(pose, 32);
            for (ItemStack item : equipment) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item);
            }
        }

        private static ProjectionBody read(RegistryFriendlyByteBuf buffer) {
            UUID sourcePlayerId = buffer.readUUID();
            UUID fakeBodyId = buffer.readUUID();
            String sourceName = buffer.readUtf(64);
            String dimension = buffer.readUtf(128);
            int kind = buffer.readUnsignedByte();
            int phase = buffer.readUnsignedByte();
            boolean pinOriginal = buffer.readBoolean();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            float yRot = buffer.readFloat();
            float xRot = buffer.readFloat();
            float bodyRot = buffer.readFloat();
            float headRot = buffer.readFloat();
            String pose = buffer.readUtf(32);
            List<ItemStack> equipment = new ArrayList<>(EQUIPMENT_COUNT);
            for (int index = 0; index < EQUIPMENT_COUNT; index++) {
                equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            return new ProjectionBody(
                    sourcePlayerId, fakeBodyId, sourceName, dimension, kind, phase,
                    pinOriginal, x, y, z, yRot, xRot, bodyRot, headRot, pose, equipment);
        }
    }

    public record TaskMarkersS2C(String json) implements CustomPacketPayload {
        public static final Type<TaskMarkersS2C> TYPE = GooseToolsPayloads.type("task_markers_s2c");
        public static final StreamCodec<RegistryFriendlyByteBuf, TaskMarkersS2C> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(com.goosethings.tools.map.TaskMarkerConfig.MAX_BYTES),
                        TaskMarkersS2C::json, TaskMarkersS2C::new);

        @Override
        public Type<TaskMarkersS2C> type() { return TYPE; }
    }

    public record WebRequestC2S(String pageId) implements CustomPacketPayload {
        public static final Type<WebRequestC2S> TYPE = GooseToolsPayloads.type("web_request_c2s");
        public static final StreamCodec<RegistryFriendlyByteBuf, WebRequestC2S> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_PAGE_ID_LENGTH), WebRequestC2S::pageId,
                WebRequestC2S::new);

        @Override
        public Type<WebRequestC2S> type() {
            return TYPE;
        }
    }

    /**
     * The target and ray observed by the rendering client when a skill item is used.
     * This is only a claim: the server independently validates its age, geometry,
     * range, occlusion and current datapack candidate tags before accepting it.
     */
    public record AimClaimC2S(
            long sequence,
            UUID targetId,
            double originX,
            double originY,
            double originZ,
            float directionX,
            float directionY,
            float directionZ,
            double hitX,
            double hitY,
            double hitZ,
            double targetX,
            double targetY,
            double targetZ) implements CustomPacketPayload {
        public static final Type<AimClaimC2S> TYPE = GooseToolsPayloads.type("aim_claim_c2s_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AimClaimC2S> CODEC =
                StreamCodec.of(AimClaimC2S::write, AimClaimC2S::read);

        private static void write(RegistryFriendlyByteBuf buffer, AimClaimC2S payload) {
            buffer.writeVarLong(payload.sequence());
            buffer.writeUUID(payload.targetId());
            buffer.writeDouble(payload.originX());
            buffer.writeDouble(payload.originY());
            buffer.writeDouble(payload.originZ());
            buffer.writeFloat(payload.directionX());
            buffer.writeFloat(payload.directionY());
            buffer.writeFloat(payload.directionZ());
            buffer.writeDouble(payload.hitX());
            buffer.writeDouble(payload.hitY());
            buffer.writeDouble(payload.hitZ());
            buffer.writeDouble(payload.targetX());
            buffer.writeDouble(payload.targetY());
            buffer.writeDouble(payload.targetZ());
        }

        private static AimClaimC2S read(RegistryFriendlyByteBuf buffer) {
            return new AimClaimC2S(
                    buffer.readVarLong(),
                    buffer.readUUID(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble());
        }

        @Override
        public Type<AimClaimC2S> type() {
            return TYPE;
        }
    }

    public record AiReportStartS2C(long transferId, int compressedSize, int chunkCount)
            implements CustomPacketPayload {
        public static final Type<AiReportStartS2C> TYPE = GooseToolsPayloads.type("ai_report_start_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiReportStartS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, AiReportStartS2C::transferId,
                ByteBufCodecs.VAR_INT, AiReportStartS2C::compressedSize,
                ByteBufCodecs.VAR_INT, AiReportStartS2C::chunkCount,
                AiReportStartS2C::new);

        @Override
        public Type<AiReportStartS2C> type() { return TYPE; }
    }

    public record AiReportChunkS2C(long transferId, int index, byte[] bytes)
            implements CustomPacketPayload {
        public static final Type<AiReportChunkS2C> TYPE = GooseToolsPayloads.type("ai_report_chunk_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiReportChunkS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, AiReportChunkS2C::transferId,
                ByteBufCodecs.VAR_INT, AiReportChunkS2C::index,
                ByteBufCodecs.byteArray(MAX_CHUNK_BYTES), AiReportChunkS2C::bytes,
                AiReportChunkS2C::new);

        @Override
        public Type<AiReportChunkS2C> type() { return TYPE; }
    }

    public record AiReportClearS2C() implements CustomPacketPayload {
        public static final AiReportClearS2C INSTANCE = new AiReportClearS2C();
        public static final Type<AiReportClearS2C> TYPE = GooseToolsPayloads.type("ai_report_clear_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiReportClearS2C> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<AiReportClearS2C> type() { return TYPE; }
    }

    public record AiProgressS2C(String stage, int percent, int completed, int total, boolean terminal)
            implements CustomPacketPayload {
        public static final Type<AiProgressS2C> TYPE = GooseToolsPayloads.type("ai_progress_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiProgressS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), AiProgressS2C::stage,
                ByteBufCodecs.VAR_INT, AiProgressS2C::percent,
                ByteBufCodecs.VAR_INT, AiProgressS2C::completed,
                ByteBufCodecs.VAR_INT, AiProgressS2C::total,
                ByteBufCodecs.BOOL, AiProgressS2C::terminal,
                AiProgressS2C::new);

        public AiProgressS2C {
            stage = stage == null ? "" : stage;
            percent = Math.clamp(percent, 0, 100);
            completed = Math.max(0, completed);
            total = Math.max(0, total);
        }

        @Override
        public Type<AiProgressS2C> type() { return TYPE; }
    }

    public record AiNameVoteOpenS2C(
            long voteId,
            int questionIndex,
            int questionCount,
            int timeoutSeconds,
            String alias,
            String context,
            List<AiVoteCandidate> candidates) implements CustomPacketPayload {
        public static final int MAX_CANDIDATES = 64;
        public static final Type<AiNameVoteOpenS2C> TYPE = GooseToolsPayloads.type("ai_name_vote_open_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiNameVoteOpenS2C> CODEC =
                StreamCodec.of(AiNameVoteOpenS2C::write, AiNameVoteOpenS2C::read);

        public AiNameVoteOpenS2C {
            alias = alias == null ? "" : alias;
            context = context == null ? "" : context;
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            if (alias.length() > 48 || context.length() > 240 || candidates.size() > MAX_CANDIDATES) {
                throw new IllegalArgumentException("Invalid AI name-vote payload");
            }
        }

        private static void write(RegistryFriendlyByteBuf buffer, AiNameVoteOpenS2C payload) {
            buffer.writeVarLong(payload.voteId());
            buffer.writeVarInt(payload.questionIndex());
            buffer.writeVarInt(payload.questionCount());
            buffer.writeVarInt(payload.timeoutSeconds());
            buffer.writeUtf(payload.alias(), 48);
            buffer.writeUtf(payload.context(), 240);
            buffer.writeVarInt(payload.candidates().size());
            for (AiVoteCandidate candidate : payload.candidates()) {
                buffer.writeUUID(candidate.playerId());
                buffer.writeUtf(candidate.playerName(), 64);
            }
        }

        private static AiNameVoteOpenS2C read(RegistryFriendlyByteBuf buffer) {
            long voteId = buffer.readVarLong();
            int questionIndex = buffer.readVarInt();
            int questionCount = buffer.readVarInt();
            int timeoutSeconds = buffer.readVarInt();
            String alias = buffer.readUtf(48);
            String context = buffer.readUtf(240);
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_CANDIDATES) {
                throw new IllegalArgumentException("Invalid AI vote candidate count: " + size);
            }
            List<AiVoteCandidate> candidates = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                candidates.add(new AiVoteCandidate(buffer.readUUID(), buffer.readUtf(64)));
            }
            return new AiNameVoteOpenS2C(voteId, questionIndex, questionCount, timeoutSeconds,
                    alias, context, candidates);
        }

        @Override
        public Type<AiNameVoteOpenS2C> type() { return TYPE; }
    }

    public record AiVoteCandidate(UUID playerId, String playerName) {
        public AiVoteCandidate {
            if (playerId == null) throw new IllegalArgumentException("Missing AI vote candidate UUID");
            playerName = playerName == null ? "" : playerName;
            if (playerName.length() > 64) throw new IllegalArgumentException("AI vote candidate name is too long");
        }
    }

    public record AiNameVoteCloseS2C(long voteId) implements CustomPacketPayload {
        public static final Type<AiNameVoteCloseS2C> TYPE = GooseToolsPayloads.type("ai_name_vote_close_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiNameVoteCloseS2C> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, AiNameVoteCloseS2C::voteId,
                        AiNameVoteCloseS2C::new);

        @Override
        public Type<AiNameVoteCloseS2C> type() { return TYPE; }
    }

    public record AiNameVoteC2S(long voteId, UUID candidateId) implements CustomPacketPayload {
        public static final Type<AiNameVoteC2S> TYPE = GooseToolsPayloads.type("ai_name_vote_c2s_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiNameVoteC2S> CODEC =
                StreamCodec.of(AiNameVoteC2S::write, AiNameVoteC2S::read);

        private static void write(RegistryFriendlyByteBuf buffer, AiNameVoteC2S payload) {
            buffer.writeVarLong(payload.voteId());
            buffer.writeUUID(payload.candidateId());
        }

        private static AiNameVoteC2S read(RegistryFriendlyByteBuf buffer) {
            return new AiNameVoteC2S(buffer.readVarLong(), buffer.readUUID());
        }

        @Override
        public Type<AiNameVoteC2S> type() { return TYPE; }
    }

    public record AiDebugStartS2C(long transferId, int compressedSize, int chunkCount)
            implements CustomPacketPayload {
        public static final Type<AiDebugStartS2C> TYPE = GooseToolsPayloads.type("ai_debug_start_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiDebugStartS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, AiDebugStartS2C::transferId,
                ByteBufCodecs.VAR_INT, AiDebugStartS2C::compressedSize,
                ByteBufCodecs.VAR_INT, AiDebugStartS2C::chunkCount,
                AiDebugStartS2C::new);

        @Override
        public Type<AiDebugStartS2C> type() { return TYPE; }
    }

    public record AiDebugChunkS2C(long transferId, int index, byte[] bytes)
            implements CustomPacketPayload {
        public static final Type<AiDebugChunkS2C> TYPE = GooseToolsPayloads.type("ai_debug_chunk_s2c_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, AiDebugChunkS2C> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, AiDebugChunkS2C::transferId,
                ByteBufCodecs.VAR_INT, AiDebugChunkS2C::index,
                ByteBufCodecs.byteArray(MAX_CHUNK_BYTES), AiDebugChunkS2C::bytes,
                AiDebugChunkS2C::new);

        @Override
        public Type<AiDebugChunkS2C> type() { return TYPE; }
    }
}
