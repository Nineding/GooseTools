package com.goosethings.tools.boardgame;

import com.goosethings.tools.gomoku.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.goosethings.tools.boardgame.ChessPosition.*;

class ChessRulesTest {
    private static int m(int from, int to) { return move(from, to, 0); }
    private static boolean legal(ChessPosition p, int from, int to) { return Arrays.stream(p.legalMoves()).anyMatch(v -> v == m(from, to)); }
    private static long perft(ChessPosition p, int depth) {
        if (depth == 1) return p.legalMoves().length;
        long count = 0;
        for (int move : p.legalMoves()) { ChessPosition child = p.copy(); assertTrue(child.play(move)); count += perft(child, depth - 1); }
        return count;
    }
    @Test void openingMoveTreesAndCopies() {
        ChessPosition chess = new ChessPosition(CHESS), xiangqi = new ChessPosition(XIANGQI);
        assertEquals(20, perft(chess, 1)); assertEquals(400, perft(chess, 2)); assertEquals(8902, perft(chess, 3));
        assertEquals(44, perft(xiangqi, 1)); assertEquals(1920, perft(xiangqi, 2));
        int[] cells = chess.cells(); cells[0] = 0; assertEquals(-ROOK, chess.cells()[0]);
    }
    @Test void castleRequiresRightsClearSafeTransitAndRook() {
        var p = fen(CHESS, "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertTrue(legal(p,60,62)); assertTrue(legal(p,60,58)); assertTrue(p.play(m(60,62)));
        assertEquals(ROOK,p.cells()[61]); assertEquals(0,p.cells()[63]);
        assertFalse(legal(fen(CHESS,"4kr2/8/8/8/8/8/8/4K2R w K - 0 1"),60,62));
        assertFalse(legal(fen(CHESS,"4k3/8/8/8/8/8/8/4K3 w K - 0 1"),60,62));
        assertFalse(legal(fen(CHESS,"4k3/8/8/8/8/8/8/4K2R w - - 0 1"),60,62));
    }
    @Test void enPassantExpiresAndCannotExposeKing() {
        var p=fen(CHESS,"4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1");
        assertTrue(p.play(m(11,27))); assertTrue(legal(p,28,19)); assertTrue(p.play(m(28,19)));
        assertEquals(0,p.cells()[27]); assertEquals(PAWN,p.cells()[19]);
        var pinned=fen(CHESS,"k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"); assertFalse(legal(pinned,28,19));
        var expired=fen(CHESS,"4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        assertTrue(expired.play(m(60,61))); assertTrue(expired.play(m(4,5))); assertFalse(legal(expired,28,19));
    }
    @Test void promotionOffersExactlyFourChoicesAndNoUnpromotedMove() {
        var p=fen(CHESS,"7k/P7/8/8/8/8/8/7K w - - 0 1");
        assertEquals(4,Arrays.stream(p.legalMoves()).filter(v->from(v)==8&&to(v)==0).count());
        assertFalse(p.play(m(8,0))); assertFalse(p.play(move(8,0,KING))); assertTrue(p.play(move(8,0,KNIGHT)));
        assertEquals(KNIGHT,p.cells()[0]); assertEquals(3,p.outcome());
    }
    @Test void mateStalemateAndAutomaticMoveLimit() {
        var mate=fen(CHESS,"7k/8/5KQ1/8/8/8/8/8 w - - 149 1");
        assertTrue(mate.play(m(22,14))); assertEquals(1,mate.outcome()); assertEquals(MATE,mate.reason());
        var stale=fen(CHESS,"7k/8/5K2/6Q1/8/8/8/8 w - - 0 1");
        assertTrue(stale.play(m(30,22))); assertEquals(3,stale.outcome()); assertEquals(STALEMATE,stale.reason());
        var limit=fen(CHESS,"7k/8/8/8/8/8/8/R6K w - - 149 1");
        assertTrue(limit.canClaimDraw(-1)); assertTrue(limit.play(m(56,48))); assertEquals(MOVE_LIMIT,limit.reason());
        var intended=fen(CHESS,"7k/8/8/8/8/8/8/R6K w - - 99 1");
        assertFalse(intended.canClaimDraw(-1)); assertTrue(intended.canClaimDraw(m(56,48))); assertFalse(intended.canClaimDraw(m(0,4)));
    }
    @Test void repetitionClaimsAndFivefoldAutomatic() {
        var p=new ChessPosition(CHESS); int[] cycle={m(62,45),m(6,21),m(45,62),m(21,6)};
        for(int n=0;n<4;n++) { for(int move:cycle)assertTrue(p.play(move));
            assertEquals(n==3?3:0,p.outcome()); if(n==1)assertTrue(p.canClaimDraw(-1)); }
        assertEquals(REPETITION,p.reason());
    }
    @Test void xiangqiHorseElephantPalaceCannonAndRiver() {
        var p=new ChessPosition(XIANGQI);
        assertTrue(legal(p,82,63)); assertTrue(legal(p,82,65));
        assertFalse(legal(p,82,75)); // blocked by back-rank elephant
        assertTrue(legal(p,83,63)); assertFalse(legal(p,83,47));
        assertTrue(legal(p,64,1)); assertFalse(legal(p,64,19)); // exactly one screen, not zero
        assertFalse(legal(p,54,55)); assertTrue(legal(p,54,45));
        assertTrue(legal(p,85,76));
    }
    @Test void xiangqiFacingKingsAndStalemateLoss() {
        var p=fen(XIANGQI,"4k4/9/9/9/4R4/9/9/9/9/4K4 w - - 0 1");
        assertFalse(legal(p,40,39)); assertTrue(legal(p,40,31));
        var stale=fen(XIANGQI,"4k4/5R3/3R5/9/9/9/9/9/4P4/4K4 w - - 0 1");
        assertTrue(stale.play(m(21,12))); assertEquals(1,stale.outcome());
    }
    @Test void xiangqiPerpetualCheckLosesInsteadOfDrawing() {
        var p=fen(XIANGQI,"4k4/3R5/9/9/9/9/9/9/9/3K5 w - - 0 1");
        assertTrue(p.play(m(12,13)));
        int[] cycle={m(4,5),m(13,14),m(5,4),m(14,13)};
        for(int n=0;n<5 && p.outcome()==0;n++) for(int move:cycle) { if(p.outcome()!=0)break; assertTrue(p.play(move),"move="+move); }
        assertEquals(2,p.outcome()); assertEquals(PERPETUAL,p.reason());
    }
    @Test void illegalEncodingTurnAndStaleAiCannotMutateMatches() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(); var group=new GomokuAccess.Group(1,7);
        for(int kind:new int[]{XIANGQI,CHESS}) {
            var game=new GomokuMatch(1,a,b,group,1,0,kind); int move=game.legalMoves()[0];
            assertFalse(game.move(b,0,1,move,1)); assertFalse(game.move(a,0,1,Integer.MAX_VALUE,1));
            assertTrue(game.move(a,1,1,move,59999)); assertFalse(game.move(b,1,1,game.legalMoves()[0],60000));
            assertFalse(game.move(b,2,game.revision(),game.legalMoves()[0],119999)); assertEquals(1,game.outcome());
        }
    }
    @Test void xiangqiPerpetualChaseTracksTheVictimAcrossSquares() {
        var p=fen(XIANGQI,"c3k4/9/9/1R7/4P4/9/9/9/9/4K4 w - - 0 1");
        int[] cycle={m(28,27),m(0,1),m(27,28),m(1,0)};
        for(int n=0;n<6&&p.outcome()==0;n++)for(int move:cycle){if(p.outcome()!=0)break;assertTrue(p.play(move));}
        assertEquals(2,p.outcome());assertEquals(PERPETUAL,p.reason());
    }
    @Test void xiangqiQuietRepetitionAndNoCaptureLimitDraw() {
        var p=fen(XIANGQI,"4k4/9/9/9/4P4/9/9/9/9/R3K4 w - - 0 1");
        int[] cycle={m(81,82),m(4,5),m(82,81),m(5,4)};
        for(int n=0;n<6&&p.outcome()==0;n++)for(int move:cycle){if(p.outcome()!=0)break;assertTrue(p.play(move));}
        assertEquals(3,p.outcome());assertEquals(REPETITION,p.reason());
        var limit=fen(XIANGQI,"4k4/9/9/9/4P4/9/9/9/9/R3K4 w - - 99 1");
        assertTrue(limit.play(m(81,82)));assertEquals(3,limit.outcome());assertEquals(MOVE_LIMIT,limit.reason());
    }
    @Test void invitationKindAndSharedSeatsSurviveRematch() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(); var group=new GomokuAccess.Group(2,7); var r=new GomokuRoom();
        r.update(a,group,0);r.update(b,group,0); assertEquals(GomokuRoom.Reply.OK,r.invite(a,b,0,CHESS));
        assertEquals(GomokuRoom.Reply.BUSY,r.invite(b,a,1,XIANGQI)); var invite=r.pending(a);
        assertEquals(GomokuRoom.Reply.OK,r.answer(b,invite.id(),true,true,2)); var g=r.match(a); assertEquals(CHESS,g.kind);
        assertEquals(GomokuRoom.Reply.BUSY,r.startAi(a,1,1,3,XIANGQI));g.forfeit(a,GomokuMatch.RESIGN,4);
        assertFalse(r.rematch(a,g.id,1,5));assertTrue(r.rematch(b,g.id,1,6));assertEquals(CHESS,r.match(a).kind);assertEquals(2,r.match(a).color(a));
    }
    @Test void allAiLevelsLegalBoundedAndImmediateMate() {
        for(int kind:new int[]{XIANGQI,CHESS})for(int difficulty=0;difficulty<3;difficulty++) {
            var p=new ChessPosition(kind);int d=difficulty;
            int chosen=assertTimeoutPreemptively(Duration.ofSeconds(3),()->ChessAi.choose(p,d,150));
            assertTrue(Arrays.stream(p.legalMoves()).anyMatch(v->v==chosen));assertEquals(-1,p.last());
        }
        var mate=fen(CHESS,"7k/8/5KQ1/8/8/8/8/8 w - - 0 1");
        for(int difficulty=0;difficulty<3;difficulty++) {var p=mate.copy();assertTrue(p.play(ChessAi.choose(p,difficulty,1000)));assertEquals(1,p.outcome());}
    }
}
