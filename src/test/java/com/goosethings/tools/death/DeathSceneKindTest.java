package com.goosethings.tools.death;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DeathSceneKindTest {
    @Test void specificCauseSurvivesGenericCleanup() {
        for(var k:DeathSceneKind.values()) {
            assertEquals(k,DeathSceneKind.parse(k.id()));
            assertEquals(k,DeathSceneKind.prefer(k,DeathSceneKind.UNKNOWN));
        }
        assertEquals(DeathSceneKind.SHERIFF,DeathSceneKind.prefer(DeathSceneKind.SHERIFF,DeathSceneKind.KNIFE));
        assertEquals(DeathSceneKind.LAVA,DeathSceneKind.prefer(DeathSceneKind.LAVA,DeathSceneKind.SPECIAL));
        assertEquals(DeathSceneKind.LOVER,DeathSceneKind.prefer(DeathSceneKind.MEETING_SHOT,DeathSceneKind.LOVER));
    }
    @Test void easingIsBoundedMonotonicAndTimed() {
        assertEquals(0,DeathSceneKind.opacity(-1));assertEquals(0,DeathSceneKind.opacity(3));
        assertEquals(1,DeathSceneKind.opacity(1));
        double previous=0;
        for(int i=0;i<=1000;i++){double value=DeathSceneKind.ease(i/1000.0);assertTrue(value>=previous&&value<=1);previous=value;}
        assertEquals(0,DeathSceneKind.ease(0));assertEquals(1,DeathSceneKind.ease(1));
    }
}
