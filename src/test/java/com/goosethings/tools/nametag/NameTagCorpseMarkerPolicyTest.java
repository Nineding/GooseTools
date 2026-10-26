package com.goosethings.tools.nametag;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameTagCorpseMarkerPolicyTest {
    @Test void onlyPrivateLivingMeetingViewsReceiveCorpseMarks() {
        for (boolean full : new boolean[]{false,true}) for (String context : new String[]{"inTalk","inDream","gamingGGD","lobby"}) {
            Set<String> tags = new HashSet<>(Set.of("players",context));
            boolean allowed = full && (context.equals("inTalk") || context.equals("inDream"));
            assertEquals(allowed,NameTagCorpseMarkerPolicy.viewerCanSee(full,tags));
            for(String deny : new String[]{"spectator","deadInMap","endGame"}) {
                tags.add(deny);assertFalse(NameTagCorpseMarkerPolicy.viewerCanSee(full,tags));tags.remove(deny);
            }
        }
    }
    @Test void sourceMustBeCanonicalAndUnambiguousPublicMeetingCorpseLabel() {
        for(int seat=1;seat<=21;seat++) {
            assertEquals(seat,NameTagCorpseMarkerPolicy.sourceSeat(Set.of("talk","ggd.corpseLabel","p"+seat)));
            assertEquals(0,NameTagCorpseMarkerPolicy.sourceSeat(Set.of("ggd.corpseLabel","p"+seat)));
            assertEquals(0,NameTagCorpseMarkerPolicy.sourceSeat(Set.of("talk","players","p"+seat)));
        }
        for(String invalid : new String[]{"p0","p22","p01","p1suffix","dreamSource_p1"})
            assertEquals(0,NameTagCorpseMarkerPolicy.sourceSeat(Set.of("talk","ggd.corpseLabel",invalid)));
        assertEquals(0,NameTagCorpseMarkerPolicy.sourceSeat(Set.of("talk","ggd.corpseLabel","p1","p2")));
    }
    @Test void onlyTheExplicitOverlayFlagSelectsMarkerOnlyRendering() {
        for(int prior=0;prior<128;prior++) {
            assertFalse(NameTagCorpseMarkerPolicy.markerOnly(prior));
            assertTrue(NameTagCorpseMarkerPolicy.markerOnly(prior|NameTagCorpseMarkerPolicy.CORPSE_MARKER_ONLY));
        }
    }
}
