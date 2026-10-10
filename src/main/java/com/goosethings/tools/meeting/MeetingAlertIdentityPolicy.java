package com.goosethings.tools.meeting;

import com.goosethings.tools.nametag.NameTagSync;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Meeting-alert HUD models must not share the live player UUID. Disguise
 * rewrites PlayerInfo and nametag identity under that UUID, so a report/bell
 * banner would otherwise show the stolen skin and wardrobe.
 */
public final class MeetingAlertIdentityPolicy {
    private static final int DEFAULT_RGB = 0xffffff;

    private MeetingAlertIdentityPolicy() {
    }

    public static UUID isolatedModelId(UUID playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Meeting alert model UUID is required");
        }
        return UUID.nameUUIDFromBytes(
                ("goosetools:meeting-alert:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isTrueIdentity(UUID playerId, UUID identityPlayerId) {
        return playerId != null && playerId.equals(identityPlayerId);
    }

    public static Label nametag(
            String snapshotName,
            UUID playerId,
            UUID liveIdentityPlayerId,
            String liveName,
            int liveRgb,
            int liveSerial,
            int liveAttachmentFlags,
            String cachedName,
            int cachedRgb,
            int cachedSerial,
            int cachedAttachmentFlags) {
        if (liveName != null && liveName.isEmpty()) {
            return Label.hiddenFromViewer();
        }
        if (isTrueIdentity(playerId, liveIdentityPlayerId)) {
            return visible(
                    firstNonBlank(liveName, snapshotName),
                    liveRgb,
                    liveSerial,
                    liveAttachmentFlags);
        }
        if (cachedName != null) {
            return visible(cachedName, cachedRgb, cachedSerial, cachedAttachmentFlags);
        }
        return visible(nullToEmpty(snapshotName), DEFAULT_RGB, 0, 0);
    }

    private static Label visible(String name, int rgb, int serial, int attachmentFlags) {
        return new Label(
                nullToEmpty(name),
                rgb & 0x00ffffff,
                Math.clamp(serial, 0, 21),
                (attachmentFlags & NameTagSync.LOVER) != 0,
                false);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? nullToEmpty(fallback) : preferred;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record Label(String name, int rgb, int serial, boolean lover, boolean concealed) {
        static Label hiddenFromViewer() {
            return new Label("", DEFAULT_RGB, 0, false, true);
        }
    }
}
