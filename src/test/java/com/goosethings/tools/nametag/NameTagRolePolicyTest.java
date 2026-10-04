package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagRolePolicyTest {
    @Test
    void acceptsRealRoleOrMatchingSeagullBorrow() {
        assertTrue(NameTagRolePolicy.hasRole(Set.of("Detective"), "Detective"));
        assertTrue(NameTagRolePolicy.hasRole(
                Set.of("Seagull", "seagullNametagDetective"), "Detective"));
    }

    @Test
    void rejectsOrphanedOrDifferentBorrowTags() {
        assertFalse(NameTagRolePolicy.hasRole(Set.of("seagullNametagDetective"), "Detective"));
        assertFalse(NameTagRolePolicy.hasRole(
                Set.of("Seagull", "seagullNametagPigeon"), "Detective"));
    }
}
