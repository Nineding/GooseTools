package com.goosethings.tools.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrafficRulesTest {
    @Test void steeringMovesContinuouslyAndRejectsRepeatOrRoadEdges() {
        TrafficGame g=new TrafficGame(7,0,1);
        assertTrue(g.input(GameSession.DIRECTION,1,0,0));assertEquals(90,g.x);
        assertFalse(g.input(GameSession.DIRECTION,3,0,0));g.tick(10);assertEquals(94.2,g.x,.001);
        for(int i=0;i<17;i++)g.tick(10);assertEquals(150,g.x);
        assertFalse(g.input(GameSession.DIRECTION,1,0,0));assertTrue(g.input(GameSession.DIRECTION,3,0,0));
        assertFalse(g.input(GameSession.DIRECTION,0,0,0));
    }
    @Test void collisionUsesActualPositionWhileChangingLanes() {
        TrafficGame g=new TrafficGame(7,0,1);g.obstacles.add(new TrafficGame.Obstacle(1,230,0));
        g.input(GameSession.DIRECTION,1,0,0);g.tick(10);assertEquals(GameSession.LOST,g.phase);
        TrafficGame safe=new TrafficGame(7,0,1);safe.obstacles.add(new TrafficGame.Obstacle(0,230,0));
        safe.tick(10);assertEquals(GameSession.RUNNING,safe.phase);
    }
    @Test void brakeSlowsWithoutStoppingAndExpiresOrReleases() {
        TrafficGame g=new TrafficGame(7,0,1);double initial=g.speed;
        g.input(GameSession.POINT,0,0,0);for(int i=0;i<50;i++)g.tick(10);
        assertTrue(g.speed<initial*.65);assertTrue(g.speed>0);assertTrue(g.distance>0);
        assertTrue(g.brake);g.input(GameSession.POINT,1,0,0);assertFalse(g.brake);
        g.input(GameSession.POINT,0,0,0);for(int i=0;i<61;i++)g.tick(10);assertFalse(g.brake);
        assertFalse(g.input(GameSession.POINT,2,0,0));
    }
    @Test void pausingReleasesBrakeAndExcludesActivityTime() {
        GameSession s=new GameSession(1,GameType.TRAFFIC,7,0);s.apply(0,GameSession.START,0,0,0,0);
        s.apply(1,GameSession.POINT,0,0,0,0);assertTrue(((TrafficGame)s.game).brake);
        s.apply(2,GameSession.PAUSE,0,0,0,50);assertFalse(((TrafficGame)s.game).brake);
        long elapsed=s.activeMillis();s.tick(50_000);assertEquals(elapsed,s.activeMillis());
    }
    @Test void wavesHaveAnOpenLaneAndBoundedStorageForEndlessDriving() {
        for(int diff=0;diff<3;diff++)for(int seed=0;seed<20;seed++){
            TrafficGame g=new TrafficGame(seed,0,diff);
            for(int t=0;t<20_000;t++){
                // Observe the upcoming wave and choose its open lane early; preserve real road simulation.
                double next=Double.MAX_VALUE;boolean[] occupied=new boolean[3];
                for(var o:g.obstacles)if(o.y<270&&o.y> -40)next=Math.min(next,230-o.y);
                if(next<Double.MAX_VALUE){double y=230-next;for(var o:g.obstacles)if(Math.abs(o.y-y)<.01)occupied[o.lane]=true;
                    int target=0;while(occupied[target])target++;g.lane=target;g.x=30+target*60;}
                g.tick(10);assertEquals(GameSession.RUNNING,g.phase,"seed="+seed+" diff="+diff);
                assertTrue(g.obstacles.size()<=TrafficGame.MAX_OBSTACLES);assertTrue(g.speed<=175.001);
                for(var o:g.obstacles){boolean[] lanes=new boolean[3];for(var other:g.obstacles)if(Math.abs(other.y-o.y)<.01)lanes[other.lane]=true;assertFalse(lanes[0]&&lanes[1]&&lanes[2]);}
            }
            assertTrue(g.score>20_000);assertTrue(g.opponent>30);assertEquals(6+g.obstacles.size()*3,g.actors().length);
        }
    }
}
