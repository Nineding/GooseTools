package com.goosethings.tools.presence;

import java.util.Set;

/** Coarse, spoiler-free game phase published to optional client integrations. */
public enum GamePresencePhase {
    CONNECTED(0, "connected"),
    LOBBY(1, "lobby"),
    PREPARING(2, "preparing"),
    PLAYING(3, "playing"),
    MEETING(4, "meeting"),
    SPECTATING(5, "spectating"),
    RESULTS(6, "results"),
    TUTORIAL(7, "tutorial");

    private final int code;
    private final String id;

    GamePresencePhase(int code, String id) {
        this.code = code;
        this.id = id;
    }

    public int code() {
        return code;
    }

    public String id() {
        return id;
    }

    public String stateTranslationKey() {
        return "presence.goosetools.state." + id;
    }

    public String activityTranslationKey() {
        return "presence.goosetools.activity." + id;
    }

    public static GamePresencePhase fromCode(int code) {
        for (GamePresencePhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        return CONNECTED;
    }

    public static GamePresencePhase resolve(Set<String> tags, int meetingPhase) {
        if (tags.contains("inTutorial")) {
            return TUTORIAL;
        }
        if (tags.contains("endGame")) {
            return RESULTS;
        }

        boolean inGame = tags.contains("gamingGGD");
        if (inGame && tags.contains("lobby")) {
            return PREPARING;
        }
        if (tags.contains("lobby")) {
            return LOBBY;
        }
        if (inGame && meetingPhase > 0) {
            return MEETING;
        }
        if (inGame && tags.contains("spectator")) {
            return SPECTATING;
        }
        if (inGame) {
            return PLAYING;
        }
        return CONNECTED;
    }
}
