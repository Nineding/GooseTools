package com.goosethings.tools.dream;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class ObserverDreamPolicyTest {
 @Test void onlySpectatorsDuringDiscussionOrVotingCanObserve() {
  for(int phase=-1;phase<=4;phase++){
   assertEquals(phase==1||phase==2,ObserverDreamPolicy.allowed(Set.of("spectator"),phase));
   assertFalse(ObserverDreamPolicy.allowed(Set.of("players","Raven"),phase));
   assertFalse(ObserverDreamPolicy.allowed(Set.of("spectator","endGame"),phase));
  }
 }
 @Test void connectionOrMeetingStateCannotLeaveAnObserverActive() {
  assertTrue(ObserverDreamPolicy.valid(Set.of("spectator","spectatorDream"),1));
  assertFalse(ObserverDreamPolicy.valid(Set.of("spectatorDream"),1));
  assertFalse(ObserverDreamPolicy.valid(Set.of("spectator","spectatorDream"),0));
 }
 @Test void onlyListeningButtonsInTheirOwnMainHandSlotsAreAllowed() {
  for(int slot=0;slot<9;slot++) {
   assertEquals(slot==0,ObserverDreamPolicy.listeningControl(true,slot,true,true,false));
   assertEquals(slot==1,ObserverDreamPolicy.listeningControl(true,slot,true,false,true));
   assertFalse(ObserverDreamPolicy.listeningControl(true,slot,true,false,false));
   assertFalse(ObserverDreamPolicy.listeningControl(false,slot,true,true,true));
   assertFalse(ObserverDreamPolicy.listeningControl(true,slot,false,true,true));
  }
 }
 @Test void mapEvidenceAndOtherDreamersAreHighlighted() {
  assertFalse(ObserverDreamPolicy.highlight(0,false));
  assertTrue(ObserverDreamPolicy.highlight(1,false));
  assertTrue(ObserverDreamPolicy.highlight(2,false));
  assertTrue(ObserverDreamPolicy.highlight(3,false));
  assertTrue(ObserverDreamPolicy.highlight(1,true));
  assertFalse(ObserverDreamPolicy.highlight(3,true));
 }
}
