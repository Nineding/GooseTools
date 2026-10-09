package com.goosethings.tools.task;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class PowerStationRulesTest {
    private int sequence;private long time;
    private PowerStationSession fresh(long seed){sequence=0;time=1000;var s=new PowerStationSession(seed);s.start(time);return s;}
    private boolean act(PowerStationSession s,int action,int item,double a,double b){time+=50;return s.apply(sequence++,action,item,a,b,time);}
    private void calculate(PowerStationSession s){assertTrue(act(s,PowerStationSession.CHECK,s.minimumCapacity(),s.current(),s.apparent()));assertEquals(1,s.stage());}
    private void compensate(PowerStationSession s){int mask=0;while(s.compensationFeedback(mask)!=0)mask++;for(int bit=0;bit<4;bit++)if((mask&(1<<bit))!=0)act(s,PowerStationSession.CAPACITOR,bit,0,0);act(s,PowerStationSession.CHECK,0,0,0);assertEquals(2,s.stage());}
    private void synchronize(PowerStationSession s){
        act(s,PowerStationSession.SEQUENCE,0,0,0);act(s,PowerStationSession.VOLTAGE,0,s.voltage,0);
        act(s,PowerStationSession.FREQUENCY,0,50.1,0);
        for(int i=0;i<400&&Math.abs(s.snapshot(time).phase())>3;i++){time+=50;s.tick(time);}
        assertTrue(Math.abs(s.snapshot(time).phase())<=3);act(s,PowerStationSession.FREQUENCY,0,50,0);
        for(int i=0;i<8;i++){time+=50;s.tick(time);}assertTrue(act(s,PowerStationSession.CLOSE_BREAKER,0,0,0));assertEquals(3,s.stage());
    }
    private void balanced(PowerStationSession s){
        double[] ps=s.snapshot(time).loadP();boolean[] used=new boolean[6];int phase=0;
        for(int i=0;i<6;i++)if(!used[i])for(int j=i+1;j<6;j++)if(!used[j]&&Math.abs(ps[i]+ps[j]-s.power/3)<1e-7){used[i]=used[j]=true;act(s,PowerStationSession.ASSIGN,i,phase,0);act(s,PowerStationSession.ASSIGN,j,phase++,0);break;}
        assertEquals(3,phase);assertEquals(0,s.distributionFeedback());
    }
    @Test void nameplateUsesLineVoltageAndKilowattToAmpConversion(){var s=fresh(42);assertEquals(s.power/s.initialPf,s.apparent(),1e-10);assertEquals(s.apparent()*1000/(Math.sqrt(3)*s.voltage),s.current(),1e-10);assertEquals(s.power,Arrays.stream(s.snapshot(time).loadP()).sum(),1e-8);}
    @Test void everyRandomStationCanBeSolvedThroughAllFourStages(){
        for(int seed=0;seed<1000;seed++){var s=fresh(seed);calculate(s);compensate(s);synchronize(s);balanced(s);act(s,PowerStationSession.ENERGIZE,0,0,0);
            for(int i=0;i<29;i++){time+=50;s.tick(time);}assertFalse(s.complete());time+=50;s.tick(time);assertTrue(s.complete());
            assertEquals(PowerStationSession.SUCCESS,s.snapshot(time).feedback());long elapsed=s.elapsed(time);s.tick(time+10_000);assertEquals(elapsed,s.elapsed(time+10_000));assertFalse(act(s,PowerStationSession.ENERGIZE,0,0,0));}
    }
    @Test void wrongCalculationsAndCapacityKeepStageAndCountErrors(){var s=fresh(7);act(s,0,s.minimumCapacity(),0,s.apparent());assertEquals(1,s.snapshot(time).feedback());act(s,0,s.minimumCapacity(),s.current(),0);assertEquals(2,s.snapshot(time).feedback());act(s,0,(s.minimumCapacity()+1)%5,s.current(),s.apparent());assertEquals(3,s.snapshot(time).feedback());assertEquals(3,s.snapshot(time).errors());assertEquals(0,s.stage());calculate(s);assertEquals(3,s.snapshot(time).errors());}
    @Test void underAndOverCompensationCannotAdvance(){var s=fresh(7);calculate(s);act(s,0,0,0,0);assertEquals(4,s.snapshot(time).feedback());for(int i=0;i<4;i++)act(s,1,i,0,0);act(s,0,0,0,0);assertEquals(5,s.snapshot(time).feedback());assertEquals(1,s.stage());}
    @Test void synchronizationGatesSequenceVoltageFrequencyAndAngle(){var s=fresh(8);calculate(s);compensate(s);act(s,5,0,0,0);assertEquals(6,s.snapshot(time).feedback());act(s,4,0,0,0);act(s,5,0,0,0);assertEquals(7,s.snapshot(time).feedback());act(s,2,0,s.voltage,0);act(s,5,0,0,0);assertEquals(8,s.snapshot(time).feedback());act(s,3,0,50,0);act(s,5,0,0,0);assertEquals(9,s.snapshot(time).feedback());assertEquals(2,s.stage());}
    @Test void stableWindowRequiresContinuousTimeAndResetsAfterAdjustment(){var s=fresh(9);calculate(s);compensate(s);act(s,4,0,0,0);act(s,2,0,s.voltage,0);act(s,3,0,50.1,0);while(Math.abs(s.snapshot(time).phase())>2){time+=50;s.tick(time);}act(s,3,0,50,0);time+=300;s.tick(time);assertTrue(s.snapshot(time).stable()<400);act(s,5,0,0,0);assertEquals(2,s.stage());act(s,2,0,s.voltage,0);assertEquals(0,s.snapshot(time).stable());}
    @Test void disconnectedAndOverloadedDistributionCannotRestoreSupply(){var s=fresh(7);calculate(s);compensate(s);synchronize(s);act(s,7,0,0,0);assertEquals(10,s.snapshot(time).feedback());for(int i=0;i<6;i++)act(s,6,i,0,0);act(s,7,0,0,0);assertEquals(11,s.snapshot(time).feedback());assertFalse(s.complete());balanced(s);act(s,7,0,0,0);time+=250;s.tick(time);act(s,6,0,-1,0);for(int i=0;i<10;i++){time+=250;s.tick(time);}assertFalse(s.complete());assertEquals(0,s.snapshot(time).stable());}
    @Test void currentsUseEachPhasesComplexPowerAndEqualCompensation(){var s=fresh(7);calculate(s);compensate(s);synchronize(s);balanced(s);for(double current:s.phaseCurrents())assertEquals(Math.hypot(s.power,s.reactive())*1000/(Math.sqrt(3)*s.voltage),current,1e-8);}
    @Test void underRatedButUnbalancedLoadsStillFailTheBalanceInterlock(){
        var s=fresh(7);calculate(s);compensate(s);synchronize(s);boolean found=false;
        for(int code=0;code<729&&!found;code++){int value=code;for(int i=0;i<6;i++){act(s,6,i,value%3,0);value/=3;}found=s.distributionFeedback()==PowerStationSession.UNBALANCED;}
        assertTrue(found,"expected an unbalanced configuration below individual ratings");act(s,7,0,0,0);assertEquals(12,s.snapshot(time).feedback());assertFalse(s.complete());
    }
    @Test void nonfiniteStaleWrongStageAndFloodedInputCannotBypassValidation(){var s=fresh(7);assertFalse(s.apply(0,0,0,Double.NaN,0,time));assertFalse(s.apply(0,0,0,0,Double.POSITIVE_INFINITY,time));assertFalse(act(s,7,0,0,0));assertFalse(s.apply(0,0,0,0,0,time));assertFalse(s.apply(99,6,7,0,0,time));for(int i=100;i<145;i++)s.apply(i,7,0,0,0,time);assertFalse(s.apply(146,0,s.minimumCapacity(),s.current(),s.apparent(),time));assertEquals(0,s.stage());}
    @Test void snapshotsOwnTheirArraysAndRejectMalformedLengths(){var s=fresh(7);var snap=s.snapshot(time);double original=snap.loadP()[0];double[] p=snap.loadP();p[0]=0;assertEquals(original,snap.loadP()[0]);int[] a=snap.assignments();a[0]=2;assertEquals(-1,snap.assignments()[0]);assertThrows(IllegalArgumentException.class,()->new PowerStationSnapshot(0,0,0,false,0,0,0,400,120,.8,7.5,0,0,400,50,0,0,0,200,new double[7],new double[6],new int[6],new double[3]));}
    @Test void catchupAndNegativeClockCannotArtificiallyFinishStableSupply(){var s=fresh(7);calculate(s);compensate(s);synchronize(s);balanced(s);act(s,7,0,0,0);time+=100_000;s.tick(time);assertEquals(250,s.snapshot(time).stable());s.tick(time-1000);assertEquals(250,s.snapshot(time).stable());assertFalse(s.complete());}
}
