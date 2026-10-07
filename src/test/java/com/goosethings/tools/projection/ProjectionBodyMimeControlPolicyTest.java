package com.goosethings.tools.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionBodyMimeControlPolicyTest {
    @Test
    void activeRemoteRolesExposeTheirRetainedBody() {
        assertTrue(ProjectionBodyServer.Kind.ASTRAL.supportsMimeBodyControl());
        assertTrue(ProjectionBodyServer.Kind.SNIPER.supportsMimeBodyControl());
        assertTrue(ProjectionBodyServer.Kind.ESPER.supportsMimeBodyControl());
    }

    @Test
    void mimeAndDreamBodiesCannotBeNestedControlTargets() {
        assertFalse(ProjectionBodyServer.Kind.MIME.supportsMimeBodyControl());
        assertFalse(ProjectionBodyServer.Kind.DREAM_LUCID.supportsMimeBodyControl());
        assertFalse(ProjectionBodyServer.Kind.DREAM_RAVEN.supportsMimeBodyControl());
    }
}
