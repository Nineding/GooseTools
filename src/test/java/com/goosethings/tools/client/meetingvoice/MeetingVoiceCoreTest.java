package com.goosethings.tools.client.meetingvoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MeetingVoiceCoreTest {
 @TempDir Path temp;
 @Test void oldConfigurationIsRetainedAndCannotDisableMeetingPrivacy()throws Exception{
  var p=temp.resolve("meetingvoice.properties");Files.writeString(p,"enabled=true\nshowSelf=true\nmeetingOnly=false\nrange=999\nbubbleScale=NaN\n");
  var c=Config.load(p);assertTrue(c.meetingOnly);assertTrue(c.showSelf);assertEquals(48,c.range);assertEquals(1,c.bubbleScale);assertTrue(Files.readString(p).contains("showSelf=true"));
 }
 @Test void previousContextCannotRefillClearedVoiceCache(){
  PlayerVoiceStateCache.clear();long epoch=PlayerVoiceStateCache.token();var id=UUID.randomUUID();short[] pcm=new short[960];Arrays.fill(pcm,(short)12000);
  PlayerVoiceStateCache.feed(epoch,id,pcm,1_000_000_000L,new Config());assertEquals(1,PlayerVoiceStateCache.size());
  PlayerVoiceStateCache.clear();PlayerVoiceStateCache.feed(epoch,id,pcm,1_100_000_000L,new Config());assertEquals(0,PlayerVoiceStateCache.size());
 }
 @Test void audioProducesSpeechThenFadesWithoutKeepingPcm(){
  var c=new Config();var meter=new Meter();short[] pcm=new short[960];Arrays.fill(pcm,(short)12000);long now=1_000_000_000L;
  meter.accept(pcm,now,c);assertTrue(meter.sample(now).level()>0);var animator=new VoiceBubbleAnimator();animator.update(meter.sample(now),false,now,c);
  assertTrue(animator.update(meter.sample(now+180_000_000),false,now+180_000_000,c).opacity()>0);
  animator.update(new Meter.Sample(0,0),false,now+2_000_000_000L,c);
  assertEquals(0,animator.update(new Meter.Sample(0,0),false,now+3_000_000_000L,c).opacity());assertTrue(meter.expired(now+3_000_000_000L));
 }
}