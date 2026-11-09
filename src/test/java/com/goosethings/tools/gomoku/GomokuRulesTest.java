package com.goosethings.tools.gomoku;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GomokuRulesTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
    private final GomokuAccess.Group lobby = new GomokuAccess.Group(1, 4), ghosts = new GomokuAccess.Group(2, 4);
    private GomokuMatch game() { return new GomokuMatch(1, a, b, lobby, 1, 0); }
    private GomokuRoom room() { var r = new GomokuRoom(); r.update(a, lobby, 0); r.update(b, lobby, 0); r.update(c, lobby, 0); return r; }
    private GomokuMatch pair(GomokuRoom r) { assertEquals(GomokuRoom.Reply.OK, r.invite(a, b, 0)); var i = r.pending(a); assertEquals(GomokuRoom.Reply.OK, r.answer(b, i.id(), true, true, 1)); return r.match(a); }

    @Test void allFourAxesWinIncludingEdges() {
        for (int[] cells : new int[][]{{0,1,2,3,4},{14,29,44,59,74},{0,16,32,48,64},{14,28,42,56,70}}) {
            var board = new GomokuBoard();
            for (int i=0;i<5;i++) { assertTrue(board.place(cells[i], 1)); assertEquals(i==4?1:0, board.winner()); }
            assertEquals(5,board.line().length); assertFalse(board.place(200,2));
        }
    }
    @Test void overlineWinsAndEdgesNeverWrap() {
        var board=new GomokuBoard(); for(int i:new int[]{0,1,2,4,5})assertTrue(board.place(i,2));
        assertTrue(board.place(3,2)); assertEquals(2,board.winner()); assertEquals(6,board.line().length);
        var edge=new GomokuBoard(); for(int i:new int[]{12,13,14,15,16})edge.place(i,1); assertEquals(0,edge.winner());
    }
    @Test void fullBoardDrawAndInvalidStones() {
        var board=new GomokuBoard();assertFalse(board.place(-1,1));assertFalse(board.place(225,1));assertFalse(board.place(0,3));
        for(int y=0;y<15;y++)for(int x=0;x<15;x++)assertTrue(board.place(y*15+x,1+((x+2*y)%4)/2));
        assertTrue(board.full());assertEquals(0,board.winner());assertFalse(board.place(0,2));
        int[] copy=board.cells();copy[0]=0;assertFalse(board.empty(0));
    }
    @Test void sharedTurnsSequenceRevisionAndDeadlineAreAuthoritative() {
        var g=game(); assertFalse(g.move(c,0,1,112,0)); assertFalse(g.move(b,0,1,112,0));
        assertTrue(g.move(a,0,1,112,59_999)); assertEquals(2,g.turn());assertEquals(60_000,g.remaining(59_999));
        assertFalse(g.move(b,0,g.revision(),113,60_000)); // already-consumed sequence
        assertFalse(g.move(b,1,1,113,60_000)); // stale position
        assertFalse(g.move(b,2,g.revision(),112,60_000));assertFalse(g.move(b,3,g.revision(),225,60_000));
        assertTrue(g.move(b,4,g.revision(),113,119_998)); assertEquals(1,g.turn());
        assertFalse(g.move(a,1,g.revision(),114,179_998));assertEquals(2,g.outcome());assertEquals(GomokuMatch.TIMEOUT,g.reason());
    }
    @Test void hidingHasNoClockSideEffectsAndResultsAreTerminal() {
        var g=game();g.expire(59_999);assertEquals(1,g.remaining(59_999));g.expire(60_000);
        assertEquals(2,g.outcome());g.forfeit(b,GomokuMatch.RESIGN,60_001);assertEquals(2,g.outcome());
        assertFalse(g.move(a,0,g.revision(),0,60_001));
    }
    @Test void invitationsAreExclusiveScopedAndRequireIntendedRecipient() {
        var r=room();r.update(c,ghosts,0);
        assertEquals(GomokuRoom.Reply.UNAVAILABLE,r.invite(a,c,0));assertEquals(GomokuRoom.Reply.INVALID,r.invite(a,a,0));
        assertEquals(GomokuRoom.Reply.OK,r.invite(a,b,0));var i=r.pending(a);
        assertEquals(GomokuRoom.Reply.BUSY,r.invite(a,b,1));assertEquals(GomokuRoom.Reply.INVALID,r.answer(c,i.id(),true,true,1));
        assertEquals(GomokuRoom.Reply.OK,r.answer(b,i.id(),true,true,1));assertSame(r.match(a),r.match(b));assertNull(r.pending(a));
        assertEquals(GomokuRoom.Reply.BUSY,r.startAi(a,1,1,1));
    }
    @Test void invitationDeclineExpiryAndBoundaryRecheck() {
        var r=room();r.invite(a,b,0);assertEquals(GomokuRoom.Reply.OK,r.answer(b,r.pending(b).id(),false,true,1));assertTrue(r.free(a));
        r.invite(a,b,2);long id=r.pending(b).id();assertEquals(GomokuRoom.Reply.EXPIRED,r.answer(b,id,true,true,30_002));assertTrue(r.free(a));
        r.invite(a,b,40_000);id=r.pending(a).id();r.update(b,ghosts,40_001);assertEquals(GomokuRoom.Reply.EXPIRED,r.answer(b,id,true,true,40_002));
    }
    @Test void rematchNeedsBothConsentThenSwapsColorsAndIds() {
        var r=room();var g=pair(r);g.forfeit(a,GomokuMatch.RESIGN,10);
        assertFalse(r.rematch(a,g.id,0,11));assertEquals(1,g.rematchMask());assertFalse(r.rematch(a,g.id,1,12));
        assertTrue(r.rematch(b,g.id,0,13));var next=r.match(a);assertNotEquals(g.id,next.id);assertEquals(b,next.black);assertEquals(a,next.white);
        assertEquals(0,next.board.moves());assertFalse(r.rematch(a,g.id,2,14));
    }
    @Test void rematchVoteExpiresAndDisconnectedOpponentCannotBeRecreated() {
        var r=room();var g=pair(r);g.forfeit(a,GomokuMatch.RESIGN,10);r.rematch(a,g.id,0,11);r.tick(30_011);assertEquals(0,g.rematchMask());
        r.disconnect(b,30_012);assertFalse(r.rematch(a,g.id,1,30_013));assertSame(g,r.match(a));
        r.tick(120_010);assertNull(r.match(a));assertTrue(r.games().isEmpty());
    }
    @Test void disconnectLosesButWorldBoundaryCancelsWithoutWinner() {
        var r=room();var g=pair(r);r.disconnect(a,10);assertEquals(2,g.outcome());assertEquals(GomokuMatch.DISCONNECT,g.reason());assertNull(r.match(a));assertSame(g,r.match(b));
        r=room();g=pair(r);r.update(a,new GomokuAccess.Group(1,5),10);assertEquals(GomokuMatch.CANCELLED,g.outcome());assertNull(r.match(a));assertNull(r.match(b));
    }
    @Test void concurrentGamesAndComputerRematchesAreIndependent() {
        var r=room();var g=pair(r);assertEquals(GomokuRoom.Reply.OK,r.startAi(c,2,2,10));var ai=r.match(c);
        assertNotEquals(ai.id,g.id);assertTrue(ai.aiTurn());assertTrue(ai.aiMove(ai.revision(),112,11));assertFalse(ai.aiMove(1,113,12));
        ai.forfeit(c,GomokuMatch.RESIGN,20);assertTrue(r.rematch(c,ai.id,0,21));assertEquals(c,r.match(c).black);assertSame(g,r.match(a));
    }
    @Test void realPlayerTagsExcludeLivingSwallowedAndTemporaryStates() {
        assertNotNull(GomokuAccess.resolve(Set.of("lobby"),0,-1,true));
        Set<String> dead=new HashSet<>(Set.of("players","gamingGGD","spectator","deadInMap"));
        assertNotNull(GomokuAccess.resolve(dead,4,4,true));dead.add("inTalk");assertNotNull(GomokuAccess.resolve(dead,4,4,true));
        dead.add("dlcGhostActive");assertNotNull(GomokuAccess.resolve(dead,4,4,true));
        for(String state:List.of("inPelican","inDream","spectatorDream","endGame","inTutorial","ggdStarting","ggdStartingReady","astralProjected")) {
            var blocked=new HashSet<>(dead);blocked.add(state);assertNull(GomokuAccess.resolve(blocked,4,4,true),state);
        }
        assertNull(GomokuAccess.resolve(dead,5,4,true));assertNull(GomokuAccess.resolve(dead,4,4,false));
        assertNull(GomokuAccess.resolve(Set.of("gamingGGD","players","deadInMap","inPelican"),4,4,true));
        assertNull(GomokuAccess.resolve(Set.of("lobby","gamingGGD"),4,4,true));
        assertNull(GomokuAccess.resolve(Set.of("players","gamingGGD"),4,4,true));
        assertNotNull(GomokuAccess.resolve(Set.of("gamingGGD","spectator","deadInMap","ggdLateSpectator"),4,4,true));
        assertNull(GomokuAccess.resolve(Set.of("lobby"),-1,-1,true));
    }
    @Test void allAiDifficultiesWinBlockAndReturnLegalMovesWithoutChangingInput() {
        for(int d=0;d<3;d++) {
            int[] board=new int[225];board[105]=2;for(int i=106;i<110;i++)board[i]=1;
            assertEquals(110,GomokuAi.choose(board,1,d,50));assertEquals(110,GomokuAi.choose(board,2,d,50));
            board=new int[225];board[112]=1;int[] copy=board.clone();int move=GomokuAi.choose(board,2,d,50);
            assertTrue(move>=0&&move<225);assertEquals(0,board[move]);assertArrayEquals(copy,board);
            assertEquals(112,GomokuAi.choose(new int[225],1,d,1));
        }
    }
    @Test void searchBudgetAndInterruptAreBounded() {
        int[] board=new int[225];board[112]=1;board[113]=2;board[97]=1;
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),()->assertTrue(GomokuAi.choose(board,2,2,20)>=0));
        Thread.currentThread().interrupt();try {assertTrue(GomokuAi.choose(board,2,2,100)>=0);}finally{Thread.interrupted();}
        Arrays.fill(board,1);assertEquals(-1,GomokuAi.choose(board,2,2,20));
    }
}
