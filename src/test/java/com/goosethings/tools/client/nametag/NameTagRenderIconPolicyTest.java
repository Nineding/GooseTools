package com.goosethings.tools.client.nametag;

import com.goosethings.tools.nametag.NameTagSync;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagRenderIconPolicyTest {
    @Test
    void mergesPrivateAndSpectatorCurseSourcesIntoOneDecision() {
        assertTrue(NameTagRenderIconPolicy.showWitchDoctorCurse(true, 0));
        assertTrue(NameTagRenderIconPolicy.showWitchDoctorCurse(
                false, NameTagSync.WITCH_DOCTOR_CURSE));
        assertTrue(NameTagRenderIconPolicy.showWitchDoctorCurse(
                true, NameTagSync.WITCH_DOCTOR_CURSE));
        assertFalse(NameTagRenderIconPolicy.showWitchDoctorCurse(false, 0));
    }
}
