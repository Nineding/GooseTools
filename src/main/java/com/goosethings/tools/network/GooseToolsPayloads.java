package com.goosethings.tools.network;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
        PayloadTypeRegistry.clientboundPlay().register(VisionStateS2C.TYPE, VisionStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlackoutAssistStateS2C.TYPE, BlackoutAssistStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenClientSettingsS2C.TYPE, OpenClientSettingsS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BirdwatcherStateS2C.TYPE, BirdwatcherStateS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WitchDoctorTargetS2C.TYPE, WitchDoctorTargetS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NameTagSnapshotS2C.TYPE, NameTagSnapshotS2C.CODEC);
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
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, path));
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
                int serialNumber = Math.clamp(buffer.readVarInt(), 0, 20);
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
            serialNumber = Math.clamp(serialNumber, 0, 20);
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
