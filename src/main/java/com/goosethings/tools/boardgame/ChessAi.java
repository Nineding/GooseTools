package com.goosethings.tools.boardgame;

import java.util.Arrays;
import java.util.Comparator;

/** Bounded iterative deepening. Only completed iterations replace the legal fallback. */
public final class ChessAi {
    private static final int[] VALUES = {0, 20000, 500, 310, 330, 900, 100, 350, 180};
    private final long deadline;
    private ChessAi(long millis) { deadline = System.nanoTime() + Math.max(1, millis) * 1_000_000; }
    private void time() { if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) throw new SearchEnded(); }
    private static final class SearchEnded extends RuntimeException { @Override public synchronized Throwable fillInStackTrace() { return this; } }
    public static int choose(ChessPosition position, int difficulty, long millis) {
        ChessAi search = new ChessAi(millis);
        int[] moves = position.legalMoves(); if (moves.length == 0) return -1;
        int best = moves[0];
        try {
            for (int depth = 1; depth <= (difficulty == 0 ? 2 : difficulty == 1 ? 3 : 6); depth++) {
                int iterationBest = best, score = -1000000;
                for (int move : ordered(position, best)) {
                    search.time(); ChessPosition next = position.copy(); next.play(move);
                    int value = -search.search(next, depth - 1, -1000000, -score, 1);
                    if (value > score) { score = value; iterationBest = move; }
                }
                best = iterationBest;
                if (score >= 90000) break;
            }
        } catch (SearchEnded ignored) { }
        return best;
    }
    private int search(ChessPosition p, int depth, int alpha, int beta, int ply) {
        time();
        if (p.outcome() != 0) return p.outcome() == 3 ? 0 : p.outcome() == p.turn() ? 100000 - ply : -100000 + ply;
        if (depth == 0) return evaluate(p);
        for (int move : ordered(p, -1)) {
            ChessPosition child = p.copy(); child.play(move);
            int score = -search(child, depth - 1, -beta, -alpha, ply + 1);
            if (score >= beta) return score;
            alpha = Math.max(alpha, score);
        }
        return alpha;
    }
    private static int[] ordered(ChessPosition p, int preferred) {
        int[] board = p.cells();
        return Arrays.stream(p.legalMoves()).boxed().sorted(Comparator.comparingInt((Integer m) ->
                m == preferred ? 100000 : VALUES[Math.abs(board[ChessPosition.to(m)])] * 10 - VALUES[Math.abs(board[ChessPosition.from(m)])] + VALUES[ChessPosition.promotion(m)]).reversed()).mapToInt(Integer::intValue).toArray();
    }
    private static int evaluate(ChessPosition p) {
        int score = 0; int[] board = p.cells();
        for (int i = 0; i < board.length; i++) if (board[i] != 0) {
            int side = ChessPosition.side(board[i]), type = Math.abs(board[i]);
            int advance = side == 1 ? p.height() - 1 - i / p.width() : i / p.width();
            int central = p.width() - Math.abs(2 * (i % p.width()) - p.width() + 1);
            int value = VALUES[type] + (type == ChessPosition.PAWN ? advance * 8 : type == ChessPosition.KNIGHT ? central * 4 : 0);
            score += side == p.turn() ? value : -value;
        }
        return score - (p.check() ? 30 : 0);
    }
}
