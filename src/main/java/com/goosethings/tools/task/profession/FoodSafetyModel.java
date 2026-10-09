package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;
import java.util.Random;
import static com.goosethings.tools.task.profession.ProfessionFeedback.*;

public final class FoodSafetyModel extends ProfessionModel {
    private final int[] hazardKinds,toolKinds,lotMasks;
    private final int[] deviations;
    public final double referenceT=70,dReference,z,qualityTemperature=82;
    public final int logReduction=6,badLot;
    public FoodSafetyModel(long seed){
        super(TaskType.FOODSAFETY);Random r=new Random(seed);dReference=new double[]{.5,1,1.5}[r.nextInt(3)];z=r.nextBoolean()?7:10;badLot=r.nextInt(4);
        hazardKinds=shuffle(new int[]{0,1,2,3},r);toolKinds=shuffle(new int[]{0,1,2,3},r);
        lotMasks=new int[6];deviations=new int[6];
        for(int i=0;i<6;i++)lotMasks[i]=(1<<r.nextInt(4))|(1<<r.nextInt(4));
        lotMasks[0]|=1<<badLot;lotMasks[1]=1<<((badLot+1)%4);
        lotMasks[2]=1<<((badLot+1)%4);deviations[2]=1;deviations[3]=2;
    }
    private static int[] shuffle(int[] a,Random r){for(int i=a.length-1;i>0;i--){int j=r.nextInt(i+1),v=a[i];a[i]=a[j];a[j]=v;}return a;}
    public int[] hazardKinds(){return hazardKinds.clone();}
    public int[] toolKinds(){return toolKinds.clone();}
    public int[] lotMasks(){return lotMasks.clone();}
    public int[] deviations(){return deviations.clone();}
    public double targetF(){return logReduction*dReference;}
    @Override public double[] initialDials(){double[] d=new double[16];d[0]=70;d[1]=1;return d;}
    @Override public boolean acceptsChoice(int stage,int slot,int value){return switch(stage){
        case 0 -> slot>=0&&slot<4&&value>=0&&value<4;
        case 2 -> slot>=4&&slot<=7&&value>=0&&value<=2 || (slot==8||slot==9)&&value>=0&&value<=2;
        case 3 -> slot>=10&&slot<=15&&value>=0&&value<=3;
        default -> false;};}
    @Override public boolean acceptsDial(int stage,int slot,double value){return stage==1&&(slot==0&&value>=65&&value<=85||slot==1&&value>=.5&&value<=30);}
    /** Piecewise-constant cold-point measurements and durations are shown explicitly. */
    public double[] coldTemperatures(double[] d){return new double[]{50,60,d[0]-6,d[0]-2,d[0],d[0]};}
    public double[] durations(double[] d){return new double[]{.5,.5,.5,.5,d[1]/2,d[1]/2};}
    public double lethality(double[] d){double[] t=coldTemperatures(d),minutes=durations(d);double f=0;
        for(int i=0;i<t.length;i++)f+=minutes[i]*Math.pow(10,(t[i]-referenceT)/z);return f;}
    public boolean affected(int batch){return (lotMasks[batch]&(1<<badLot))!=0||deviations[batch]!=0;}
    public int disposition(int batch){
        if(deviations[batch]==2)return 2;
        if((lotMasks[batch]&(1<<badLot))!=0)return 1;
        return deviations[batch]==1?3:0;
    }
    @Override public ProfessionFeedback check(int stage,int[] p,double[] d,double a,double b,double c){switch(stage){
        case 0:
            if(!selected(p,0,4))return MISSING;
            int[] controls={1,2,3,0};for(int i=0;i<4;i++)if(p[i]!=controls[hazardKinds[i]])return HAZARD;
            return READY;
        case 1:
            if(!near(a,lethality(d),.02))return CALC_A;
            if(!near(b,targetF(),.01))return CALC_B;
            return lethality(d)<targetF()-1e-9||lethality(d)>targetF()*3||d[0]>qualityTemperature?LETHALITY:READY;
        case 2:
            if(!selected(p,4,6))return MISSING;
            int[] zone={0,1,2,0};for(int i=0;i<4;i++)if(p[4+i]!=zone[toolKinds[i]])return CONTAMINATION;
            return p[8]!=1||p[9]!=1?CONTAMINATION:READY;
        case 3:
            if(!selected(p,10,6))return MISSING;
            for(int i=0;i<6;i++)if(p[10+i]!=disposition(i))return TRACE;
            return READY;
        default:return MISSING;
    }}
}
