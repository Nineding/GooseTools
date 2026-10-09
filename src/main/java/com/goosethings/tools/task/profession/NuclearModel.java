package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;
import java.util.Random;
import static com.goosethings.tools.task.profession.ProfessionFeedback.*;

public final class NuclearModel extends ProfessionModel {
    public static final double[] PUMP_FLOW = {150,225,300,450,600}, TRANSMISSION = {1,.5,.2};
    private static final double[][] DISTANCE = {{1,1.5,2},{2,2.5,3},{3,3.5,4}};
    private final int[] components;
    public final double neutronsProduced,neutronsLost=1000,initialMW,timeMinutes,deltaT,cp=4.2,rateAt1m;
    public final int requiredMinutes;
    public final double doseBudget;
    public NuclearModel(long seed) {
        super(TaskType.NUCLEAR);Random r=new Random(seed);neutronsProduced=940+r.nextInt(51);
        initialMW=12+r.nextInt(9);timeMinutes=new int[]{10,30,60}[r.nextInt(3)];deltaT=20+5*r.nextInt(3);
        rateAt1m=100+25*r.nextInt(5);requiredMinutes=2+r.nextInt(3);
        components=new int[]{0,1,2};for(int i=2;i>0;i--){int j=r.nextInt(i+1),v=components[i];components[i]=components[j];components[j]=v;}
        doseBudget=integratedDose(2,2,requiredMinutes)*1.15;
    }
    public int[] components() { return components.clone(); }
    /** A supplied illustrative curve, not a universal reactor decay correlation. */
    public double decayMW(double minutes) { return initialMW/Math.pow(1+minutes/60,.2); }
    public double requiredFlow() { return decayMW(timeMinutes)*1000/(cp*deltaT); }
    public int minimumPump() { for(int i=0;i<PUMP_FLOW.length;i++)if(PUMP_FLOW[i]>=requiredFlow()*1.1)return i;throw new IllegalStateException("No pump"); }
    public double[] routeDistances(int route) { return DISTANCE[route].clone(); }
    public double integratedDose(int route,int shield,double minutes) {
        double[] weights={.5,.3,.2};double sum=0;
        for(int i=0;i<3;i++)sum+=rateAt1m/(DISTANCE[route][i]*DISTANCE[route][i])*minutes*weights[i]/60;
        return sum*TRANSMISSION[shield];
    }
    @Override public double[] initialDials(){double[] d=new double[16];d[2]=requiredMinutes;return d;}
    @Override public boolean acceptsChoice(int stage,int slot,int value) {
        return switch(stage) {
            case 0 -> slot>=0&&slot<=2&&value>=0&&value<=2 || slot==3&&value>=0&&value<=1 || slot==4&&value>=0&&value<=2;
            case 1 -> slot==5&&value>=0&&value<PUMP_FLOW.length;
            case 2 -> slot>=6&&slot<=11&&value>=0&&value<=1;
            case 3 -> (slot==12||slot==13)&&value>=0&&value<=2;
            default -> false;
        };
    }
    @Override public boolean acceptsDial(int stage,int slot,double value) {
        return stage==2&&(slot==0||slot==1)&&value>=0&&value<=600
                || stage==3&&slot==2&&value>=1&&value<=8;
    }
    public ProfessionFeedback cooling(int[] p,double[] d) {
        if(!selected(p,6,6)||p[5]<0)return MISSING;
        if(p[6]==p[7]||p[8]==p[9]||p[10]!=1||p[11]!=1)return INDEPENDENCE;
        double heat=decayMW(timeMinutes);
        for(int i=0;i<2;i++){double removed=d[i]*cp*deltaT/1000;
            if(d[i]>PUMP_FLOW[p[5]]||removed<heat*1.1-1e-9||removed>heat*1.25+1e-9)return COOLING;}
        return READY;
    }
    public double coolingMW(double[] d,int failedBranch){double flow=(failedBranch==0?0:d[0])+(failedBranch==1?0:d[1]);return flow*cp*deltaT/1000;}
    @Override public ProfessionFeedback check(int stage,int[] p,double[] d,double a,double b,double c) {
        switch(stage){
            case 0:
                if(!selected(p,0,5))return MISSING;
                if(!near(a,neutronsProduced/neutronsLost,.0005))return CALC_A;
                for(int i=0;i<3;i++)if(p[i]!=components[i])return CIRCUIT;
                if(p[3]!=1)return CIRCUIT;
                return p[4]!=0?SUBCRITICAL:READY;
            case 1:
                if(p[5]<0)return MISSING;
                if(!near(a,decayMW(timeMinutes),.01))return CALC_A;
                if(!near(b,requiredFlow(),.1))return CALC_B;
                return p[5]!=minimumPump()?PUMP:READY;
            case 2: return cooling(p,d);
            case 3:
                if(!selected(p,12,2))return MISSING;
                double dose=integratedDose(p[12],p[13],d[2]);
                if(!near(a,dose,.001))return CALC_A;
                return d[2]<requiredMinutes||dose>doseBudget+1e-9?DOSE:READY;
            default:return MISSING;
        }
    }
}
