package com.goosethings.tools.intro;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoleIntroTwoActTest {
    @Test void shuffledRosterNeverCentersOrReordersTheHero() {
        var all = new ArrayList<RoleIntroPolicy.Player>();
        for(int i=1;i<=21;i++) all.add(new RoleIntroPolicy.Player(new UUID(0,i),i,Set.of("players","good","p"+i)));
        Collections.shuffle(all,new Random(42));
        for(var hero:all) {
            var roster=RoleIntroPolicy.roster(hero,all);
            for(int i=0;i<21;i++) assertEquals(new UUID(0,i+1),roster.get(i).id());
            assertEquals(hero.id(),roster.get(hero.seat()-1).id());
        }
    }
    @Test void actsAndMapReturnTotalExactlyTwelveSeconds() {
        assertEquals(100,RoleIntroScene.FIRST_ACT_TICKS);
        assertEquals(240,RoleIntroScene.DURATION_TICKS+RoleIntroScene.RETURN_TICKS);
        assertEquals(0,RoleIntroScene.expansion(4_400_000_000L),.00001F);
        assertEquals(1,RoleIntroScene.expansion(5_000_000_000L));
        assertTrue(RoleIntroScene.ease(.25F) < RoleIntroScene.ease(.5F));
        assertTrue(RoleIntroScene.ease(.5F) < RoleIntroScene.ease(.75F));
        assertEquals(0,RoleIntroScene.ease(0),.0001F);
        assertEquals(1,RoleIntroScene.ease(1),.0001F);
        int narrow=RoleIntroScene.stagePixel(0,"Goose",.04,.7,0)&255;
        int wide=RoleIntroScene.stagePixel(0,"Goose",.04,.7,1)&255;
        assertTrue(wide>narrow+60);
    }
    @Test void revealIsContinuousAndPremultipliedWithoutMovingTheTwelveSecondHandoff() {
        assertEquals(0,RoleIntroScene.rosterOpacity(4_700_000_000L));
        assertEquals(.5F,RoleIntroScene.rosterOpacity(5_150_000_000L),.0001F);
        assertEquals(1,RoleIntroScene.rosterOpacity(5_600_000_000L));
        assertEquals(0x80808080,RoleIntroScene.premultipliedOpacity(.5F));
        assertEquals(0,RoleIntroScene.premultipliedOpacity(0));
        assertEquals(0xFFFFFFFF,RoleIntroScene.premultipliedOpacity(1));
        float previous=0;
        for(long t=4_700_000_000L;t<=5_600_000_000L;t+=10_000_000L) {
            float next=RoleIntroScene.rosterOpacity(t);
            assertTrue(next>=previous && next-previous<.02F);previous=next;
        }
        int unchanged=RoleIntroScene.stagePixel(3,"Assassin",.81,.72,0,true);
        assertEquals(RoleIntroScene.stagePixel(3,"Assassin",.81,.72,0),unchanged);
        int right=RoleIntroScene.stagePixel(3,"Assassin",.81,.72,1,true);
        int left=RoleIntroScene.stagePixel(3,"Assassin",.32,.72,1,true);
        int pink=RoleIntroScene.stagePixel(3,"Assassin",.32,.72,1,false);
        assertTrue((right&255)<(right>>16&255)/2);
        assertTrue(Math.abs((left&255)-(pink&255))<3);
        assertEquals(RoleIntroScene.stagePixel(3,"Assassin",.81,.1,1),RoleIntroScene.stagePixel(3,"Assassin",.81,.1,1,true));
    }
    @Test void rowsFitLabelsAndKeepEverySeatWithoutCollisions() {
        for(int width:new int[]{320,427,640,960}) for(int height:new int[]{180,240,360,540}) {
            float ui=Math.max(.55F,Math.min(width/640F,height/360F));
            float nameScale=.8F*ui;
            for(int count=1;count<=21;count++) {
                float labelWidth=110*nameScale;
                var slots=RoleIntroScene.grid(width*.06F,width*.94F,height*.29F,height*.91F,count,labelWidth,nameScale);
                assertEquals(count,slots.size());
                for(int i=0;i<count;i++) {
                    var p=slots.get(i);
                    assertTrue(p.center()-p.step()*.5F>=0);
                    assertTrue(p.center()+p.step()*.5F<=width);
                    assertTrue(p.floor()<=height);
                    assertTrue(p.floor()-p.scale()*2.05F-14*nameScale>=height*.2F);
                    if(i>0) {
                        var prev=slots.get(i-1);
                        assertTrue(p.floor()>=prev.floor());
                        if(p.floor()==prev.floor()) assertTrue(p.center()-prev.center()>=labelWidth-1);
                    }
                }
            }
        }
    }
    @Test void mapNamesAreLocalizedAndUseConfiguredColours() {
        assertEquals("text.mapselect9",RoleIntroMap.of(9,false).key());
        assertEquals(0x8CE8FF,RoleIntroMap.of(9,false).color());
        assertEquals(0x7BCFA0,RoleIntroMap.of(11,false).color());
        assertEquals("text.ggd.map.eagleton.simplify",RoleIntroMap.of(11,false).key());
        assertEquals("text.ggd.map.eagleton.full",RoleIntroMap.of(11,true).key());
        assertTrue(RoleIntroMap.of(0,false).key().isEmpty());
    }
}
