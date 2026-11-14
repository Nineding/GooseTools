package com.goosethings.tools.ambience;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class ChapelAmbienceConfigTest {
    ChapelAmbienceConfig load()throws Exception{try(var in=getClass().getResourceAsStream("/assets/goosetools/ambience/goosechapel.json")){assertNotNull(in);return ChapelAmbienceConfig.parse(new String(in.readAllBytes(),StandardCharsets.UTF_8));}}
    @Test void assignedSoundsMatchAllThreeExistingLocations()throws Exception{
        var c=load();assertEquals(8,c.mapId());assertEquals(3,c.regions().size());
        assertEquals("factory",c.select(null,"minecraft:overworld",-993,116.6,-647).id());
        assertEquals("tavern",c.select(null,"minecraft:overworld",-991,117.6,-724).id());
        assertEquals("gateway",c.select(null,"minecraft:overworld",-937,116.6,-641).id());
        assertEquals("gateway",c.select(null,"minecraft:overworld",-932,116.6,-621).id());
    }
    @Test void onlyTavernKeepsItsPlaybackClockWhileOutside()throws Exception{
        var regions=load().regions();
        assertTrue(regions.stream().filter(r->r.id().equals("tavern")).findFirst().orElseThrow().continuous());
        assertFalse(regions.stream().filter(r->r.id().equals("factory")).findFirst().orElseThrow().continuous());
        assertFalse(regions.stream().filter(r->r.id().equals("gateway")).findFirst().orElseThrow().continuous());
    }
    @Test void buildingsUseVerticalBoundsAndAvoidNeighbouringRooms()throws Exception{
        var c=load();assertNull(c.select(null,"minecraft:overworld",-993,125,-647));
        assertNull(c.select("factory","minecraft:overworld",-993,113,-647));
        assertNull(c.select(null,"minecraft:overworld",-987,116.6,-660));
        assertNull(c.select(null,"minecraft:overworld",-969,116.6,-637));
        assertNull(c.select(null,"minecraft:overworld",-963,117.6,-719));
        assertNull(c.select(null,"minecraft:the_nether",-993,116.6,-647));
        assertEquals("factory",c.select(null,"ggd_training:goosechapel",-993,116.6,-647).id());
    }
    @Test void borderHysteresisDoesNotExpandVerticalBoundsOrOtherScenes()throws Exception{
        var c=load();assertNull(c.select(null,"minecraft:overworld",-990.8,116.6,-660));
        assertEquals("factory",c.select("factory","minecraft:overworld",-990.8,116.6,-660).id());
        assertNull(c.select("factory","minecraft:overworld",-990.4,116.6,-660));
        assertNull(c.select("factory","minecraft:overworld",-993,121.1,-660));
        assertNull(c.select("tavern","minecraft:overworld",-990.8,116.6,-660));
    }
    @Test void malformedVolumesBoxesAndSoundsAreRejected()throws Exception{
        String json;try(var in=getClass().getResourceAsStream("/assets/goosetools/ambience/goosechapel.json")){json=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        final String valid=json;
        assertThrows(RuntimeException.class,()->ChapelAmbienceConfig.parse(valid.replace("0.8","1.8")));
        assertThrows(RuntimeException.class,()->ChapelAmbienceConfig.parse(valid.replace("\"min_x\": -1002","\"min_x\": -900")));
        assertThrows(RuntimeException.class,()->ChapelAmbienceConfig.parse(valid.replace("ggd:ambience/goosechapel/factory","minecraft:entity.generic.explode")));
    }
}
