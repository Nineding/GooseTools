package com.goosethings.tools.client.nametag;

import com.goosethings.tools.marker.PlayerMarkerCatalog;
import com.goosethings.tools.network.GooseToolsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Client cache for the already privacy-filtered nametag snapshot. */
public final class NameTagClientState {
    private static final Map<UUID, GooseToolsPayloads.NameTagEntry> ENTRIES = new LinkedHashMap<>();
    private static final Map<UUID, UUID> ENTITY_ALIASES = new LinkedHashMap<>();
    private static final Map<UUID, GooseToolsPayloads.NameTagEntry> ITEM_PROFILES = new LinkedHashMap<>();
    private static final Map<String, GooseToolsPayloads.NameTagEntry> SCOREBOARD_NAMES = new LinkedHashMap<>();

    private NameTagClientState() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                GooseToolsPayloads.NameTagSnapshotS2C.TYPE,
                (payload, context) -> context.client().execute(() -> apply(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static GooseToolsPayloads.NameTagEntry entry(UUID playerId) {
        return playerId == null ? null : ENTRIES.get(playerId);
    }

    public static GooseToolsPayloads.NameTagEntry entry(Entity entity) {
        return entry(sourcePlayerId(entity));
    }

    public static boolean isManaged(Entity entity) {
        return entry(entity) != null;
    }

    public static PlayerMarkerCatalog.Definition marker(ItemStack stack) {
        GooseToolsPayloads.NameTagEntry entry = entryForHead(stack);
        return entry == null ? null : PlayerMarkerCatalog.byCode(entry.markerCode());
    }

    /** Returns the identity currently presented by the rendered player. */
    public static UUID identityPlayerId(UUID renderedPlayerId) {
        GooseToolsPayloads.NameTagEntry entry = entry(renderedPlayerId);
        return entry == null ? renderedPlayerId : entry.identityPlayerId();
    }

    /** Resolves real players and profile-backed stand-ins to the same source UUID. */
    public static UUID sourcePlayerId(Entity entity) {
        if (entity instanceof Player) {
            return entity.getUUID();
        }
        if (entity instanceof Mannequin mannequin) {
            UUID alias = ENTITY_ALIASES.get(entity.getUUID());
            return alias != null ? alias : mannequin.getProfile().partialProfile().id();
        }
        return null;
    }

    private static void apply(GooseToolsPayloads.NameTagSnapshotS2C snapshot) {
        ENTRIES.clear();
        ENTITY_ALIASES.clear();
        ITEM_PROFILES.clear();
        SCOREBOARD_NAMES.clear();
        for (GooseToolsPayloads.NameTagEntry entry : snapshot.entries()) {
            ENTRIES.put(entry.playerId(), entry);
            SCOREBOARD_NAMES.put(entry.scoreboardName().toLowerCase(Locale.ROOT), entry);
            ITEM_PROFILES.put(entry.playerId(), entry);
            ITEM_PROFILES.put(menuProfileId(entry.playerId()), entry);
            ITEM_PROFILES.put(entry.identityPlayerId(), entry);
            ITEM_PROFILES.put(menuProfileId(entry.identityPlayerId()), entry);
        }
        for (GooseToolsPayloads.NameTagAlias alias : snapshot.aliases()) {
            ENTITY_ALIASES.put(alias.entityId(), alias.playerId());
        }
    }

    private static void clear() {
        ENTRIES.clear();
        ENTITY_ALIASES.clear();
        ITEM_PROFILES.clear();
        SCOREBOARD_NAMES.clear();
    }

    private static GooseToolsPayloads.NameTagEntry entryForHead(ItemStack stack) {
        if (stack == null || !stack.is(Items.PLAYER_HEAD)) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            GooseToolsPayloads.NameTagEntry direct = byUuid(tag.getStringOr(
                    "goosetools_mark_target", ""));
            if (direct != null) {
                return direct;
            }
            direct = byName(tag.getStringOr("goosetools_mark_name", ""));
            if (direct != null) {
                return direct;
            }
            direct = byName(tag.getStringOr("vote_target", ""));
            if (direct != null) {
                return direct;
            }
            direct = byName(tag.getStringOr("PlayerName", ""));
            if (direct != null) {
                return direct;
            }
        }
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null) {
            return null;
        }
        UUID profileId = profile.partialProfile().id();
        return profileId == null ? null : ITEM_PROFILES.get(profileId);
    }

    private static GooseToolsPayloads.NameTagEntry byUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ENTRIES.get(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static GooseToolsPayloads.NameTagEntry byName(String name) {
        return name == null || name.isBlank()
                ? null : SCOREBOARD_NAMES.get(name.toLowerCase(Locale.ROOT));
    }

    private static UUID menuProfileId(UUID sourceId) {
        String key = "goosethings:menu-head:" + sourceId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
