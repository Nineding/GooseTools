package com.goosethings.tools.nametag;

import com.goosethings.tools.network.GooseToolsPayloads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded public nametag-icon slots populated by server commands.
 *
 * <p>The slot key makes updates idempotent: setting the same slot replaces its
 * previous visual instead of appending another icon. Texture paths and ordering
 * remain data-pack owned, so adding a new public icon does not require Java code.</p>
 */
final class NameTagIconAssignments {
    private static final String IDENTITY_TITLE_SLOT = "trust";
    private static final int MAX_ICONS_PER_PLAYER =
            GooseToolsPayloads.NameTagSnapshotS2C.MAX_ATTACHMENTS_PER_ENTRY;
    private static final int MIN_ORDER = -10_000;
    private static final int MAX_ORDER = 10_000;
    private static final Map<UUID, Map<String, Assignment>> BY_PLAYER = new HashMap<>();

    private NameTagIconAssignments() {
    }

    static int set(Collection<UUID> players,
                   String slot,
                   String texture,
                   float width,
                   float height,
                   int rgb,
                   int order) {
        validateSlot(slot);
        validateTexture(texture);
        validateDimensions(width, height);
        if (order < MIN_ORDER || order > MAX_ORDER) {
            throw new IllegalArgumentException("order must be between -10000 and 10000");
        }
        Assignment next = new Assignment(
                slot, texture, width, height, rgb & 0x00ff_ffff, order);
        int changed = 0;
        for (UUID player : players) {
            Map<String, Assignment> slots = BY_PLAYER.computeIfAbsent(
                    player, ignored -> new HashMap<>());
            if (!slots.containsKey(slot) && slots.size() >= MAX_ICONS_PER_PLAYER) {
                continue;
            }
            if (!next.equals(slots.put(slot, next))) {
                changed++;
            }
        }
        return changed;
    }

    static int remove(Collection<UUID> players, String slot) {
        validateSlot(slot);
        int changed = 0;
        for (UUID player : players) {
            Map<String, Assignment> slots = BY_PLAYER.get(player);
            if (slots == null || slots.remove(slot) == null) {
                continue;
            }
            changed++;
            if (slots.isEmpty()) {
                BY_PLAYER.remove(player);
            }
        }
        return changed;
    }

    static int clear(Collection<UUID> players) {
        int changed = 0;
        for (UUID player : players) {
            Map<String, Assignment> removed = BY_PLAYER.remove(player);
            if (removed != null) {
                changed += removed.size();
            }
        }
        return changed;
    }

    static void removePlayer(UUID player) {
        BY_PLAYER.remove(player);
    }

    static void clear() {
        BY_PLAYER.clear();
    }

    static List<GooseToolsPayloads.NameTagIcon> icons(UUID player) {
        return iconsForIdentity(player, player);
    }

    /**
     * Resolves public icons for a rendered player while allowing an active
     * disguise to present the visual identity's title. Other public slots stay
     * attached to the rendered player and cannot leak unrelated lobby state.
     */
    static List<GooseToolsPayloads.NameTagIcon> iconsForIdentity(
            UUID renderedPlayer,
            UUID visualIdentity) {
        Map<String, Assignment> renderedSlots = BY_PLAYER.get(renderedPlayer);
        if (renderedPlayer.equals(visualIdentity)) {
            return toIcons(renderedSlots);
        }

        Map<String, Assignment> effectiveSlots = renderedSlots == null
                ? new HashMap<>()
                : new HashMap<>(renderedSlots);
        effectiveSlots.remove(IDENTITY_TITLE_SLOT);

        Map<String, Assignment> identitySlots = BY_PLAYER.get(visualIdentity);
        Assignment identityTitle = identitySlots == null
                ? null
                : identitySlots.get(IDENTITY_TITLE_SLOT);
        if (identityTitle != null) {
            if (effectiveSlots.size() >= MAX_ICONS_PER_PLAYER) {
                Assignment last = effectiveSlots.values().stream()
                        .max(Comparator.comparingInt(Assignment::order)
                                .thenComparing(Assignment::slot))
                        .orElseThrow();
                effectiveSlots.remove(last.slot());
            }
            effectiveSlots.put(IDENTITY_TITLE_SLOT, identityTitle);
        }
        return toIcons(effectiveSlots);
    }

    private static List<GooseToolsPayloads.NameTagIcon> toIcons(
            Map<String, Assignment> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        List<Assignment> ordered = new ArrayList<>(slots.values());
        ordered.sort(Comparator.comparingInt(Assignment::order)
                .thenComparing(Assignment::slot));
        return ordered.stream()
                .map(assignment -> new GooseToolsPayloads.NameTagIcon(
                        assignment.texture(),
                        assignment.width(),
                        assignment.height(),
                        assignment.rgb()))
                .toList();
    }

    private static void validateSlot(String slot) {
        if (slot == null || !slot.matches("[a-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException(
                    "slot must use 1-64 lowercase letters, digits, dots, underscores, or hyphens");
        }
    }

    private static void validateTexture(String texture) {
        if (texture == null
                || texture.length() > GooseToolsPayloads.NameTagSnapshotS2C.MAX_ATTACHMENT_TEXTURE_LENGTH
                || !texture.matches("[a-z0-9_.-]{1,64}:[a-z0-9/._-]{1,191}")) {
            throw new IllegalArgumentException("invalid texture identifier");
        }
        String path = texture.substring(texture.indexOf(':') + 1);
        if (!path.startsWith("textures/")
                || !path.endsWith(".png")
                || path.contains("..")
                || path.contains("//")) {
            throw new IllegalArgumentException(
                    "texture must be a safe namespaced textures/*.png path");
        }
    }

    private static void validateDimensions(float width, float height) {
        if (!Float.isFinite(width) || !Float.isFinite(height)
                || width < 1.0F || width > 64.0F
                || height < 1.0F || height > 64.0F) {
            throw new IllegalArgumentException("width and height must be between 1 and 64");
        }
    }

    private record Assignment(
            String slot,
            String texture,
            float width,
            float height,
            int rgb,
            int order) {
    }
}
