package com.goosethings.tools.boardgame;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BoardPacketsTest {
    @Test void allBoardShapesSignedPiecesLegalMovesAndInviteKindRoundTrip() {
        for (int kind = 0; kind < 3; kind++) {
            var chess = kind == 0 ? null : new ChessPosition(kind);
            int[] cells = kind == 0 ? new int[225] : chess.cells();
            var p = new BoardPackets.Position(9, 7, "First", "Second", 2, 1, 0, 0, 2, 49000, -1, 0, false,
                    cells, new int[0], kind, chess == null ? new int[0] : chess.legalMoves(), false, false);
            var view = new BoardPackets.View(8, 1, 2, 0, p, List.of(new BoardPackets.Person(UUID.randomUUID(), "Busy", false)), 3, 2, "Inviter", 21000, kind);
            var b = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                BoardPackets.View.CODEC.encode(b, view); var copy = BoardPackets.View.CODEC.decode(b);
                assertEquals(0, b.readableBytes()); assertArrayEquals(cells, copy.position().board());
                assertArrayEquals(p.legal(), copy.position().legal()); assertEquals(kind, copy.inviteKind()); assertFalse(copy.players().getFirst().available());
                var a = new BoardPackets.Action(BoardPackets.MOVE, 0, 9, 7, 1, ChessPosition.move(8, 0, ChessPosition.QUEEN), 0, BoardPackets.NONE, kind);
                BoardPackets.Action.CODEC.encode(b, a); assertEquals(a, BoardPackets.Action.CODEC.decode(b)); assertTrue(a.valid());
                BoardPackets.View.CODEC.encode(b, view); b.writerIndex(b.writerIndex() - 1);
                assertThrows(IndexOutOfBoundsException.class, () -> BoardPackets.View.CODEC.decode(b));
            } finally { b.release(); }
        }
    }
    @Test void protocolRejectsUnboundedInputAndWrongBoardShape() {
        for (int value : new int[]{-2, 131072, Integer.MAX_VALUE}) assertFalse(new BoardPackets.Action(7, 0, 1, 1, 1, value, 0, BoardPackets.NONE, 2).valid());
        assertFalse(new BoardPackets.Action(2, 0, 0, 0, 0, 1, 1, BoardPackets.NONE, 3).valid());
        assertThrows(IllegalArgumentException.class, () -> new BoardPackets.Position(1, 1, "", "", 1, 1, 0, 0, 1, 0, -1, 0, false, new int[225], new int[0], 1, new int[0], false, false));
    }
}
