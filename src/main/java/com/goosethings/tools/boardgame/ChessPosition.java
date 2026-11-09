package com.goosethings.tools.boardgame;

import java.util.*;

/** Pure rules, with signed pieces (positive = red/white), row zero at black's back rank.
 * No Minecraft objects or clocks enter search copies. Moves carry the promotion choice. */
public final class ChessPosition {
    public static final int XIANGQI = 1, CHESS = 2;
    public static final int KING = 1, ROOK = 2, KNIGHT = 3, BISHOP = 4, QUEEN = 5, PAWN = 6, CANNON = 7, ADVISOR = 8;
    public static final int MATE = 6, STALEMATE = 7, REPETITION = 8, MOVE_LIMIT = 9, DEAD = 10, PERPETUAL = 11;
    private final int kind, width, height;
    private int[] board, identities;
    private int turn = 1, rights = 15, ep = -1, halfmoves, last = -1, outcome, reason;
    private int redChecks, blackChecks;
    private int[] legalCache;
    private final List<History> history = new ArrayList<>();
    private record History(String key, int side, boolean check, Set<Integer> chased, int[] threats) {}

    public ChessPosition(int kind) {
        if (kind != XIANGQI && kind != CHESS) throw new IllegalArgumentException("Unknown game");
        this.kind = kind; width = kind == CHESS ? 8 : 9; height = kind == CHESS ? 8 : 10;
        board = new int[width * height]; identities = new int[board.length];
        int[] back = kind == CHESS ? new int[]{ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK}
                : new int[]{ROOK, KNIGHT, BISHOP, ADVISOR, KING, ADVISOR, BISHOP, KNIGHT, ROOK};
        for (int x = 0; x < width; x++) { board[x] = -back[x]; board[(height - 1) * width + x] = back[x]; }
        if (kind == CHESS) for (int x = 0; x < 8; x++) { board[8 + x] = -PAWN; board[48 + x] = PAWN; }
        else {
            board[19] = board[25] = -CANNON; board[64] = board[70] = CANNON;
            for (int x = 0; x < 9; x += 2) { board[27 + x] = -PAWN; board[54 + x] = PAWN; }
        }
        for (int i = 0; i < board.length; i++) if (board[i] != 0) identities[i] = i + 1;
        history.add(new History(key(), 0, false, Set.of(), new int[0]));
    }
    private ChessPosition(ChessPosition p, boolean withHistory) {
        kind = p.kind; width = p.width; height = p.height; board = p.board.clone(); identities = p.identities.clone();
        turn = p.turn; rights = p.rights; ep = p.ep; halfmoves = p.halfmoves; last = p.last;
        outcome = p.outcome; reason = p.reason;
        redChecks = p.redChecks; blackChecks = p.blackChecks;
        if (withHistory) history.addAll(p.history);
    }
    public ChessPosition copy() { return new ChessPosition(this, true); }
    public int kind() { return kind; }
    public int width() { return width; }
    public int height() { return height; }
    public int turn() { return turn; }
    public int[] cells() { return board.clone(); }
    public int last() { return last; }
    public int outcome() { return outcome; }
    public int reason() { return reason; }
    public static int move(int from, int to, int promotion) { return from | (to << 7) | (promotion << 14); }
    public static int from(int move) { return move & 127; }
    public static int to(int move) { return (move >>> 7) & 127; }
    public static int promotion(int move) { return move >>> 14; }
    public static int side(int piece) { return piece == 0 ? 0 : piece > 0 ? 1 : 2; }
    public boolean check() { return checked(turn); }
    public int[] legalMoves() {
        if (outcome != 0) return new int[0];
        if (legalCache == null) {
            ArrayList<Integer> moves = new ArrayList<>();
            for (int f = 0; f < board.length; f++) if (side(board[f]) == turn)
                for (int t = 0; t < board.length; t++) if (pseudo(f, t, false) && Math.abs(board[t]) != KING) {
                    boolean promotes = kind == CHESS && Math.abs(board[f]) == PAWN && (t / width == 0 || t / width == 7);
                    if (promotes) {
                        for (int piece : new int[]{QUEEN, ROOK, BISHOP, KNIGHT}) if (safe(move(f, t, piece), turn)) moves.add(move(f, t, piece));
                    } else if (safe(move(f, t, 0), turn)) moves.add(move(f, t, 0));
                }
            legalCache = moves.stream().mapToInt(Integer::intValue).toArray();
        }
        return legalCache.clone();
    }
    public boolean play(int move) {
        if (Arrays.stream(legalMoves()).noneMatch(m -> m == move)) return false;
        int movingSide = turn, captured = board[to(move)], piece = Math.abs(board[from(move)]);
        boolean forwardPawn = piece == PAWN && from(move) / width != to(move) / width;
        raw(move); turn = 3 - turn; legalCache = null;
        if (kind == XIANGQI) {
            if (captured != 0) redChecks = blackChecks = 0;
            else if (checked(turn) && (movingSide == 1 ? ++redChecks : ++blackChecks) > 10) halfmoves--;
        }
        // Captures and forward pawn moves cannot recur; also bound repetition memory.
        if (captured != 0 || (kind == CHESS ? piece == PAWN : forwardPawn)) history.clear();
        if (kind == XIANGQI && !history.isEmpty()) finalizeChase();
        int[] threats = kind == XIANGQI && !checked(turn) ? threats(movingSide) : new int[0];
        history.add(new History(key(), movingSide, checked(turn), Set.of(), threats));
        adjudicate(); return true;
    }
    private void adjudicate() {
        if (legalMoves().length == 0) {
            outcome = kind == XIANGQI || check() ? 3 - turn : 3;
            reason = check() ? MATE : STALEMATE; return;
        }
        int repetitions = repetitions();
        if (kind == XIANGQI) {
            int a = violation(1), b = violation(2);
            if (a >= 0 && b >= 0) { outcome = a == b ? 3 : a > b ? 2 : 1; reason = a == b ? REPETITION : PERPETUAL; }
        } else if (repetitions >= 5) { outcome = 3; reason = REPETITION; }
        if (outcome == 0 && halfmoves >= (kind == CHESS ? 150 : 100)) { outcome = 3; reason = MOVE_LIMIT; }
        if (outcome == 0 && dead()) { outcome = 3; reason = DEAD; }
    }
    public boolean canClaimDraw(int intendedMove) {
        if (kind != CHESS || outcome != 0) return false;
        if (intendedMove < 0) return repetitions() >= 3 || halfmoves >= 100;
        ChessPosition next = copy();
        return next.play(intendedMove) && (next.repetitions() >= 3 || next.halfmoves >= 100);
    }
    private int repetitions() { String key = key(); return (int) history.stream().filter(h -> h.key.equals(key)).count(); }
    private String key() {
        // A nominal EP square changes repetition identity only when a legal EP capture exists.
        int effectiveEp = -1;
        if (kind == CHESS && ep >= 0) for (int f = 0; f < board.length; f++)
            if (side(board[f]) == turn && Math.abs(board[f]) == PAWN && pseudo(f, ep, false) && safe(move(f, ep, 0), turn)) effectiveEp = ep;
        return Arrays.toString(board) + ':' + turn + ':' + (kind == CHESS ? rights : 0) + ':' + effectiveEp;
    }
    private boolean dead() {
        if (kind == XIANGQI) {
            for (int p : board) if (p != 0 && Math.abs(p) != KING && Math.abs(p) != ADVISOR && Math.abs(p) != BISHOP) return false;
            return true;
        }
        int minors = 0, bishopsColor = -1;
        boolean onlyBishops = true;
        for (int i = 0; i < board.length; i++) {
            int p = Math.abs(board[i]); if (p == 0 || p == KING) continue;
            if (p != BISHOP && p != KNIGHT) return false;
            minors++;
            if (p == KNIGHT) onlyBishops = false;
            else { int color = (i / 8 + i % 8) % 2; if (bishopsColor >= 0 && bishopsColor != color) onlyBishops = false; bishopsColor = color; }
        }
        return minors <= 1 || onlyBishops;
    }
    private boolean safe(int move, int side) { ChessPosition p = new ChessPosition(this, false); p.raw(move); return !p.checked(side); }
    private boolean checked(int side) {
        int king = -1;
        for (int i = 0; i < board.length; i++) if (board[i] == (side == 1 ? KING : -KING)) { king = i; break; }
        return king < 0 || attacked(king, 3 - side);
    }
    private boolean attacked(int target, int side) {
        for (int f = 0; f < board.length; f++) if (side(board[f]) == side && pseudo(f, target, true)) return true;
        return false;
    }
    private boolean palace(int x, int y, int side) { return x >= 3 && x <= 5 && (side == 1 ? y >= 7 : y <= 2); }
    private int between(int f, int t) {
        int dx = Integer.signum(t % width - f % width), dy = Integer.signum(t / width - f / width), n = 0;
        for (int x = f % width + dx, y = f / width + dy; x != t % width || y != t / width; x += dx, y += dy) if (board[y * width + x] != 0) n++;
        return n;
    }
    private boolean pseudo(int f, int t, boolean attack) {
        if (f == t || f < 0 || t < 0 || f >= board.length || t >= board.length || board[f] == 0) return false;
        int side = side(board[f]), p = Math.abs(board[f]), x = f % width, y = f / width, tx = t % width, ty = t / width;
        int dx = tx - x, dy = ty - y, ax = Math.abs(dx), ay = Math.abs(dy), forward = side == 1 ? -1 : 1;
        if (!attack && side(board[t]) == side) return false;
        if (kind == XIANGQI) return switch (p) {
            case KING -> ax + ay == 1 && palace(tx, ty, side) || ax == 0 && Math.abs(board[t]) == KING && between(f, t) == 0;
            case ADVISOR -> ax == 1 && ay == 1 && palace(tx, ty, side);
            case BISHOP -> ax == 2 && ay == 2 && (side == 1 ? ty >= 5 : ty <= 4) && board[(y + dy / 2) * width + x + dx / 2] == 0;
            case KNIGHT -> ax * ay == 2 && board[(y + (ay == 2 ? dy / 2 : 0)) * width + x + (ax == 2 ? dx / 2 : 0)] == 0;
            case ROOK -> (ax == 0 || ay == 0) && between(f, t) == 0;
            case CANNON -> (ax == 0 || ay == 0) && between(f, t) == (attack || board[t] != 0 ? 1 : 0);
            case PAWN -> dx == 0 && dy == forward || ay == 0 && ax == 1 && (side == 1 ? y <= 4 : y >= 5);
            default -> false;
        };
        return switch (p) {
            case KING -> ax <= 1 && ay <= 1 || !attack && castle(f, t, side);
            case ROOK -> (ax == 0 || ay == 0) && between(f, t) == 0;
            case BISHOP -> ax == ay && between(f, t) == 0;
            case QUEEN -> (ax == ay || ax == 0 || ay == 0) && between(f, t) == 0;
            case KNIGHT -> ax * ay == 2;
            case PAWN -> attack ? ax == 1 && dy == forward :
                    ax == 1 && dy == forward && (board[t] != 0 || t == ep)
                    || ax == 0 && board[t] == 0 && (dy == forward || dy == 2 * forward && y == (side == 1 ? 6 : 1) && board[f + forward * 8] == 0);
            default -> false;
        };
    }
    private boolean castle(int f, int t, int side) {
        int home = side == 1 ? 60 : 4;
        if (f != home || (t != home + 2 && t != home - 2)) return false;
        boolean kingSide = t > f; int mask = (side == 1 ? 1 : 4) * (kingSide ? 1 : 2), rook = home + (kingSide ? 3 : -4);
        if ((rights & mask) == 0 || board[rook] != (side == 1 ? ROOK : -ROOK) || between(f, rook) != 0 || checked(side)) return false;
        // Move the king for the transit check so its original square cannot mask a ray.
        return safe(move(f, f + (kingSide ? 1 : -1), 0), side);
    }
    private void raw(int move) {
        int f = from(move), t = to(move), piece = board[f], captured = board[t], side = side(piece);
        if (kind == CHESS) {
            if (Math.abs(piece) == PAWN && t == ep && captured == 0 && f % 8 != t % 8) { board[t + (side == 1 ? 8 : -8)] = 0; identities[t + (side == 1 ? 8 : -8)] = 0; captured = PAWN; }
            if (Math.abs(piece) == KING) {
                rights &= side == 1 ? ~3 : ~12;
                if (Math.abs(t - f) == 2) {
                    int rook = f + (t > f ? 3 : -4), target = f + (t > f ? 1 : -1);
                    board[target] = board[rook]; board[rook] = 0; identities[target] = identities[rook]; identities[rook] = 0;
                }
            }
            for (int corner : new int[]{63, 56, 7, 0}) if (f == corner || t == corner) rights &= ~(corner == 63 ? 1 : corner == 56 ? 2 : corner == 7 ? 4 : 8);
            ep = Math.abs(piece) == PAWN && Math.abs(t - f) == 16 ? (t + f) / 2 : -1;
        }
        halfmoves = captured != 0 || kind == CHESS && Math.abs(piece) == PAWN ? 0 : halfmoves + 1;
        board[t] = promotion(move) == 0 ? piece : (side == 1 ? 1 : -1) * promotion(move); board[f] = 0;
        identities[t] = identities[f]; identities[f] = 0; last = move; legalCache = null;
    }
    // WXF chase classification follows the response-based algorithm described in
    // Tan & Watkinson Medina (2024), arXiv:2412.17334. Piece identity survives moves.
    private int[] threats(int side) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int f = 0; f < board.length; f++) if (side(board[f]) == side && Math.abs(board[f]) != KING && Math.abs(board[f]) != PAWN)
            for (int t = 0; t < board.length; t++) if (side(board[t]) == 3 - side && Math.abs(board[t]) != KING) {
                if (Math.abs(board[t]) == PAWN && (side(board[t]) == 1 ? t / width >= 5 : t / width <= 4)) continue;
                if (!pseudo(f, t, false) || !safe(move(f, t, 0), side)) continue;
                // A legal reciprocal capture is an exchange, except a minor attacking a rook.
                if (unprotected(f, t)) list.add(move(identities[f], identities[t], 0));
            }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
    private boolean unprotected(int f, int t) {
        boolean rookChase = Math.abs(board[t]) == ROOK && (Math.abs(board[f]) == KNIGHT || Math.abs(board[f]) == CANNON);
        if (rookChase) return true;
        if (pseudo(t, f, false) && safe(move(t, f, 0), side(board[t]))) return false;
        return !protectedCapture(f, t);
    }
    private boolean protectedCapture(int f, int t) {
        ChessPosition after = new ChessPosition(this, false); int defender = side(board[t]); after.raw(move(f, t, 0));
        for (int d = 0; d < board.length; d++) if (d != t && side(board[d]) == defender) {
            int type = Math.abs(board[d]);
            // WXF protection rays use occupancy before capture (examples 40/41).
            boolean reaches = (type == ROOK || type == CANNON) ? pseudo(d, t, true) : after.pseudo(d, t, true);
            if (reaches && after.safe(move(d, t, 0), defender)) return true;
        }
        return false;
    }
    private int squareOf(int id) { for (int i = 0; i < identities.length; i++) if (identities[i] == id) return i; return -1; }
    private void finalizeChase() {
        int i = history.size() - 1; History h = history.get(i); Set<Integer> chased = new HashSet<>();
        for (int threat : h.threats) {
            int f = squareOf(from(threat)), t = squareOf(to(threat));
            if (f >= 0 && t >= 0 && (!pseudo(f, t, false) || !safe(move(f, t, 0), h.side) || !unprotected(f, t))) chased.add(to(threat));
        }
        history.set(i, new History(h.key, h.side, h.check, Set.copyOf(chased), h.threats));
    }
    private int violation(int side) {
        // A chase is determined by the actual reply. Never guess the pending reply.
        int end = history.size() - 2;
        while (end >= 0 && history.get(end).side != side) end--;
        if (end < 0) return -1;
        String current = history.get(end).key; int start = -1, found = 0;
        for (int i = end; i >= 0; i--) if (history.get(i).key.equals(current) && ++found == 4) { start = i; break; }
        if (start < 0) return -1;
        boolean allCheck = true, allChase = true, seen = false; Set<Integer> victims = null;
        for (int i = start + 1; i <= end; i++) {
            History h = history.get(i); if (h.side != side) continue; seen = true; allCheck &= h.check;
            Set<Integer> targets = h.chased;
            allChase &= !h.check && !targets.isEmpty();
            if (victims == null) victims = new HashSet<>(targets); else victims.retainAll(targets);
        }
        return !seen ? 0 : allCheck ? 2 : allChase && victims != null && !victims.isEmpty() ? 1 : 0;
    }

    /** FEN loader used by rule fixtures and offline analysis; never accepted over the network. */
    public static ChessPosition fen(int kind, String fen) {
        ChessPosition p = new ChessPosition(kind); Arrays.fill(p.board, 0); Arrays.fill(p.identities, 0);
        String[] parts = fen.split(" "); int i = 0;
        for (char c : parts[0].toCharArray()) {
            if (c == '/') continue;
            if (Character.isDigit(c)) { i += c - '0'; continue; }
            int type = switch (Character.toLowerCase(c)) { case 'k' -> KING; case 'r' -> ROOK; case 'n', 'h' -> KNIGHT; case 'b', 'e' -> BISHOP; case 'q' -> QUEEN; case 'p' -> PAWN; case 'c' -> CANNON; case 'a' -> ADVISOR; default -> throw new IllegalArgumentException("Piece"); };
            p.board[i] = Character.isUpperCase(c) ? type : -type; p.identities[i] = i + 1; i++;
        }
        if (i != p.board.length) throw new IllegalArgumentException("Board length");
        p.turn = parts.length > 1 && parts[1].equals("b") ? 2 : 1; p.rights = 0; p.ep = -1;
        if (kind == CHESS && parts.length > 2) { if (parts[2].contains("K")) p.rights |= 1; if (parts[2].contains("Q")) p.rights |= 2; if (parts[2].contains("k")) p.rights |= 4; if (parts[2].contains("q")) p.rights |= 8; }
        if (kind == CHESS && parts.length > 3 && !parts[3].equals("-")) p.ep = (8 - (parts[3].charAt(1) - '0')) * 8 + parts[3].charAt(0) - 'a';
        if (parts.length > 4) p.halfmoves = Integer.parseInt(parts[4]);
        p.history.clear(); p.history.add(new History(p.key(), 0, false, Set.of(), new int[0])); return p;
    }
}
