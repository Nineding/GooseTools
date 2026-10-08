package com.goosethings.tools.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class GgdMapState {
    public static final String MARKER_PREFIX = "ggdmap:";
    public static final String MEETING_LAST_POSITION = "meeting_lastpos";
    public static final String REPORTED_BODY = "reported_body";
    public static final String GAME_ACTIVE = "game_active";
    public static final String LAYOUT_POOLCORE_BASIC = "layout_poolcore_basic";
    public static final String LAYOUT_POOLCORE_ADVANCE = "layout_poolcore_advance";

    private static boolean taskPathsVisible;
    private static boolean gameActiveSticky;
    private static final Map<String, Marker> cachedSpecialMarkers = new HashMap<>();
    private static final Map<Integer, String> cachedSpecialMarkerIds = new HashMap<>();

    private GgdMapState() {
    }

    public static boolean taskPathsVisible() {
        return taskPathsVisible;
    }

    public static void toggleTaskPaths() {
        taskPathsVisible = !taskPathsVisible;
    }

    public static Vec3 effectiveMapPosition(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }

        Vec3 playerPosition = minecraft.player.position();
        Marker meetingPosition = findMarker(minecraft, MEETING_LAST_POSITION);
        if (isEagletonMeetingPosition(playerPosition, meetingPosition)) {
            return meetingPosition.position();
        }
        if (GameMapBounds.at(playerPosition.x, playerPosition.z) != null) {
            return playerPosition;
        }

        if (meetingPosition != null
                && GameMapBounds.at(meetingPosition.position().x, meetingPosition.position().z) != null) {
            return meetingPosition.position();
        }
        return playerPosition;
    }

    public static GameMapBounds currentBounds(Minecraft minecraft) {
        if (!isGameActive(minecraft)) {
            return null;
        }
        Vec3 position = effectiveMapPosition(minecraft);
        return position == null ? null : GameMapBounds.at(position.x, position.z);
    }

    public static Integer currentCaveTopY(Minecraft minecraft) {
        GameMapBounds bounds = currentBounds(minecraft);
        if (bounds == null || !bounds.usesCaveMode()) {
            return null;
        }
        Vec3 position = effectiveMapPosition(minecraft);
        if (position == null) {
            return null;
        }
        // Xaero's automatic cave detector caps the visible layer at four
        // blocks above the player's feet. Mirroring that rule also keeps the
        // same room layer selected after the player is moved to a meeting.
        return (int) Math.floor(position.y) + 4;
    }

    public static boolean isMeetingView(Minecraft minecraft) {
        if (!isGameActive(minecraft) || minecraft.player == null) {
            return false;
        }
        Marker marker = findMarker(minecraft, MEETING_LAST_POSITION);
        if (isEagletonMeetingPosition(minecraft.player.position(), marker)) {
            return true;
        }
        if (GameMapBounds.at(minecraft.player.getX(), minecraft.player.getZ()) != null) {
            return false;
        }
        return marker != null
                && GameMapBounds.at(marker.position().x, marker.position().z) != null;
    }

    static boolean isEagletonMeetingPosition(Vec3 playerPosition, Marker meetingPosition) {
        return meetingPosition != null
                && GameMapBounds.EAGLETON_SIMPLIFY.contains(meetingPosition.position().x, meetingPosition.position().z)
                && playerPosition.x >= -1701 && playerPosition.x <= -1669
                && playerPosition.y >= 78 && playerPosition.y <= 88
                && playerPosition.z >= -563 && playerPosition.z <= -546;
    }

    public static boolean isPoolcoreAdvanced(Minecraft minecraft) {
        return findMarker(minecraft, LAYOUT_POOLCORE_ADVANCE) != null;
    }

    public static boolean isGameActive(Minecraft minecraft) {
        if (!GgdServerContext.isGooseServer()) {
            return false;
        }
        if (findMarker(minecraft, GAME_ACTIVE) != null) {
            gameActiveSticky = true;
            return true;
        }
        // Keep the match flag across death/respawn. Killing a player recreates
        // the local player and used to wipe this cache, which dropped NO_RADAR.
        return gameActiveSticky;
    }

    public static boolean isControlMarker(String id) {
        return GAME_ACTIVE.equals(id)
                || LAYOUT_POOLCORE_BASIC.equals(id)
                || LAYOUT_POOLCORE_ADVANCE.equals(id);
    }

    public static Marker findMarker(Minecraft minecraft, String wantedId) {
        if (minecraft.level == null) {
            return cachedSpecialMarkers.get(wantedId);
        }
        for (var entity : minecraft.level.entitiesForRendering()) {
            Marker marker = markerFromEntity(entity);
            if (marker != null) {
                cacheSpecialMarker(entity.getId(), marker);
                if (wantedId.equals(marker.id())) {
                    return marker;
                }
            }
        }
        return cachedSpecialMarkers.get(wantedId);
    }

    public static void observeMarkerEntity(Entity entity) {
        Marker marker = markerFromEntity(entity);
        if (marker != null) {
            cacheSpecialMarker(entity.getId(), marker);
        }
    }

    public static void removeMarkerEntity(int entityId) {
        String markerId = cachedSpecialMarkerIds.remove(entityId);
        if (markerId != null) {
            cachedSpecialMarkers.remove(markerId);
            if (GAME_ACTIVE.equals(markerId)) {
                gameActiveSticky = false;
            }
        }
    }

    public static void clearCachedMarkers() {
        cachedSpecialMarkers.clear();
        cachedSpecialMarkerIds.clear();
        gameActiveSticky = false;
    }

    private static Marker markerFromEntity(Entity entity) {
        if (!(entity instanceof Display.BlockDisplay) || entity.getCustomName() == null) {
            return null;
        }
        String encoded = entity.getCustomName().getString();
        if (!encoded.startsWith(MARKER_PREFIX)) {
            return null;
        }
        String[] parts = encoded.split(":", 3);
        if (parts.length < 2) {
            return null;
        }
        String id = parts[1];
        if (!MEETING_LAST_POSITION.equals(id)
                && !REPORTED_BODY.equals(id)
                && !GAME_ACTIVE.equals(id)
                && !LAYOUT_POOLCORE_BASIC.equals(id)
                && !LAYOUT_POOLCORE_ADVANCE.equals(id)) {
            return null;
        }
        String kind = parts.length == 3 ? parts[2] : "normal";
        return new Marker(id, kind, entity.position());
    }

    private static void cacheSpecialMarker(int entityId, Marker marker) {
        String previousMarkerId = cachedSpecialMarkerIds.put(entityId, marker.id());
        if (previousMarkerId != null && !previousMarkerId.equals(marker.id())) {
            cachedSpecialMarkers.remove(previousMarkerId);
        }
        cachedSpecialMarkers.put(marker.id(), marker);
    }

    public record Marker(String id, String kind, Vec3 position) {
    }
}
