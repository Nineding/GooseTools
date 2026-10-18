package com.goosethings.tools.dream;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionSpatialContextTest {
    @Test void outwardActionsUseTheRemoteOrigin() {
        for (String path : new String[]{"task/goosechapel/clean", "skill/sniper/update_crosshair",
                "skill/sniper/fire", "skill/seagull/sniper/fire", "rccheck"}) {
            assertTrue(DreamSpatialContext.projectionGameplay(Identifier.parse("ggd:" + path)), path);
        }
    }
    @Test void victimsAndLifecycleKeepThePhysicalBodyOrigin() {
        for (String path : new String[]{"skill/kill/hit_astral_body", "skill/eat/hit_sniper_decoy",
                "skill/sniper/fire_victim", "skill/esper/abort", "skill/mime/finish", "skill/astral/close",
                "talker/start", "role_extensions/cleanup"}) {
            assertFalse(DreamSpatialContext.projectionGameplay(Identifier.parse("ggd:" + path)), path);
        }
    }
    @Test void sessionPacketPreservesConfiguredFlightSpeeds() {
        assertEquals(.2F, new DreamAvatarPackets.View(1,true,0,64,0,true,0).flightScale());
        assertEquals(.25F, new DreamAvatarPackets.View(1,true,0,64,0,true,1).flightScale());
        assertEquals(1F, new DreamAvatarPackets.View(1,true,0,64,0,true).flightScale());
        assertThrows(IllegalArgumentException.class, () -> new DreamAvatarPackets.View(1,true,0,64,0,true,4));
    }
}
