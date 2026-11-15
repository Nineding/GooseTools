package com.goosethings.tools.regions;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RegionVolumeTest {
    RegionVolume.Point p(double x,double z){return new RegionVolume.Point(x,z);}
    @Test void concavePolygonKeepsItsCutoutAndHeight(){
        var v=new RegionVolume(10,20,List.of(p(0,0),p(6,0),p(6,2),p(2,2),p(2,6),p(0,6)));
        assertTrue(v.contains(1,12,5,0));assertTrue(v.contains(5,12,1,0));assertFalse(v.contains(5,12,5,0));
        assertTrue(v.contains(0,10,0,0));assertFalse(v.contains(1,20,1,0));assertFalse(v.contains(1,9,1,2));
    }
    @Test void clockwiseAndCounterclockwiseMatch(){
        var points=List.of(p(-950,-640),p(-930,-640),p(-930,-620),p(-950,-620));var reversed=new ArrayList<>(points);Collections.reverse(reversed);
        assertEquals(new RegionVolume(0,10,points).contains(-940,5,-630,0),new RegionVolume(0,10,reversed).contains(-940,5,-630,0));
    }
    @Test void boundaryGraceOnlyExpandsHorizontalEdges(){
        var v=RegionVolume.box(0,4,10,20,0,4);
        assertTrue(v.contains(4.4,15,2,.5));assertFalse(v.contains(4.6,15,2,.5));assertFalse(v.contains(2,20.1,2,.5));
    }
    @Test void invalidPolygonsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new RegionVolume(0,1,List.of(p(0,0),p(4,4),p(0,4),p(4,0))));
        assertThrows(IllegalArgumentException.class,()->new RegionVolume(0,1,List.of(p(0,0),p(2,0),p(1,0),p(1,2))));
        assertThrows(IllegalArgumentException.class,()->new RegionVolume(0,1,List.of(p(0,0),p(0,0),p(0,4))));
        assertThrows(IllegalArgumentException.class,()->new RegionVolume(0,1,List.of(p(0,0),p(1,0),p(2,0))));
        assertThrows(IllegalArgumentException.class,()->new RegionVolume(5,5,List.of(p(0,0),p(1,0),p(0,1))));
    }
    @Test void exclusionsRemainExcludedWithExitGrace(){
        var r=new RegionDefinition("test","test","minecraft:overworld","test","","",0,0,List.of(RegionVolume.box(0,10,0,10,0,10)),List.of(RegionVolume.box(4,6,0,10,4,6)),0);
        assertTrue(r.contains(3,5,3,.5));assertFalse(r.contains(5,5,5,.5));
    }
}
