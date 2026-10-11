package com.goosethings.tools.task;
import com.goosethings.tools.task.profession.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Test-only independent numerical solutions feed the actual server-owned sessions. */
final class ArtisanSolver {
    static void solve(TaskSession task) {
        if(task.station()!=null){new ArtisanSolver().power(task.station());return;}
        Runner r=new Runner(task.profession());
        switch(task.type){case TELECOM->telecom(r);case NUCLEAR->nuclear(r);case FOODSAFETY->food(r);case CIVIL->civil(r);default->throw new IllegalStateException();}
        assertTrue(task.complete());
    }
    private void power(PowerStationSession s){
        sequence=0;time=TaskServer.now();calculate(s);compensate(s);synchronize(s);balanced(s);act(s,PowerStationSession.ENERGIZE,0,0,0);
        for(int i=0;i<30;i++){time+=50;s.tick(time);}assertTrue(s.complete());
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
    private static final class Runner {
        final ProfessionSession s;int sequence;long now;
        Runner(ProfessionSession current){s=current;now=TaskServer.now();}
        void choice(int slot,int value){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.CHOICE,slot,value,0,0,now));}
        void dial(int slot,double value){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.DIAL,slot,value,0,0,now));}
        void check(double a,double b,double c){assertTrue(s.apply(sequence++,s.stage(),ProfessionSession.CHECK,0,a,b,c,now));}
        void waitTicks(long millis){for(long i=0;i<millis;i+=50){now+=Math.min(50,millis-i);s.tick(now);}}
    }
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

}
