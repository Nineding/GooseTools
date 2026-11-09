package com.goosethings.tools.gomoku;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GomokuPacketsTest {
    @Test void snapshotsAndInputsRoundTripWithoutHiddenOrUnboundedState() {
        int[] board = new int[225]; board[112]=1; board[113]=2;
        var position=new GomokuPackets.Position(7,19,"Black","White",1,1,0,0,2,48_000,113,0,false,board,new int[0]);
        UUID target=UUID.randomUUID();
        var view=new GomokuPackets.View(20,4,2,0,position,List.of(new GomokuPackets.Person(target,"Opponent")),8,2,"Inviter",28_000);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            GomokuPackets.View.CODEC.encode(buffer,view);assertTrue(buffer.readableBytes()<512);
            var decoded=GomokuPackets.View.CODEC.decode(buffer);assertEquals(0,buffer.readableBytes());
            assertEquals(view.serial(),decoded.serial());assertEquals(view.request(),decoded.request());assertEquals(view.players(),decoded.players());
            assertArrayEquals(board,decoded.position().board());assertEquals(28_000,decoded.inviteRemaining());assertEquals(48_000,decoded.position().remaining());
            var input=new GomokuPackets.Action(GomokuPackets.MOVE,0,7,19,23,114,0,target);
            GomokuPackets.Action.CODEC.encode(buffer,input);assertEquals(input,GomokuPackets.Action.CODEC.decode(buffer));assertTrue(input.valid());
        } finally {buffer.release();}
    }
    @Test void maliciousCoordinatesSequencesAndSnapshotArraysAreRejected() {
        for(int value:new int[]{Integer.MIN_VALUE,-2,225,Integer.MAX_VALUE})
            assertFalse(new GomokuPackets.Action(GomokuPackets.MOVE,0,1,1,1,value,0,GomokuPackets.NONE).valid());
        assertFalse(new GomokuPackets.Action(99,0,1,1,1,1,0,GomokuPackets.NONE).valid());
        assertFalse(new GomokuPackets.Action(7,0,1,1,-1,1,0,GomokuPackets.NONE).valid());
        assertThrows(IllegalArgumentException.class,()->new GomokuPackets.Position(1,0,"a","b",1,1,0,0,1,60_001,-1,0,false,new int[225],new int[0]));
        assertThrows(IllegalArgumentException.class,()->new GomokuPackets.Position(1,0,"a","b",1,1,0,0,1,1,-1,0,false,new int[226],new int[0]));
        int[] board=new int[225];board[1]=7;
        assertThrows(IllegalArgumentException.class,()->new GomokuPackets.Position(1,0,"a","b",1,1,0,0,1,1,-1,0,false,board,new int[0]));
        assertThrows(IllegalArgumentException.class,()->new GomokuPackets.View(1,0,1,0,GomokuPackets.Position.empty(),Collections.nCopies(129,new GomokuPackets.Person(GomokuPackets.NONE,"x")),0,0,"",0));
    }
    @Test void decodingRejectsTruncatedPayloads() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            var view=new GomokuPackets.View(1,0,1,0,GomokuPackets.Position.empty(),List.of(),0,0,"",0);
            GomokuPackets.View.CODEC.encode(buffer,view);
            buffer.writerIndex(buffer.writerIndex()-1);
            assertThrows(IndexOutOfBoundsException.class,()->GomokuPackets.View.CODEC.decode(buffer));
        } finally {buffer.release();}
    }
}
