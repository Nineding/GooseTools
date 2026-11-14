package com.goosethings.tools.regions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RegionPlaybackTest {
    @TempDir Path temp;
    @Test void oldSettingsMigrateWithVisibleLabelsAndOriginalContinuousClock(){
        var audio=RegionCatalog.JSON.fromJson("{\"sound\":\"ggd:wave\",\"volume\":0.8,\"continuous\":true,\"custom\":false}",RegionDefinition.Audio.class);
        assertFalse(audio.delayed());assertTrue(audio.sharesProgress());
        assertEquals(AmbienceClock.key("map","room","ggd:wave",true),AmbienceClock.key("map","room",audio));
    }
    @Test void replayRangeValidatesAndSamplesAnInclusiveFiveToTenSeconds(){
        var audio=new RegionDefinition.Audio("ggd:wood",.8f,true,true,"random_delay",5,10,true);var random=new Random(1);Set<Integer> delays=new HashSet<>();
        for(int i=0;i<4000;i++){int ticks=audio.nextDelayTicks(random);assertTrue(ticks>=100&&ticks<=200);delays.add(ticks);}
        assertEquals(101,delays.size());
        assertThrows(IllegalArgumentException.class,()->new RegionDefinition.Audio("ggd:wood",1,true,true,"random_delay",10,5,true));
        assertThrows(IllegalArgumentException.class,()->new RegionDefinition.Audio("ggd:wood",1,true,true,"random_delay",0,3601,true));
        assertThrows(IllegalArgumentException.class,()->new RegionDefinition.Audio("ggd:wood",1,true,true,"unknown",0,0,true));
    }
    @Test void sharedPlaybackIsExplicitAndDifferentSchedulesStayIndependent(){
        var shared=new RegionDefinition.Audio("ggd:wave",1,false,true,"loop",0,0,true);
        var own=new RegionDefinition.Audio("ggd:wave",1,true,true,"loop",0,0,false);
        assertEquals(AmbienceClock.key("map","a",shared),AmbienceClock.key("map","b",shared));
        assertNotEquals(AmbienceClock.key("map","a",own),AmbienceClock.key("map","b",own));
        var delayed=new RegionDefinition.Audio("ggd:wave",1,false,true,"random_delay",5,10,true);
        assertNotEquals(AmbienceClock.key("map","a",shared),AmbienceClock.key("map","a",delayed));
    }
    @Test void hiddenRegionStillResolvesAndSettingsPersistAcrossEditorRemoval()throws Exception{
        var audio=new RegionDefinition.Audio("ggd:wood",.75f,true,true,"random_delay",5,10,false);
        var room=new RegionDefinition("map/room","map","minecraft:overworld","Room","","",1,1,List.of(RegionVolume.box(0,4,0,4,0,4)),List.of(),0,audio,true);
        var loaded=RegionCatalog.parse(new RegionCatalog(1,List.of(room),Set.of()).json()).find(room.id());assertTrue(loaded.hiddenOnMap());assertEquals(audio,loaded.audio());
        assertEquals(loaded,RegionResolver.resolve(List.of(loaded),1,1,1,r->false));
    }
    @Test void nestedRestroomOverridesPoliceStationRegardlessOfCatalogOrder(){
        var police=new RegionDefinition("map/police","map","minecraft:overworld","Police","","",0,0,List.of(RegionVolume.box(0,30,0,8,0,30)),List.of(),1);
        var wc=new RegionDefinition("map/wc","map","minecraft:overworld","WC","","",0,0,List.of(RegionVolume.box(0,10,0,8,0,15)),List.of(),1);
        assertEquals(wc,RegionResolver.resolve(List.of(police,wc),5,2,5,r->true));
        assertEquals(wc,RegionResolver.resolve(List.of(wc,police),5,2,5,r->true));
        assertEquals(police,RegionResolver.resolve(List.of(police,wc),20,2,20,r->true));
    }
}
