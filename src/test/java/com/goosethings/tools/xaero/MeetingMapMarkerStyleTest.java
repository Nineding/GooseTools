package com.goosethings.tools.xaero;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MeetingMapMarkerStyleTest {
 @Test void nearbyBodyYieldsToSelfButDistantBodyRemains(){
  assertTrue(MeetingMapMarkerStyle.hideBody(10,20,10,20));
  assertTrue(MeetingMapMarkerStyle.hideBody(16,20,10,20));
  assertFalse(MeetingMapMarkerStyle.hideBody(16.01,20,10,20));
  assertFalse(MeetingMapMarkerStyle.hideBody(15,25,10,20));
 }
 @Test void artHasTransparentCornersAndPinTipIsOnRecordedCoordinate(){
  for(boolean pin:new boolean[]{true,false}){
   var pixels=new java.util.HashMap<String,Integer>();
   MeetingMapMarkerStyle.draw(pin,(x,y,c)->{assertNotEquals(0,c>>>24);pixels.put(x+","+y,c);});
   assertTrue(pixels.size()>30);assertFalse(pixels.containsKey("-7,-17"));
   if(pin){assertTrue(pixels.containsKey("0,-1"));assertFalse(pixels.containsKey("0,0"));}
  }
 }
}
