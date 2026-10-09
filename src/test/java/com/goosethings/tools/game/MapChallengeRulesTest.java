package com.goosethings.tools.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapChallengeRulesTest {
    private GameSession driving() {
        GameSession s = new GameSession(1, GameType.TRAFFIC, 42, 0); s.startChallenge(0);
        ((TrafficGame)s.game).nextWave = 1_000_000;
        return s;
    }
    @Test void taskGamesStartInHardModeAndCannotChangeDifficultyOrRestart() {
        for (GameType type : new GameType[]{GameType.TRAFFIC, GameType.WHACK}) {
            GameSession s = new GameSession(1, type, 42, 0); s.startChallenge(0);
            assertEquals(GameSession.RUNNING, s.phase()); assertEquals(2, s.difficulty());
            assertFalse(s.apply(0, GameSession.OPTIONS, 0, 0, 0, 0));
            assertFalse(s.apply(1, GameSession.RETRY, 0, 0, 0, 0));
            assertEquals(2, s.difficulty()); assertEquals(GameSession.RUNNING, s.phase());
        }
    }
    @Test void survivalCompletesAtThirtySecondsAndStopsBeforeFurtherSimulation() {
        GameSession s = driving();
        for (int n=1; n<=2999; n++) s.tick(n*10L);
        assertFalse(s.challengeComplete()); assertEquals(29_990, s.activeMillis());
        s.tick(30_240);
        assertTrue(s.challengeComplete()); assertEquals(GameSession.WON, s.phase());
        assertEquals(30_000, s.activeMillis()); s.tick(100_000); assertEquals(30_000, s.activeMillis());
    }
    @Test void collisionBeforeTargetCannotBecomeSuccessFromLargeTick() {
        GameSession s=driving(); for(int n=1;n<=2998;n++) s.tick(n*10L);
        TrafficGame g=(TrafficGame)s.game; g.obstacles.add(new TrafficGame.Obstacle(1,230,0));
        s.tick(30_200); assertEquals(GameSession.LOST,s.phase()); assertFalse(s.challengeComplete());
        assertEquals(29_990,s.activeMillis());
    }
    @Test void pauseDoesNotCountAndResumesSameHardRun() {
        GameSession s=driving(); s.tick(250); s.apply(0,GameSession.PAUSE,0,0,0,250);
        s.tick(100_000); assertEquals(250,s.activeMillis());
        s.apply(1,GameSession.CONTINUE,0,0,0,100_000); s.tick(100_010);
        assertEquals(260,s.activeMillis()); assertEquals(2,s.difficulty());
    }
    @Test void twentiethActualMoleHitCompletesAndDuplicateHitsDoNotCount() {
        GameSession s=new GameSession(1,GameType.WHACK,42,0); s.startChallenge(0);
        WhackGame g=(WhackGame)s.game;
        int seq=0; long now=0;
        for(int hit=1;hit<=20;hit++) {
            while(g.hole<0){now+=10;s.tick(now);}
            int hole=g.hole;
            assertTrue(s.apply(seq++,GameSession.REVEAL,hole,0,0,now));
            assertFalse(s.apply(seq++,GameSession.REVEAL,hole,0,0,now));
            assertEquals(hit,s.score());
            if(hit<20)assertFalse(s.challengeComplete());
        }
        assertTrue(s.challengeComplete());assertEquals(GameSession.WON,s.phase());assertEquals(3,g.lives);
    }
    @Test void LosingThreeLivesBeforeTwentyFails() {
        GameSession s=new GameSession(1,GameType.WHACK,42,0);s.startChallenge(0);
        for(int n=1;n<1500&&s.phase()!=GameSession.LOST;n++)s.tick(n*10L);
        assertEquals(GameSession.LOST,s.phase());assertFalse(s.challengeComplete());assertEquals(0,s.score());
    }
    @Test void arcadeDeltaCursorCountsEachMillisecondOnceAcrossReplay() {
        GameSession s=new GameSession(1,GameType.MERGE,42,0);
        s.apply(0,GameSession.START,0,0,0,0);s.tick(250);
        assertEquals(250,s.takeTaskActiveDelta());assertEquals(0,s.takeTaskActiveDelta());
        s.apply(1,GameSession.RETRY,0,0,0,250);s.apply(2,GameSession.START,0,0,0,250);s.tick(500);
        assertEquals(250,s.takeTaskActiveDelta());assertEquals(0,s.takeTaskActiveDelta());
    }
    @Test void sessionsDoNotShareProgressOrOutcomes() {
        GameSession a=driving(),b=driving();
        for(int n=1;n<=120;n++)a.tick(n*250L);
        assertTrue(a.challengeComplete());assertFalse(b.challengeComplete());assertEquals(0,b.activeMillis());
    }
}
