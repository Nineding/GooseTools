package com.goosethings.tools.regions;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RegionResolverTest {
    RegionDefinition region(String id,double anchor,List<RegionVolume> volume){return new RegionDefinition(id,"test","minecraft:overworld",id,"","",anchor,0,volume,List.of(),1);}
    @Test void boundedRegionsNeverParticipateInNearestAnchorFallback(){
        var bounded=region("bounded",0,List.of(RegionVolume.box(10,20,0,5,10,20)));var anchor=region("anchor",100,List.of());
        assertEquals(anchor,RegionResolver.resolve(List.of(bounded,anchor),0,1,0,r->true));
        assertEquals(bounded,RegionResolver.resolve(List.of(bounded,anchor),12,1,12,r->true));
        assertNull(RegionResolver.resolve(List.of(bounded),0,1,0,r->true));
    }
    @Test void drawingAndRemovingARangeSwitchesTheDetectionMode(){
        var first=region("first",0,List.of());var second=region("second",100,List.of());
        assertEquals(first,RegionResolver.resolve(List.of(first,second),1,1,0,r->true));
        var converted=region("first",0,List.of(RegionVolume.box(10,20,0,5,10,20)));
        assertEquals(second,RegionResolver.resolve(List.of(converted,second),1,1,0,r->true));
        assertEquals(first,RegionResolver.resolve(List.of(first,second),1,1,0,r->true));
        assertNull(RegionResolver.resolve(List.of(first,second),1,1,0,r->false));
    }
    @Test void continuousRegionsShareOnlyTheSameMapAndSoundClock(){
        assertEquals(AmbienceClock.key("chapel","gateway","wave",true),AmbienceClock.key("chapel","harbor","wave",true));
        assertNotEquals(AmbienceClock.key("chapel","gateway","wave",true),AmbienceClock.key("other","harbor","wave",true));
        assertNotEquals(AmbienceClock.key("chapel","gateway","wave",true),AmbienceClock.key("chapel","harbor","tavern",true));
        assertNotEquals(AmbienceClock.key("chapel","gateway","wave",false),AmbienceClock.key("chapel","harbor","wave",false));
    }
}
