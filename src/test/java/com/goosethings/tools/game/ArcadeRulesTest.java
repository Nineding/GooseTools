package com.goosethings.tools.game;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeRulesTest {
    @Test void menusAndPausesDoNotCountAsPlayingAndLongSessionsHaveNoTimeout() {
        GameSession s = new GameSession(1, GameType.MERGE, 7, 0);
        s.tick(100_000); assertEquals(0, s.activeMillis());
        s.apply(0, GameSession.START, 0, 0, 0, 100_000);
        for (int i = 1; i <= 37_000; i++) s.tick(100_000 + i * 50L);
        assertEquals(GameSession.RUNNING, s.phase()); assertTrue(s.activeMillis() > 1_800_000);
        long active = s.activeMillis();
        s.apply(1, GameSession.PAUSE, 0, 0, 0, 1_950_000); s.tick(5_000_000); assertEquals(active, s.activeMillis());
        s.apply(2, GameSession.CONTINUE, 0, 0, 0, 5_000_000); s.tick(5_000_050); assertEquals(active + 50, s.activeMillis());
    }
    @Test void staleInputsInvalidCoordinatesAndRateFloodsAreRejected() {
        GameSession s = new GameSession(1, GameType.MERGE, 7, 0);
        assertTrue(s.apply(0, GameSession.START, 0, 0, 0, 0));
        assertFalse(s.apply(0, GameSession.PAUSE, 0, 0, 0, 0));
        assertFalse(s.apply(1, GameSession.PAUSE, 0, Double.NaN, 0, 0));
        assertFalse(s.apply(1, GameSession.PAUSE, 0, 601, 0, 0));
        for (int i = 1; i < 60; i++) s.apply(i, GameSession.DIRECTION, -1, 0, 0, 0);
        assertFalse(s.apply(60, GameSession.PAUSE, 0, 0, 0, 0)); assertEquals(GameSession.RUNNING, s.phase());
    }
    @Test void catchUpIsBoundedAndRestartDoesNotResetLifetimeActivity() {
        GameSession s = new GameSession(1, GameType.MERGE, 7, 0); s.apply(0, GameSession.START, 0, 0, 0, 0);
        s.tick(100_000); assertEquals(250, s.activeMillis());
        s.apply(1, GameSession.RETRY, 0, 0, 0, 100_000);
        assertEquals(GameSession.MENU, s.phase()); assertEquals(0, s.activeMillis()); assertEquals(250, s.totalActiveMillis());
    }
    @Test void snapshotRejectsOversizedOrNonfiniteDataAndKeepsCopies() {
        GameSession s = new GameSession(1, GameType.MERGE, 7, 0); GameSnapshot state = s.snapshot(0, 0);
        int[] b = state.board(); b[0] = 62; assertFalse(Arrays.equals(b, state.board()));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot(1,0,0,30,20,0,0,0,0,0,0,0,-1,0,0,0,new int[600],new double[0],new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot(1,0,0,0,0,0,0,0,0,0,0,0,-1,0,0,0,new int[0],new double[]{Double.NaN},new int[0]));
    }
    @Test void flappyFlapsScorePipesAndCollisionsEndTheRun() {
        FlappyGame f = new FlappyGame(9,0,1);
        assertTrue(f.input(GameSession.FLAP,0,0,0)); assertFalse(f.input(GameSession.FLAP,0,0,0)); assertEquals(-155, f.vy);
        for (int i = 0; i < 1000 && f.phase == GameSession.RUNNING; i++) f.tick(10);
        assertEquals(GameSession.LOST, f.phase);
        FlappyGame scoring = new FlappyGame(9,0,1);
        // Stabilize the bird inside the current gap to isolate scoring and bounded recycling.
        for (int i = 0; i < 30_000; i++) {
            double[] a = scoring.actors(); double gap = a[3];
            for (int p = 0; p < 3; p++) if (a[2+p*2]+34 >= 38) { gap = a[3+p*2]; break; }
            scoring.y = gap; scoring.vy = 0; scoring.tick(10);
            assertEquals(GameSession.RUNNING, scoring.phase); assertEquals(8, scoring.actors().length);
        }
        assertTrue(scoring.score > 100); // No ten-pipe task limit and no ever-growing obstacle list.
    }
    @Test void snakeRejectsReverseAndQueuesAtMostTwoTurns() {
        SnakeGame s = new SnakeGame(4,0,1);
        assertFalse(s.input(GameSession.DIRECTION,3,0,0));
        assertTrue(s.input(GameSession.DIRECTION,0,0,0)); assertFalse(s.input(GameSession.DIRECTION,2,0,0));
        assertTrue(s.input(GameSession.DIRECTION,3,0,0)); assertFalse(s.input(GameSession.DIRECTION,2,0,0));
        for (int i = 0; i < 14; i++) s.tick(10);
        assertEquals(0,s.direction); for (int i = 0; i < 14; i++) s.tick(10); assertEquals(3,s.direction);
    }
    @Test void snakeGrowsWithoutTenFoodWinAndCanEnterItsDepartingTail() {
        SnakeGame s = new SnakeGame(4,0,2); s.score = 10;
        s.food = s.snake.getFirst()+1; for (int i=0;i<9;i++) s.tick(10);
        assertEquals(11,s.score); assertEquals(4,s.snake.size()); assertEquals(GameSession.RUNNING,s.phase);
        assertFalse(s.snake.contains(s.food));
        s.snake.clear(); s.snake.addAll(Arrays.asList(21,22,42,41)); s.direction=2; s.food=99;
        for (int i=0;i<9;i++) s.tick(10); assertEquals(GameSession.RUNNING,s.phase); assertEquals(41,s.snake.getFirst());
    }
    @Test void snakeDiesAtWallAndWinsOnlyWhenGridIsFull() {
        SnakeGame s = new SnakeGame(4,0,2); s.snake.clear(); s.snake.add(0); s.direction=3;
        for(int i=0;i<9;i++) s.tick(10); assertEquals(GameSession.LOST,s.phase);
        SnakeGame full = new SnakeGame(4,0,2); full.snake.clear(); full.snake.add(298);
        for(int i=0;i<298;i++) full.snake.add(i);
        full.food=299; full.direction=1; for(int i=0;i<9;i++) full.tick(10);
        assertEquals(GameSession.WON,full.phase); assertEquals(300,full.snake.size());
    }
    @Test void pongPaddleAngleAndMissesUseActualPhysics() {
        PongGame p = new PongGame(4,0,1); p.clock=1000; p.x=24; p.y=132; p.vx=-180; p.vy=0;
        p.tick(10); assertTrue(p.vx>0); assertTrue(p.vy>0);
        p.x=24;p.y=20;p.vx=-200;p.vy=0; for(int i=0;i<20;i++) p.tick(10);
        assertEquals(1,p.opponent);
    }
    @Test void pongClassicWinsAtElevenAndEndlessContinuesPastEleven() {
        for(int mode=0;mode<2;mode++) {
            PongGame p = new PongGame(4,mode,1); p.score=10;p.clock=1000;p.x=405;p.vx=200;p.tick(10);
            assertEquals(11,p.score); assertEquals(mode==0?GameSession.WON:GameSession.RUNNING,p.phase);
        }
        PongGame p = new PongGame(4,0,1); p.opponent=10;p.clock=1000;p.x=-5;p.vx=-200;p.tick(10); assertEquals(GameSession.LOST,p.phase);
    }
    @Test void pongAiHasLimitedSpeedAndFiniteReaction() {
        PongGame p = new PongGame(4,0,0); p.clock=1000;p.y=30;p.vx=150;
        double start=p.right;p.tick(10); assertTrue(Math.abs(p.right-start)<=1.05+.001);
        p.input(GameSession.POINT,0,0,0);assertEquals(22,p.target);
    }
    @Test void whackHasThreeLivesNoThirtySecondLimitAndNoDoubleScoring() {
        WhackGame w=new WhackGame(4,0,1);
        for(int i=0;i<12_000 && w.phase==GameSession.RUNNING;i++) {
            w.tick(10);
            if(w.hole>=0) { int hole=w.hole;long before=w.score;w.input(GameSession.REVEAL,hole,0,0);assertEquals(before+1,w.score);assertFalse(w.input(GameSession.REVEAL,hole,0,0)); }
        }
        assertEquals(GameSession.RUNNING,w.phase);assertTrue(w.clock>=120_000);assertTrue(w.score>100);assertEquals(3,w.lives);
        w.tick(150);w.input(GameSession.REVEAL,8,0,0); assertEquals(2,w.lives);
    }
    @Test void whackMissesAndEmptyClicksCanEndTheGame() {
        WhackGame w=new WhackGame(4,0,1);
        for(int i=0;i<1000&&w.phase==GameSession.RUNNING;i++) w.tick(10);
        assertEquals(0,w.lives);assertEquals(GameSession.LOST,w.phase);
    }
    @Test void minesGenerateCorrectCountsAndSafeFirstAreaForAllDifficulties() {
        for(int diff=0;diff<3;diff++) for(int seed=0;seed<100;seed++) {
            MinesGame m=new MinesGame(seed,0,diff);int first=m.cols/2+m.rows/2*m.cols;
            assertTrue(m.input(GameSession.REVEAL,first,0,0));
            int count=0;for(boolean mine:m.mines) if(mine) count++;assertEquals(new int[]{10,40,99}[diff],count);
            assertFalse(m.mines[first]);for(int c:m.neighbors(first)) assertFalse(m.mines[c]);
            int[] visible=m.board();for(int c=0;c<visible.length;c++) if(!m.open[c]) assertEquals(-1,visible[c]);
        }
    }
    @Test void minesFlagQuestionCycleDoesNotRevealHiddenCellsOrWin() {
        MinesGame m=new MinesGame(9,0,0);
        m.input(GameSession.FLAG,0,0,0);assertEquals(-2,m.board()[0]);assertFalse(m.input(GameSession.REVEAL,0,0,0));
        m.input(GameSession.FLAG,0,0,0);assertEquals(9,m.board()[0]);m.input(GameSession.FLAG,0,0,0);assertEquals(-1,m.board()[0]);assertFalse(m.planted);
    }
    @Test void minesRevealAllSafeWinsAndWrongFlagsCanDetonateChord() {
        MinesGame m=new MinesGame(13,0,0);m.input(GameSession.REVEAL,40,0,0);
        for(int i=0;i<m.mines.length;i++) if(!m.mines[i]&&!m.open[i]) m.input(GameSession.REVEAL,i,0,0);
        assertEquals(GameSession.WON,m.phase);
        MinesGame wrong=new MinesGame(13,0,0);wrong.input(GameSession.REVEAL,40,0,0);
        boolean found=false;
        for(int i=0;i<wrong.open.length&&!found;i++) if(wrong.open[i]&&wrong.adjacent(i)>0) {
            int need=wrong.adjacent(i), placed=0;
            for(int c:wrong.neighbors(i)) if(!wrong.open[c]&&!wrong.mines[c]&&placed<need) { wrong.flags[c]=true;placed++; }
            if(placed==need) {wrong.input(GameSession.CHORD,i,0,0);found=true;}
            else Arrays.fill(wrong.flags,false);
        }
        assertTrue(found);assertEquals(GameSession.LOST,wrong.phase);assertTrue(Arrays.stream(wrong.board()).anyMatch(v->v==-4));
    }
    @Test void minesContinuousBoardsCountWinsAndResetOnlyCurrentTime() {
        GameSession s=new GameSession(1,GameType.MINES,13,0);s.apply(0,GameSession.START,0,0,0,0);
        MinesGame m=(MinesGame)s.game;m.input(GameSession.REVEAL,40,0,0);
        for(int c=0;c<m.mines.length;c++) if(!m.mines[c]&&!m.open[c]) m.input(GameSession.REVEAL,c,0,0);
        s.tick(50);assertEquals(GameSession.WON,s.phase());s.apply(1,GameSession.NEXT,0,0,0,50);
        assertEquals(1,s.snapshot(0,0).wins());assertEquals(GameSession.RUNNING,s.phase());assertEquals(0,s.activeMillis());assertEquals(10,s.totalActiveMillis());
    }
    @Test void mergeMovesInFourDirectionsAndOnlyMergesEachTileOnce() {
        for(int dir=0;dir<4;dir++) {
            MergeGame m=new MergeGame(3,0,1);Arrays.fill(m.cells,0);
            for(int slot=0;slot<4;slot++) m.cells[MergeGame.index(dir,0,slot)]=1;
            assertTrue(m.input(GameSession.DIRECTION,dir,0,0));
            assertEquals(2,m.cells[MergeGame.index(dir,0,0)]);assertEquals(2,m.cells[MergeGame.index(dir,0,1)]);assertEquals(8,m.score);
        }
    }
    @Test void mergeInvalidMovesDoNotSpawnOrConsumeRandomState() {
        MergeGame m=new MergeGame(3,0,1);Arrays.fill(m.cells,0);m.cells[0]=1;
        int[] before=m.board();assertFalse(m.input(GameSession.DIRECTION,0,0,0));assertArrayEquals(before,m.board());assertEquals(0,m.event);
    }
    @Test void merge2048WinsAndCanContinueWithoutRepeatedWin() {
        MergeGame m=new MergeGame(3,0,1);Arrays.fill(m.cells,0);m.cells[0]=m.cells[1]=10;
        assertTrue(m.input(GameSession.DIRECTION,3,0,0));assertEquals(11,m.cells[0]);assertEquals(2048,m.score);assertEquals(GameSession.WON,m.phase);
        m.continuePlaying();assertEquals(GameSession.RUNNING,m.phase);m.clock=200;
        m.input(GameSession.DIRECTION,2,0,0);assertEquals(GameSession.RUNNING,m.phase);
        MergeGame endless=new MergeGame(3,1,1);Arrays.fill(endless.cells,0);endless.cells[0]=endless.cells[1]=10;endless.input(GameSession.DIRECTION,3,0,0);assertEquals(GameSession.RUNNING,endless.phase);
    }
    @Test void mergeFullBoardWithNoAdjacentPairIsLostAndValidMoveSpawnsOne() {
        MergeGame m=new MergeGame(3,1,1);
        int[] board={1,1,3,4,5,6,7,8,9,10,12,13,14,15,16,17};System.arraycopy(board,0,m.cells,0,16);
        m.input(GameSession.DIRECTION,3,0,0);assertFalse(m.legalMove());assertEquals(GameSession.LOST,m.phase);
    }
    @Test void mergeSpawnDistributionMatchesClassicTwoAndFour() {
        int fours=0,tiles=0;
        for(int seed=0;seed<1000;seed++) for(int value:new MergeGame(seed,0,1).cells) if(value!=0){tiles++;if(value==2)fours++;else assertEquals(1,value);}
        assertEquals(2000,tiles);assertTrue(fours>140&&fours<270,"fours="+fours);
    }
    @Test void largeScoresSaturateWithoutOverflowAndSnapshotsStayBounded() {
        assertEquals(Long.MAX_VALUE,ArcadeGame.add(Long.MAX_VALUE-1,2));
        for(GameType type:GameType.values()) {GameSession s=new GameSession(1,type,9,0);GameSnapshot state=s.snapshot(0,0);assertTrue(state.board().length<=480);assertTrue(state.actors().length<=64);assertTrue(state.moves().length<=128);}
    }
}
