package com.goosethings.tools.client.presence;

import com.goosethings.tools.presence.GamePresenceMap;
import com.goosethings.tools.presence.GamePresencePhase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

/** Localized, spoiler-free values exposed to CraftPresence when it is installed. */
public final class GamePresenceClient {
    private static volatile boolean connected;
    private static volatile GamePresencePhase phase = GamePresencePhase.CONNECTED;
    private static volatile int mapId = -1;

    private GamePresenceClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> CraftPresenceCompat.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void apply(int phaseCode, int currentMapId) {
        connected = true;
        phase = GamePresencePhase.fromCode(phaseCode);
        mapId = currentMapId;
        CraftPresenceCompat.refresh();
    }

    public static String phaseId() {
        return connected ? phase.id() : "";
    }

    public static String stateText() {
        return connected ? translatedState(phase) : "";
    }

    public static String mapText() {
        if (!connected) {
            return "";
        }
        GamePresenceMap map = GamePresenceMap.fromId(mapId);
        return map == null
                ? ""
                : Component.translatableWithFallback(map.translationKey(), map.fallback()).getString();
    }

    public static String activityText() {
        if (!connected) {
            return "";
        }
        String map = mapText();
        if (map.isBlank() || phase == GamePresencePhase.CONNECTED || phase == GamePresencePhase.TUTORIAL) {
            return translatedState(phase);
        }
        return Component.translatableWithFallback(
                phase.activityTranslationKey(), activityFallback(phase), map).getString();
    }

    private static String translatedState(GamePresencePhase current) {
        return Component.translatableWithFallback(
                current.stateTranslationKey(), stateFallback(current)).getString();
    }

    private static String stateFallback(GamePresencePhase current) {
        return switch (current) {
            case CONNECTED -> "On a Goose Goose Duck server";
            case LOBBY -> "In the game lobby";
            case PREPARING -> "Preparing to enter the map";
            case PLAYING -> "Playing";
            case MEETING -> "In a meeting";
            case SPECTATING -> "Spectating";
            case RESULTS -> "Viewing match results";
            case TUTORIAL -> "Playing the tutorial";
        };
    }

    private static String activityFallback(GamePresencePhase current) {
        return switch (current) {
            case LOBBY -> "In the lobby · Selected %s";
            case PREPARING -> "Preparing to enter %s";
            case PLAYING -> "Playing %s";
            case MEETING -> "%s · In a meeting";
            case SPECTATING -> "Spectating %s";
            case RESULTS -> "%s · Match results";
            case CONNECTED, TUTORIAL -> "%s";
        };
    }

    private static void reset() {
        connected = false;
        phase = GamePresencePhase.CONNECTED;
        mapId = -1;
        CraftPresenceCompat.refresh();
    }
}
