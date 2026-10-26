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
 @Test void mapEvidenceAndOtherDreamersAreHighlighted() {
  assertFalse(ObserverDreamPolicy.highlight(0,false));
  assertTrue(ObserverDreamPolicy.highlight(1,false));
  assertTrue(ObserverDreamPolicy.highlight(2,false));
  assertTrue(ObserverDreamPolicy.highlight(3,false));
  assertTrue(ObserverDreamPolicy.highlight(1,true));
  assertFalse(ObserverDreamPolicy.highlight(3,true));
 }
}
