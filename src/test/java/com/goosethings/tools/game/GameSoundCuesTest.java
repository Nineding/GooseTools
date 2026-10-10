package com.goosethings.tools.game;

import com.goosethings.tools.client.game.GameSoundCues;
import com.goosethings.tools.client.game.GameSoundCues.Cue;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameSoundCuesTest {
    private static GameSnapshot state(GameSession s) { return s.snapshot(0, 0); }
    @Test void allGamesHaveLifecycleCuesWithoutReplayingTheirOldEffectOnRetry() {
        for (GameType type : GameType.values()) {
            GameSession s = new GameSession(1, type, 42, 0);
            GameSnapshot menu = state(s);
            assertEquals(List.of(), GameSoundCues.between(type, null, menu));
            s.apply(0, GameSession.START, 0, 0, 0, 0);
            GameSnapshot playing = state(s);
            assertEquals(List.of(Cue.START), GameSoundCues.between(type, menu, playing));
            s.apply(1, GameSession.PAUSE, 0, 0, 0, 0);
            GameSnapshot paused = state(s);
            assertEquals(List.of(Cue.PAUSE), GameSoundCues.between(type, playing, paused));
            s.apply(2, GameSession.CONTINUE, 0, 0, 0, 0);
            assertEquals(List.of(Cue.RESUME), GameSoundCues.between(type, paused, state(s)));
            playing = state(s); s.game.lose(); s.tick(10);
            GameSnapshot lost = state(s);
            assertEquals(1, GameSoundCues.between(type, playing, lost).size());
            assertEquals(List.of(), GameSoundCues.between(type, lost, lost));
            s.apply(3, GameSession.RETRY, 0, 0, 0, 10);
            assertEquals(List.of(Cue.UI_CLICK), GameSoundCues.between(type, lost, state(s)));
        }
    }
    @Test void predictedFlapIsNotPlayedAgainWhenTheServerConfirmsIt() {
        GameSession s = new GameSession(1, GameType.FLAPPY, 42, 0);
        s.apply(0, GameSession.START, 0, 0, 0, 0); GameSnapshot before = state(s);
        assertTrue(s.apply(1, GameSession.FLAP, 0, 0, 0, 0));
        assertTrue(state(s).event() > before.event());
        assertEquals(List.of(), GameSoundCues.between(s.type, before, state(s)));
    }
    @Test void minesFlagCyclingAndFloodRevealUseDifferentCues() {
        GameSession s = new GameSession(1, GameType.MINES, 42, 0);
        s.apply(0, GameSession.START, 0, 0, 0, 0);
        for (int i = 1; i <= 3; i++) {
            GameSnapshot before = state(s); s.apply(i, GameSession.FLAG, 0, 0, 0, 0);
            assertEquals(List.of(Cue.FLAG), GameSoundCues.between(s.type, before, state(s)));
        }
        GameSnapshot before = state(s); s.apply(4, GameSession.REVEAL, 40, 0, 0, 0);
        assertEquals(List.of(Cue.REVEAL), GameSoundCues.between(s.type, before, state(s)));
    }
    @Test void mergingAndMovingWithoutMergingAreAudiblyDifferent() {
        GameSession s = new GameSession(1, GameType.MERGE, 42, 0);
        s.apply(0, GameSession.START, 0, 0, 0, 0);
        MergeGame game = (MergeGame)s.game; Arrays.fill(game.cells, 0); game.cells[0] = 1;
        GameSnapshot before = state(s); s.apply(1, GameSession.DIRECTION, 1, 0, 0, 0);
        assertEquals(List.of(Cue.SLIDE), GameSoundCues.between(s.type, before, state(s)));
        s.tick(100); Arrays.fill(game.cells, 0); game.cells[0] = game.cells[1] = 1;
        before = state(s); s.apply(2, GameSession.DIRECTION, 3, 0, 0, 100);
        assertEquals(List.of(Cue.MERGE), GameSoundCues.between(s.type, before, state(s)));
        before = state(s); assertFalse(s.apply(3, GameSession.DIRECTION, -1, 0, 0, 100));
        assertEquals(List.of(), GameSoundCues.between(s.type, before, state(s)));
    }
    @Test void trafficHeldBrakesAndBoundaryTurnsDoNotSpamAudio() {
        GameSession s = new GameSession(1, GameType.TRAFFIC, 42, 0);
        s.apply(0, GameSession.START, 0, 0, 0, 0); GameSnapshot before = state(s);
        s.apply(1, GameSession.POINT, 0, 0, 0, 0);
        assertEquals(List.of(Cue.BRAKE), GameSoundCues.between(s.type, before, state(s)));
        before = state(s); s.apply(2, GameSession.POINT, 0, 0, 0, 0);
        assertEquals(List.of(), GameSoundCues.between(s.type, before, state(s)));
        before = state(s); s.apply(3, GameSession.DIRECTION, 3, 0, 0, 0);
        assertEquals(List.of(Cue.LANE), GameSoundCues.between(s.type, before, state(s)));
        before = state(s); s.apply(4, GameSession.DIRECTION, 3, 0, 0, 200);
        assertEquals(List.of(), GameSoundCues.between(s.type, before, state(s)));
    }
    @Test void pongLosingAPointDoesNotSoundLikeScoring() {
        GameSession s = new GameSession(1, GameType.PONG, 42, 0);
        s.apply(0, GameSession.START, 0, 0, 0, 0); s.tick(250); s.tick(500); s.tick(750); s.tick(1000);
        PongGame pong = (PongGame)s.game;
        pong.x = -5; pong.vx = -145; GameSnapshot before = state(s); s.tick(1010);
        assertEquals(List.of(Cue.MISS), GameSoundCues.between(s.type, before, state(s)));
        s.tick(1260); s.tick(1510); s.tick(1760); s.tick(2010);
        pong.x = 405; pong.vx = 145; before = state(s); s.tick(2020);
        assertEquals(List.of(Cue.SCORE), GameSoundCues.between(s.type, before, state(s)));
    }
}
