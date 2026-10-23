package com.goosethings.tools.intro;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MatchCountsTest {
    @Test void publicSnapshotIncludesPlayersOutsideFilteredRoster() {
        var counts=new MatchCounts(11,3,1);
        assertEquals(7,counts.geese());
        assertEquals(11,counts.players());
    }
    @Test void malformedCountsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new MatchCounts(2,2,1));
        assertThrows(IllegalArgumentException.class,()->new MatchCounts(-1,0,0));
        assertThrows(IllegalArgumentException.class,()->new MatchCounts(11,-1,0));
        assertEquals(0,new MatchCounts(0,0,0).geese());
    }
}
