package com.goosethings.tools.client.game;

import com.goosethings.tools.game.GameSession;
import com.goosethings.tools.game.GameSnapshot;
import com.goosethings.tools.game.GameType;
import java.util.ArrayList;
import java.util.List;

/** Selects audio from accepted public snapshots without changing the game protocol. */
public final class GameSoundCues {
    public enum Cue {
        UI_CLICK, START, PAUSE, RESUME, FLAP, SCORE, EAT, PONG_HIT, WHACK_HIT,
        MISS, REVEAL, FLAG, SLIDE, MERGE, CRASH, WIN, LANE, BRAKE;
        public String path() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private GameSoundCues() {}

    public static List<Cue> between(GameType type, GameSnapshot before, GameSnapshot after) {
        if (before == null) return List.of();
        if (before.phase() != after.phase()) {
            return switch (after.phase()) {
                case GameSession.MENU -> List.of(Cue.UI_CLICK);
                case GameSession.PAUSED -> List.of(Cue.PAUSE);
                case GameSession.RUNNING -> List.of(before.phase() == GameSession.PAUSED
                        || before.phase() == GameSession.WON && type == GameType.MERGE ? Cue.RESUME : Cue.START);
                case GameSession.WON -> List.of(Cue.WIN);
                case GameSession.LOST -> List.of(type == GameType.FLAPPY || type == GameType.SNAKE
                        || type == GameType.MINES || type == GameType.TRAFFIC ? Cue.CRASH : Cue.MISS);
                default -> List.of();
            };
        }
        if (after.phase() == GameSession.MENU) {
            return before.mode() != after.mode() || before.difficulty() != after.difficulty()
                    ? List.of(Cue.UI_CLICK) : List.of();
        }
        if (after.phase() != GameSession.RUNNING || after.clock() < before.clock()) return List.of();
        List<Cue> cues = new ArrayList<>(3);
        if (after.event() > before.event()) {
            Cue cue = switch (after.effect()) {
                // Flaps are predicted locally; the server echo must stay silent.
                case 1 -> null;
                case 2 -> type == GameType.SNAKE ? Cue.EAT
                        : type == GameType.PONG && after.opponent() > before.opponent() ? Cue.MISS : Cue.SCORE;
                case 3 -> type == GameType.PONG ? Cue.PONG_HIT
                        : type == GameType.MINES && after.score() == before.score() ? Cue.FLAG : Cue.REVEAL;
                case 4 -> Cue.WHACK_HIT;
                case 5 -> Cue.MISS;
                case 6 -> Cue.WIN;
                case 7 -> after.score() > before.score() ? Cue.MERGE : Cue.SLIDE;
                default -> null;
            };
            if (cue != null) cues.add(cue);
        }
        if (type == GameType.TRAFFIC) {
            double[] a = before.actors(), b = after.actors();
            if (a.length >= 5 && b.length >= 5) {
                if (a[1] != b[1]) cues.add(Cue.LANE);
                if (a[4] == 0 && b[4] > 0) cues.add(Cue.BRAKE);
            }
        }
        return List.copyOf(cues);
    }
}
