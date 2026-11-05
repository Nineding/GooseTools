package com.goosethings.tools.outfit;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class OutfitRulesTest {
 @Test void missingOrNormalScorePreservesExistingOutfits(){assertTrue(OutfitRules.enabled(0));assertTrue(OutfitRules.enabled(-1));assertFalse(OutfitRules.enabled(1));}
 @Test void backupStandsAndMapCorpsesAreNotMeetingStandIns(){assertFalse(OutfitRules.meetingCorpse(Set.of("cloneArmorStand","p1")));assertFalse(OutfitRules.meetingCorpse(Set.of("deadbody","ggd.mapCorpse")));assertTrue(OutfitRules.meetingCorpse(Set.of("talk","dead")));}
 @Test void reporterAndTestSpectatorDoNotNeedAPlayerIndex(){assertTrue(OutfitRules.occupantMatches(Set.of("host"),Set.of("host","spectator","Debugger")));assertEquals(0,OutfitRules.playerIndex(Set.of("host")));}
 @Test void rotatingSeatDoesNotBecomeStablePlayerIdentity(){assertTrue(OutfitRules.occupantMatches(Set.of("one"),Set.of("1","p9")));assertFalse(OutfitRules.occupantMatches(Set.of("one"),Set.of("2","p1")));}
 @Test void explicitOwnerWinsOverAnOldSeatTag(){assertEquals(9,OutfitRules.playerIndex(Set.of("one","p9")));assertTrue(OutfitRules.occupantMatches(Set.of("one","p9"),Set.of("host","p9")));assertFalse(OutfitRules.occupantMatches(Set.of("one","p9"),Set.of("1","p1")));}
 @Test void shotAndSeatIdentityAreRecognized(){assertTrue(OutfitRules.meetingCorpse(Set.of("talk","ggd.meetingShotCorpse")));assertEquals(21,OutfitRules.corpseSeat(Set.of("twentyone")));assertEquals(4,OutfitRules.corpseSeat(Set.of("four")));assertEquals(0,OutfitRules.corpseSeat(Set.of("talk")));}
}
