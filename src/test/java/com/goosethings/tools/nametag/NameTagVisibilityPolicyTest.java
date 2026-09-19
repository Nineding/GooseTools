package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagVisibilityPolicyTest {
    private static final Set<String> LUCID_DREAMER = Set.of("LucidDreamer", "inDream");
    private static final Set<String> RAVEN_DREAMER = Set.of("Raven", "inDream");

    @Test
    void concealsRavenIdentityFromLucidDreamerWhenBothAreDreaming() {
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false, LUCID_DREAMER, RAVEN_DREAMER));
    }

    @Test
    void doesNotConcealWhenEitherSideIsOutsideTheDream() {
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("LucidDreamer"), RAVEN_DREAMER));
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, LUCID_DREAMER, Set.of("Raven")));
    }

    @Test
    void keepsTheRuleAsymmetric() {
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, RAVEN_DREAMER, LUCID_DREAMER));
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("players", "inDream"), RAVEN_DREAMER));
    }

    @Test
    void neverConcealsTheViewersOwnThirdPersonLabel() {
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                true, LUCID_DREAMER, RAVEN_DREAMER));
    }

    @Test
    void projectedAstralCannotSeeOtherPlayersLabels() {
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("Astral", "astralProjected"), Set.of("players")));
    }

    @Test
    void returningAstralCannotSeeOtherPlayersLabelsBeforeReveal() {
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("Astral", "astralRevealPending"), Set.of("players")));
    }

    @Test
    void projectedAstralKeepsOwnThirdPersonLabel() {
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                true, Set.of("Astral", "astralProjected"), Set.of("players")));
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                true, Set.of("Astral", "astralRevealPending"), Set.of("players")));
    }

    @Test
    void astralLabelsReturnAfterProjectionEnds() {
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("Astral"), Set.of("players")));
    }

    @Test
    void effectiveRavenIdentityUsesTheRenderedPlayersDreamLifecycle() {
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false,
                LUCID_DREAMER,
                Set.of("Raven", "players"),
                Set.of("Morphling", "players", "stealId", "inDream")));
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false,
                LUCID_DREAMER,
                Set.of("Raven", "players", "inDream"),
                Set.of("Morphling", "players", "stealId")));
    }

    @Test
    void parasiteCameraHasNoWorldLabelButReturnsAtItsMeetingSeat() {
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("players"), Set.of("Parasite"),
                Set.of("Parasite", "parasiteInside")));
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                true, Set.of("Parasite"), Set.of("Parasite"),
                Set.of("Parasite", "parasiteInside")));
        assertTrue(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("players"), Set.of("Parasite"),
                Set.of("Parasite", "parasiteInside", "inTalk")));
        assertFalse(NameTagVisibilityPolicy.concealIdentity(
                false, Set.of("players"), Set.of("Parasite"),
                Set.of("Parasite", "parasiteInside", "inTalk", "parasiteMeetingVisible")));
    }
}
