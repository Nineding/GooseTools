package com.goosethings.tools.network;

import com.goosethings.tools.nametag.MeetingPetLayout;
import com.goosethings.tools.nametag.NameTagCorpseMarkerPolicy;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MeetingPetNameTagPayloadTest {
    @Test void oldServerFlagsAndNewFlagsRespectTheQuarterBlockLimit() {
        int olderThreeBlockLift=MeetingPetLayout.HEADROOM|(1<<9)|(192<<16);
        assertEquals(.25, MeetingPetLayout.lift(olderThreeBlockLift));
        assertEquals(.25, MeetingPetLayout.lift(MeetingPetLayout.flags(3)));
        assertEquals(0, MeetingPetLayout.lift(MeetingPetLayout.flags(0)));
    }
    @Test void existingWireFormatPreservesHeadroomAttachmentsAndStandInAlias() {
        UUID owner=UUID.randomUUID(),proxy=UUID.randomUUID();
        for(int flags:new int[]{0,127,127|MeetingPetLayout.HEADROOM,127|MeetingPetLayout.flags(.12),127|MeetingPetLayout.flags(.4)}) {
            var entry=new GooseToolsPayloads.NameTagEntry(owner,owner,"Owner","Owner",0x55aaee,3,flags,0,false,
                    List.of(new GooseToolsPayloads.NameTagIcon("minecraft:textures/item/feather.png",10,10,0xffffff)));
            var snapshot=new GooseToolsPayloads.NameTagSnapshotS2C(List.of(entry),List.of(new GooseToolsPayloads.NameTagAlias(proxy,owner)));
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try{GooseToolsPayloads.NameTagSnapshotS2C.CODEC.encode(buffer,snapshot);
                var decoded=GooseToolsPayloads.NameTagSnapshotS2C.CODEC.decode(buffer);
                assertEquals(snapshot,decoded);assertEquals(0,buffer.readableBytes());
                assertFalse(NameTagCorpseMarkerPolicy.markerOnly(decoded.entries().getFirst().attachmentFlags()));
                assertEquals(MeetingPetLayout.lift(flags),MeetingPetLayout.lift(decoded.entries().getFirst().attachmentFlags()));
            }finally{buffer.release();}
        }
    }
}
