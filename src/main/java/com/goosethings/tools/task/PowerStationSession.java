package com.goosethings.tools.task;

import java.util.Arrays;
import java.util.Random;

/** Simplified sinusoidal station simulation. All limits shown by the panel are game parameters. */
public final class PowerStationSession {
    public static final int CHECK=0, CAPACITOR=1, VOLTAGE=2, FREQUENCY=3, SEQUENCE=4, CLOSE_BREAKER=5, ASSIGN=6, ENERGIZE=7;
    public static final int READY=0, WRONG_CURRENT=1, WRONG_POWER=2, WRONG_CAPACITY=3, UNDER_COMPENSATED=4,
            OVER_COMPENSATED=5, WRONG_SEQUENCE=6, WRONG_VOLTAGE=7, WRONG_FREQUENCY=8, WRONG_PHASE=9,
            NOT_CONNECTED=10, OVERLOAD=11, UNBALANCED=12, SUCCESS=13, STABLE=14;
    public static final int[] CAPACITIES={160,250,400,630,1000};
    public final double voltage, power, initialPf, capacitorUnit, phaseLimit;
    private final double[] loadP=new double[6],loadQ=new double[6];
    private final int[] assigned=new int[6];
    private int stage,feedback,errors,capacitors,capacity,phaseSequence=1,sequence=-1,rateCount;
    private boolean started,energizing;
    private long startAt,finishedAt,lastAt,clock,event,stable,rateAt;
    private double generatorVoltage,frequency,phase;
    public PowerStationSession(long seed) {
        Random r=new Random(seed);voltage=r.nextBoolean()?400:690;power=120+40*r.nextInt(7);
        initialPf=new double[]{.75,.8,.85}[r.nextInt(3)];capacitorUnit=power/16;
        generatorVoltage=voltage*.92;frequency=49.6;phase=60+r.nextInt(220);
        phaseLimit=current()*1.08;Arrays.fill(assigned,-1);
        double per=power/3;double[] fractions={.25,.75,.375,.625,.5,.5};
        for(int i=5;i>0;i--){int j=r.nextInt(i+1);double swap=fractions[i];fractions[i]=fractions[j];fractions[j]=swap;}
        for(int i=0;i<6;i++){loadP[i]=per*fractions[i];loadQ[i]=loadP[i]*tan(initialPf);}
        if(!hasCompensationSolution())throw new IllegalStateException("Unsolvable station compensation");
    }
    public void start(long now){if(!started){started=true;startAt=lastAt=now;event++;}}
    public boolean apply(int seq,int action,int item,double a,double b,long now) {
        if(!started||complete()||seq<0||seq<=sequence||action<0||action>ENERGIZE||item< -1||item>15
                ||!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>100_000||Math.abs(b)>100_000)return false;
        if(now-rateAt>=1000){rateAt=now;rateCount=0;}if(++rateCount>40)return false;
        sequence=seq;tick(now);
        if(stage==0&&action==CHECK&&item>=0&&item<CAPACITIES.length){
            if(Math.abs(a-current())>Math.max(1,current()*.01))fail(WRONG_CURRENT);
            else if(Math.abs(b-apparent())>Math.max(.5,apparent()*.01))fail(WRONG_POWER);
            else if(item!=minimumCapacity())fail(WRONG_CAPACITY);
            else{capacity=item;advance();}
        }else if(stage==1&&action==CAPACITOR&&item>=0&&item<4){capacitors^=1<<item;feedback=READY;event++;}
        else if(stage==1&&action==CHECK){int result=compensationFeedback(capacitors);if(result==READY)advance();else fail(result);}
        else if(stage==2&&action==VOLTAGE&&a>=voltage*.8&&a<=voltage*1.2){generatorVoltage=a;stable=0;feedback=READY;event++;}
        else if(stage==2&&action==FREQUENCY&&a>=49&&a<=51){frequency=a;stable=0;feedback=READY;event++;}
        else if(stage==2&&action==SEQUENCE&&(item==0||item==1)){phaseSequence=item;stable=0;feedback=READY;event++;}
        else if(stage==2&&action==CLOSE_BREAKER){int result=syncFeedback();if(result==READY&&stable>=400)advance();else fail(result==READY?WRONG_PHASE:result);}
        else if(stage==3&&action==ASSIGN&&item>=0&&item<6&&a==Math.rint(a)&&a>= -1&&a<=2){assigned[item]=(int)a;stable=0;energizing=false;feedback=READY;event++;}
        else if(stage==3&&action==ENERGIZE){int result=distributionFeedback();if(result==READY){energizing=true;stable=0;feedback=STABLE;event++;}else fail(result);}
        else return false;
        return true;
    }
    public boolean tick(long now) {
        if(!started||complete())return false;
        long delta=Math.clamp(now-lastAt,0,250);lastAt=Math.max(lastAt,now);clock=Math.min(3_600_000,clock+delta);
        if(stage==2){phase=wrap(phase+360*(frequency-50)*delta/1000.0);stable=syncFeedback()==READY?Math.min(2000,stable+delta):0;return delta>0;}
        if(stage==3&&energizing){if(distributionFeedback()!=READY){energizing=false;stable=0;fail(distributionFeedback());}
            else{stable=Math.min(2000,stable+delta);if(stable>=1500){advance();finishedAt=now;}}return delta>0;}
        return false;
    }
    private void advance(){stage++;feedback=stage==4?SUCCESS:READY;stable=0;event++;}
    private void fail(int reason){feedback=reason;errors=Math.min(100_000,errors+1);event++;}
    public boolean started(){return started;}
    public boolean complete(){return stage==4;}
    public int stage(){return stage;}
    public long elapsed(long now){return started?Math.clamp((complete()?finishedAt:now)-startAt,0,3_600_000):0;}
    public double apparent(){return power/initialPf;}
    public double current(){return power*1000/(Math.sqrt(3)*voltage*initialPf);}
    public int minimumCapacity(){for(int i=0;i<CAPACITIES.length;i++)if(CAPACITIES[i]>=apparent()*1.15)return i;throw new IllegalStateException("No capacity");}
    public static double tan(double pf){return Math.sqrt(1-pf*pf)/pf;}
    public double compensation(int mask){return capacitorUnit*mask;}
    public double reactive(){return power*tan(initialPf)-compensation(capacitors);}
    public double correctedPf(){return power/Math.hypot(power,reactive());}
    public int compensationFeedback(int mask){double q=power*tan(initialPf)-compensation(mask),pf=power/Math.hypot(power,q);
        return q<0||pf>.985?OVER_COMPENSATED:pf<.95?UNDER_COMPENSATED:READY;}
    private boolean hasCompensationSolution(){for(int mask=0;mask<16;mask++)if(compensationFeedback(mask)==READY)return true;return false;}
    public int syncFeedback(){if(phaseSequence!=0)return WRONG_SEQUENCE;if(Math.abs(generatorVoltage-voltage)>voltage*.02+1e-8)return WRONG_VOLTAGE;
        if(Math.abs(frequency-50)>.1+1e-8)return WRONG_FREQUENCY;if(Math.abs(wrap(phase))>8)return WRONG_PHASE;return READY;}
    public static double wrap(double angle){return ((angle+180)%360+360)%360-180;}
    public double[] phaseCurrents(){double[] p=new double[3],q=new double[3],result=new double[3];for(int i=0;i<6;i++)if(assigned[i]>=0){p[assigned[i]]+=loadP[i];q[assigned[i]]+=loadQ[i];}
        for(int i=0;i<3;i++)result[i]=Math.hypot(p[i],q[i]-compensation(capacitors)/3)*1000/(voltage/Math.sqrt(3));return result;}
    public int distributionFeedback(){for(int v:assigned)if(v<0)return NOT_CONNECTED;double[] i=phaseCurrents();double mean=(i[0]+i[1]+i[2])/3;
        for(double v:i)if(v>phaseLimit)return OVERLOAD;for(double v:i)if(Math.abs(v-mean)>mean*.10+1e-8)return UNBALANCED;return READY;}
    public PowerStationSnapshot snapshot(long now){return new PowerStationSnapshot(stage,feedback,errors,started,elapsed(now),clock,event,voltage,power,initialPf,capacitorUnit,capacitors,capacity,
            generatorVoltage,frequency,wrap(phase),phaseSequence,stable,phaseLimit,loadP,loadQ,assigned,phaseCurrents());}
}
