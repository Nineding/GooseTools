package com.goosethings.tools.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimeBodyMovementPolicyTest {
    @Test
    void mirrorsHorizontalMovementWithoutBorrowingOrdinaryVerticalMotion() {
        MimeBodyMovementPolicy.Movement movement = MimeBodyMovementPolicy.capture(
                0.21D, -0.08D, -0.13D, false, false);

        assertEquals(0.21D, movement.x());
        assertEquals(0.0D, movement.y());
        assertEquals(-0.13D, movement.z());
        assertFalse(movement.jumped());
    }

    @Test
    void mirrorsTheRealGroundedToAirborneJumpEdge() {
        MimeBodyMovementPolicy.Movement movement = MimeBodyMovementPolicy.capture(
                0.08D, 0.42D, 0.0D, true, false);

        assertEquals(0.42D, movement.y());
        assertTrue(movement.jumped());
    }

    @Test
    void stepUpWhileStillGroundedIsNotMisclassifiedAsAJump() {
        MimeBodyMovementPolicy.Movement movement = MimeBodyMovementPolicy.capture(
                0.12D, 0.5D, 0.0D, true, true);

        assertEquals(0.0D, movement.y());
        assertFalse(movement.jumped());
    }

    @Test
    void discardsTeleportSizedOrNonFiniteMovement() {
        MimeBodyMovementPolicy.Movement teleport = MimeBodyMovementPolicy.capture(
                3.0D, 0.42D, 0.0D, true, false);
        MimeBodyMovementPolicy.Movement invalid = MimeBodyMovementPolicy.capture(
                Double.NaN, 0.42D, 0.0D, true, false);

        assertEquals(0.0D, teleport.x());
        assertFalse(teleport.jumped());
        assertEquals(0.0D, invalid.x());
        assertFalse(invalid.jumped());
    }
}
