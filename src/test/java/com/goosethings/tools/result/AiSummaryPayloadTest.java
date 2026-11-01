package com.goosethings.tools.result;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiSummaryPayloadTest {
    @Test void pendingAccountTotalUsesItsOwnPacketAndSupportsLargeBalances() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            for(var status:List.of(new PendingRewardsPayload(true,6692),new PendingRewardsPayload(true,3_000_000_000L),
                    new PendingRewardsPayload(true,0),new PendingRewardsPayload(false,0))) {
                PendingRewardsPayload.CODEC.encode(buffer,status);
                assertEquals(status,PendingRewardsPayload.CODEC.decode(buffer));
                assertEquals(0,buffer.readableBytes());
            }
            PendingRewardsPayload.Request.CODEC.encode(buffer,PendingRewardsPayload.Request.INSTANCE);
            assertEquals(0,buffer.readableBytes());
            assertEquals(PendingRewardsPayload.Request.INSTANCE,PendingRewardsPayload.Request.CODEC.decode(buffer));
        } finally {buffer.release();}
    }

    @Test void repairingCoinsPreservesExperienceRosterAndWireFields() {
        UUID id=UUID.randomUUID(),round=UUID.randomUUID();
        var hero=new MatchResultPayload.Player(id,"RewardPlayer","coroner",0xbfffba,1,false,true,false,
                Collections.nCopies(EquipmentSlot.values().length,ItemStack.EMPTY));
        var roster=new SummaryRoster(List.of(new SummaryRoster.Entry(id,"RewardPlayer","coroner",0,1,0xbfffba,false,3050,0)),2);
        var before=new SummaryPayload(true,round,new MatchResultPayload(true,"good_objective",List.of(hero)),0,0,3050,true,
                30,0,0,30,0,294,15,294,45,500,0,List.of(2940),roster);
        var repaired=before.withCoins(315);
        assertEquals(0,before.coins());
        assertEquals(315,repaired.coins());
        assertEquals(before.gainedXp(),repaired.gainedXp());
        assertEquals(before.startXp(),repaired.startXp());
        assertEquals(before.endXp(),repaired.endXp());
        assertEquals(before.scoreboard(),repaired.scoreboard());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            AiSummaryPayload.CODEC.encode(buffer,new AiSummaryPayload(2582,repaired));
            var decoded=AiSummaryPayload.CODEC.decode(buffer);
            assertEquals(2582,decoded.gameId());
            assertEquals(315,decoded.summary().coins());
            assertEquals(3050,decoded.summary().spotlightIndex());
            assertEquals(45,decoded.summary().endXp());
            assertEquals(roster,decoded.summary().scoreboard());
            assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
}
