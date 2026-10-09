package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;
import java.util.Arrays;
import static com.goosethings.tools.task.profession.ProfessionFeedback.*;

/** Server-owned choices, timers and integration. No client completion operation exists. */
public final class ProfessionSession {
    public static final int CHOICE=0,DIAL=1,CHECK=2;
    public final ProfessionModel model;
    private final int[] choices=new int[16];
    private final double[] dials;
    private int stage,errors,sequence=-1,rateCount;
    private ProfessionFeedback feedback=READY;
    private boolean started,testing;
    private long startedAt,finishedAt,lastAt,event,stable,testElapsed,rateAt;
    private double temperature=90;
    public ProfessionSession(TaskType type,long seed){model=ProfessionModel.create(type,seed);dials=model.initialDials();Arrays.fill(choices,-1);}
    public void start(long now){if(!started){started=true;startedAt=lastAt=now;event++;}}
    public boolean apply(int seq,int expectedStage,int action,int slot,double a,double b,double c,long now){
        if(!started||complete()||seq<=sequence||seq<0||seq>100_000||expectedStage!=stage||action<0||action>CHECK
                ||slot<0||slot>=16||!finite(a)||!finite(b)||!finite(c))return false;
        if(now-rateAt>=1000){rateAt=now;rateCount=0;}if(++rateCount>40)return false;
        sequence=seq;tick(now);if(expectedStage!=stage)return false;
        if(action==CHOICE){
            if(a!=Math.rint(a)||!model.acceptsChoice(stage,slot,(int)a))return false;
            choices[slot]=(int)a;changed();
        }else if(action==DIAL){
            if(!model.acceptsDial(stage,slot,a))return false;
            dials[slot]=a;changed();
        }else{
            if(testing)return false;
            ProfessionFeedback result=model.check(stage,choices,dials,a,b,c);
            if(result!=READY)fail(result);
            else if(model instanceof TelecomModel&&stage==2&&stable<800)fail(HOLD);
            else if(model instanceof NuclearModel&&stage==2){testing=true;testElapsed=0;temperature=90;feedback=TESTING;event++;}
            else advance(now);
        }
        return true;
    }
    private static boolean finite(double v){return Double.isFinite(v)&&Math.abs(v)<=1_000_000;}
    private void changed(){feedback=READY;stable=0;testing=false;testElapsed=0;temperature=90;event++;}
    private void fail(ProfessionFeedback reason){feedback=reason;errors=Math.min(100_000,errors+1);event++;}
    private void advance(long now){stage++;stable=0;testing=false;feedback=complete()?SUCCESS:READY;if(complete())finishedAt=now;event++;}
    public boolean tick(long now){
        if(!started||complete())return false;
        long gap=Math.max(0,now-lastAt),dt=Math.min(250,gap);lastAt=Math.max(lastAt,now);
        if(model instanceof TelecomModel t&&stage==2){
            if(gap>250)stable=0;
            stable=t.evm(dials)<=.035?Math.min(2000,stable+dt):0;
            return dt>0;
        }
        if(model instanceof NuclearModel n&&stage==2&&testing){
            ProfessionFeedback condition=n.cooling(choices,dials);
            if(condition!=READY){testing=false;fail(condition);return true;}
            int failure=testElapsed<2000?-1:testElapsed<4000?0:1;
            // kW / (kJ/K) gives K/s. This is the stated 10,000 kJ/K teaching model.
            temperature+=(n.decayMW(n.timeMinutes)-n.coolingMW(dials,failure))*1000/10_000*dt/1000.0;
            testElapsed=Math.min(6000,testElapsed+dt);
            if(temperature<80||temperature>95){testing=false;fail(TEMPERATURE);}
            else if(testElapsed>=6000)advance(now);
            return dt>0;
        }
        return false;
    }
    public boolean started(){return started;}public boolean complete(){return stage==4;}public int stage(){return stage;}
    public long elapsed(long now){return started?Math.clamp((complete()?finishedAt:now)-startedAt,0,3_600_000):0;}
    public ProfessionSnapshot snapshot(long now){return new ProfessionSnapshot(stage,feedback.ordinal(),errors,started,elapsed(now),event,stable,testElapsed,temperature,choices,dials);}
}
