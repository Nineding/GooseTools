package com.goosethings.tools.task;

import com.goosethings.tools.task.profession.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfessionRulesTest {
    @TestFactory Stream<DynamicTest> randomQuestionsHaveCompleteExecutableSolutions(){
        return Stream.of(TaskType.TELECOM,TaskType.NUCLEAR,TaskType.FOODSAFETY,TaskType.CIVIL).flatMap(type->IntStream.range(0,1000)
                .mapToObj(seed->DynamicTest.dynamicTest(type.id+" seed "+seed,()->solve(type,seed))));
    }
    /** The test derives results independently from public nameplates, not model.check or an answer method. */
    private static void solve(TaskType type,long seed){Runner r=new Runner(type,seed);switch(type){
        case TELECOM->telecom(r);case NUCLEAR->nuclear(r);case FOODSAFETY->food(r);case CIVIL->civil(r);default->throw new IllegalStateException();}
        assertTrue(r.s.complete());assertEquals(4,r.s.stage());assertTrue(r.s.elapsed(r.now)>=0);
        assertFalse(r.s.apply(++r.sequence,4,ProfessionSession.CHECK,0,0,0,0,r.now));
    }
    private static void telecom(Runner r){TelecomModel m=(TelecomModel)r.s.model;
        double passive=.25*m.lengthKm+2*.5+4*.1;int tx=-1,att=-1;double received=0;
        outer:for(int i=0;i<3;i++)for(int j=0;j<4;j++){double pr=new double[]{-1,2,5}[i]-passive-new double[]{0,3,6,9}[j];if(pr>=-23&&pr<=-13){tx=i;att=j;received=pr;break outer;}}
        assertTrue(tx>=0);r.choice(0,tx);r.choice(1,att);r.check(received,received+26,0);assertEquals(1,r.s.stage());
        modulation(r,m);assertEquals(2,r.s.stage());
        double[][] pilots=m.constellation(r.s.snapshot(r.now).dials());
        double vx=pilots[0][0]-pilots[1][0],vy=pilots[0][1]-pilots[1][1],gain=Math.hypot(vx,vy)/Math.sqrt(2);
        r.dial(0,-Math.toDegrees(Math.atan2(vy,vx)));r.dial(1,1/gain);
        r.dial(2,-Arrays.stream(pilots).mapToDouble(p->p[0]).average().orElseThrow());r.dial(3,-Arrays.stream(pilots).mapToDouble(p->p[1]).average().orElseThrow());
        r.check(0,0,0);assertEquals(ProfessionFeedback.HOLD.ordinal(),r.s.snapshot(r.now).feedback());assertEquals(2,r.s.stage());
        r.waitTicks(850);r.check(0,0,0);assertEquals(3,r.s.stage());
        int[] assignment=null;for(int code=0;code<4096;code++){int n=code;int[] p=new int[6];for(int i=0;i<6;i++){p[i]=n%4;n/=4;}
            boolean valid=true;for(int[] e:m.edges())if(Math.abs(p[e[0]]-p[e[1]])<2){valid=false;break;}if(valid){assignment=p;break;}}
        assertNotNull(assignment);for(int i=0;i<6;i++)r.choice(5+i,assignment[i]);r.check(0,0,0);
    }
    private static void modulation(Runner r,TelecomModel m){
        double capacity=5*Math.log(1+Math.pow(10,m.snrDb/10))/Math.log(2);int modulation=-1,code=-1,symbol=-1;double net=0;
        outer:for(int mod=0;mod<2;mod++)for(int fec=0;fec<2;fec++)for(int rate=0;rate<4;rate++){
            double value=(rate+1)*(mod==0?2:4)*(fec==0?.5:.75)*.9;
            if(m.snrDb>=(mod==0?6:14)&&value>=m.demandMbps&&value<=capacity*.8&&(rate+1)*1.25<=5){modulation=mod;code=fec;symbol=rate;net=value;break outer;}}
        assertTrue(modulation>=0);r.choice(2,modulation);r.choice(3,code);r.choice(4,symbol);r.check(capacity,net,0);
    }
    private static void nuclear(Runner r){NuclearModel m=(NuclearModel)r.s.model;int[] components=m.components();
        for(int i=0;i<3;i++)r.choice(i,components[i]);r.choice(3,1);r.choice(4,0);r.check(m.neutronsProduced/1000.,0,0);assertEquals(1,r.s.stage());
        double heat=m.initialMW/Math.pow(1+m.timeMinutes/60,.2),flow=heat*1000/(4.2*m.deltaT);int pump=0;
        while(new double[]{150,225,300,450,600}[pump]<flow*1.1)pump++;r.choice(5,pump);r.check(heat,flow,0);assertEquals(2,r.s.stage());
        r.choice(6,0);r.choice(7,1);r.choice(8,0);r.choice(9,1);r.choice(10,1);r.choice(11,1);
        double selected=Math.ceil(flow*1.1/5)*5;r.dial(0,selected);r.dial(1,selected);r.check(0,0,0);assertEquals(2,r.s.stage());
        r.waitTicks(5950);assertEquals(2,r.s.stage());r.waitTicks(50);assertEquals(3,r.s.stage());
        r.choice(12,2);r.choice(13,2);r.dial(2,m.requiredMinutes);
        double dose=0;double[] distances={3,3.5,4},weights={.5,.3,.2};for(int i=0;i<3;i++)dose+=m.rateAt1m/(distances[i]*distances[i])*m.requiredMinutes*weights[i]/60*.2;
        r.check(dose,0,0);
    }
    private static void food(Runner r){FoodSafetyModel m=(FoodSafetyModel)r.s.model;int[] hazards=m.hazardKinds(),control={1,2,3,0};
        for(int i=0;i<4;i++)r.choice(i,control[hazards[i]]);r.check(0,0,0);assertEquals(1,r.s.stage());
        double target=6*m.dReference,hold=Math.ceil(target*2)/2;r.dial(0,70);r.dial(1,hold);
        double f=hold;for(double temp:new double[]{50,60,64,68})f+=.5*Math.pow(10,(temp-70)/m.z);
        r.check(f,target,0);assertEquals(2,r.s.stage());int[] tools=m.toolKinds(),zone={0,1,2,0};for(int i=0;i<4;i++)r.choice(4+i,zone[tools[i]]);
        r.choice(8,1);r.choice(9,1);r.check(0,0,0);assertEquals(3,r.s.stage());
        int[] lots=m.lotMasks(),deviations=m.deviations();for(int i=0;i<6;i++)r.choice(10+i,deviations[i]==2?2:(lots[i]&(1<<m.badLot))!=0?1:deviations[i]==1?3:0);r.check(0,0,0);
    }
    private static void civil(Runner r){CivilModel m=(CivilModel)r.s.model;double[] bs=m.backsights(),fs=m.foresights();double error=0;
        for(int i=0;i<3;i++)error+=bs[i]-fs[i];double hi=m.initialLevel+bs[0];r.choice(0,1);r.check(hi,hi-fs[0]-error/3,error);assertEquals(1,r.s.stage());
        double reaction=m.qKNm*m.lengthM/2,moment=m.qKNm*m.lengthM*m.lengthM/8;
        r.dial(1,reaction);r.dial(2,-reaction);r.dial(3,moment);r.check(reaction,moment,0);assertEquals(2,r.s.stage());
        int section=-1;double stress=0,deflect=0;
        for(int i=0;i<4;i++){double sigma=moment*1000/CivilModel.MODULUS[i]/1e6,delta=5*m.qKNm*1000*Math.pow(m.lengthM,4)/(384*200e9*CivilModel.INERTIA[i])*1000;
            if(sigma<=160&&delta<=m.lengthM*1000/250&&CivilModel.MASS[i]<=180){section=i;stress=sigma;deflect=delta;break;}}
        assertTrue(section>=0);r.choice(1,section);r.check(stress,deflect,0);assertEquals(3,r.s.stage());
        double sandOD=m.sandSSD/(1+m.sandAbsorption),stoneOD=m.stoneSSD/(1+m.stoneAbsorption);
        double fine=sandOD*(1+m.sandMoisture),coarse=stoneOD*(1+m.stoneMoisture);
        double water=m.binderKg*.45-sandOD*(m.sandMoisture-m.sandAbsorption)-stoneOD*(m.stoneMoisture-m.stoneAbsorption);
        double[] strength=m.reportStrength(),cure=m.reportCuring();int report=0;while(strength[report]<30||cure[report]<7)report++;
        r.choice(2,report);r.check(fine,coarse,water);
    }
    @Test void rejectUntrustedInputsAndOldStages(){Runner r=new Runner(TaskType.TELECOM,1);
        assertFalse(r.s.apply(0,0,0,0,Double.NaN,0,0,0));assertFalse(r.s.apply(0,0,0,0,Double.POSITIVE_INFINITY,0,0,0));
        assertFalse(r.s.apply(0,0,0,16,0,0,0,0));assertFalse(r.s.apply(0,1,0,0,0,0,0,0));
        r.choice(0,0);assertFalse(r.s.apply(0,0,0,0,1,0,0,0));assertFalse(r.s.apply(1,0,0,0,1.5,0,0,0));
        assertFalse(r.s.apply(2,0,1,0,0,0,0,0));assertFalse(r.s.apply(100001,0,0,0,1,0,0,0));
        assertEquals(0,r.s.stage());assertEquals(0,r.s.snapshot(0).errors());
    }
    @Test void snapshotsOwnTheirArraysAndRejectMalformedData(){Runner r=new Runner(TaskType.NUCLEAR,0);var s=r.s.snapshot(0);
        int[] picks=s.choices();picks[0]=15;double[] d=s.dials();d[0]=500;assertEquals(-1,s.choices()[0]);assertEquals(0,s.dials()[0]);
        assertThrows(IllegalArgumentException.class,()->new ProfessionSnapshot(5,0,0,true,0,0,0,0,90,new int[16],new double[16]));
        double[] nan=new double[16];nan[3]=Double.NaN;assertThrows(IllegalArgumentException.class,()->new ProfessionSnapshot(0,0,0,true,0,0,0,0,90,new int[16],nan));
    }
    @Test void inventoryTrialCannotBypassProfessionalChecks(){TaskSession s=new TaskSession(1,TaskType.TELECOM,0,0);assertTrue(s.apply(0,TaskSession.READY,-1,0,0,0,0,0));
        assertFalse(s.apply(1,TaskSession.HIT,0,0,0,0,0,0));assertFalse(s.complete());assertEquals(0,s.progress());assertTrue(s.profession().started());}
    @Test void professionalEnumsAppendWithoutChangingOldIds(){assertEquals(9,TaskType.POWERSTATION.ordinal());assertEquals(10,TaskType.TELECOM.ordinal());assertEquals(13,TaskType.CIVIL.ordinal());}
    @Test void unstartedSessionsAndSpamCannotAdvance(){ProfessionSession s=new ProfessionSession(TaskType.FOODSAFETY,0);
        assertFalse(s.apply(0,0,0,0,1,0,0,0));s.start(0);
        for(int i=0;i<40;i++)assertTrue(s.apply(i,0,0,0,1,0,0,0));assertFalse(s.apply(41,0,0,0,2,0,0,0));assertTrue(s.apply(42,0,0,0,2,0,0,1000));}
    @Test void foodModelDistinguishesHeatReadingAndRejectsIncorrectBatchDisposition(){FoodSafetyModel m=new FoodSafetyModel(0);int[] p=new int[16];Arrays.fill(p,-1);double[] d=m.initialDials();d[0]=70;d[1]=6*m.dReference;
        double heaterF=0;double[] ts=m.coldTemperatures(d),mins=m.durations(d);for(int i=0;i<6;i++)heaterF+=mins[i]*Math.pow(10,(ts[i]+4-70)/m.z);
        assertEquals(ProfessionFeedback.CALC_A,m.check(1,p,d,heaterF,m.targetF(),0));
        for(int i=0;i<6;i++)p[10+i]=m.disposition(i);assertEquals(ProfessionFeedback.READY,m.check(3,p,d,0,0,0));
        p[12]=0;assertEquals(ProfessionFeedback.TRACE,m.check(3,p,d,0,0,0));assertEquals(3,m.disposition(2));assertEquals(2,m.disposition(3));}
    @Test void civilSurveyCannotUsePercentToleranceOfDatum(){CivilModel m=new CivilModel(1);int[] p=new int[16];p[0]=1;
        assertEquals(ProfessionFeedback.CALC_A,m.check(0,p,new double[16],m.instrumentHeight()+.1,m.correctedFirstLevel(),m.closure));
        assertEquals(ProfessionFeedback.CALC_B,m.check(0,p,new double[16],m.instrumentHeight(),m.correctedFirstLevel()+.1,m.closure));}
    @Test void nuclearRedundancyAndDoseAreIndependentChecks(){NuclearModel m=new NuclearModel(7);int[] p=new int[16];Arrays.fill(p,-1);p[5]=4;for(int i=6;i<=11;i++)p[i]=1;
        double[] d=m.initialDials();d[0]=d[1]=m.requiredFlow()*1.15;assertEquals(ProfessionFeedback.INDEPENDENCE,m.cooling(p,d));p[6]=p[8]=0;
        assertEquals(ProfessionFeedback.READY,m.cooling(p,d));d[0]=0;assertEquals(ProfessionFeedback.COOLING,m.cooling(p,d));
        p[12]=0;p[13]=0;d[2]=m.requiredMinutes;double dose=m.integratedDose(0,0,d[2]);assertEquals(ProfessionFeedback.DOSE,m.check(3,p,d,dose,0,0));}
    @Test void iqRequiresContinuousElapsedServerTime(){Runner r=new Runner(TaskType.TELECOM,5);TelecomModel m=(TelecomModel)r.s.model;
        double pr=-1-m.lossDb-3;r.choice(0,0);r.choice(1,1);r.check(pr,pr+26,0);modulation(r,m);
        r.dial(0,-m.rawPhase);r.dial(1,1/m.rawGain);r.dial(2,-m.rawI);r.dial(3,-m.rawQ);
        r.now=1_000_000;r.s.tick(r.now);assertTrue(r.s.snapshot(r.now).stable()<=250);r.check(0,0,0);assertEquals(2,r.s.stage());
        r.waitTicks(850);r.check(0,0,0);assertEquals(3,r.s.stage());}
    private static final class Runner {
        final ProfessionSession s;int sequence;long now;
        Runner(TaskType type,long seed){s=new ProfessionSession(type,seed);s.start(0);}
        void choice(int slot,int value){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.CHOICE,slot,value,0,0,now));}
        void dial(int slot,double value){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.DIAL,slot,value,0,0,now));}
        void check(double a,double b,double c){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.CHECK,0,a,b,c,now));}
        void waitTicks(long millis){for(long i=0;i<millis;i+=50){now+=Math.min(50,millis-i);s.tick(now);}}
    }
}
