package com.goosethings.tools.result;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SummaryRosterTest {
    private static SummaryRoster.Entry row(int seat,int faction,int index) {
        return new SummaryRoster.Entry(new UUID(1,seat),"Player"+seat,"Goose",faction,seat,0xffffff,false,index,0);
    }
    @Test void frozenRosterIncludesEveryFactionAndNegativeScoreInSeatOrder() {
        var entries=new ArrayList<>(List.of(row(3,1,2934),row(4,2,-672),row(2,0,3419),row(1,0,3419)));
        var frozen=new SummaryRoster(entries,7);entries.clear();
        assertEquals(List.of(1,2,3,4),frozen.players().stream().map(SummaryRoster.Entry::serial).toList());
        assertEquals(-672,frozen.players().getLast().index());
        assertThrows(UnsupportedOperationException.class,()->frozen.players().clear());
    }
    @Test void malformedRosterCannotHidePlayersOrExceedRoomLimit() {
        assertThrows(IllegalArgumentException.class,()->new SummaryRoster(List.of(),0));
        assertThrows(IllegalArgumentException.class,()->new SummaryRoster(List.of(row(1,0,0),row(1,1,2)),0));
        assertThrows(IllegalArgumentException.class,()->new SummaryRoster(IntStream.rangeClosed(1,22).mapToObj(i->row(i,0,0)).toList(),0));
        assertThrows(IllegalArgumentException.class,()->new SummaryRoster(List.of(row(1,0,0)),-1));
        assertThrows(IllegalArgumentException.class,()->row(1,3,0));
        assertEquals(21,new SummaryRoster(IntStream.rangeClosed(1,21).mapToObj(i->row(i,0,-i)).toList(),0).players().size());
    }
}
