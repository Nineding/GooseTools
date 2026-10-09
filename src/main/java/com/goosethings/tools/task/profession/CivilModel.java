package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;
import java.util.Random;
import static com.goosethings.tools.task.profession.ProfessionFeedback.*;

public final class CivilModel extends ProfessionModel {
    public static final double[] INERTIA={.00012,.00025,.0005,.001}, MODULUS={.0006,.001,.0018,.003}, MASS={60,90,130,170};
    public final double initialLevel,closure,qKNm,lengthM,ePa=200e9,stressLimitMPa=160,massLimit=180;
    public final double sandSSD,stoneSSD,sandAbsorption,stoneAbsorption,sandMoisture,stoneMoisture,binderKg,wRatio=.45;
    private final double[] backsights,foresights,reportStrength,reportCuring;
    public CivilModel(long seed){
        super(TaskType.CIVIL);Random r=new Random(seed);initialLevel=100+r.nextInt(21);closure=(r.nextBoolean()?1:-1)*(.012+.003*r.nextInt(4));
        backsights=new double[]{1.2+.1*r.nextInt(7),1.4,1.3};foresights=new double[]{1.1+.1*r.nextInt(7),1.6,0};
        foresights[2]=backsights[0]-foresights[0]+backsights[1]-foresights[1]+backsights[2]-closure;
        qKNm=10+r.nextInt(21);lengthM=6+2*r.nextInt(3);
        sandSSD=650+25*r.nextInt(9);stoneSSD=950+25*r.nextInt(7);sandAbsorption=.02;stoneAbsorption=.01;
        sandMoisture=new double[]{.005,.015,.035,.055}[r.nextInt(4)];stoneMoisture=new double[]{.005,.015,.025}[r.nextInt(3)];binderKg=320+20*r.nextInt(5);
        reportStrength=new double[]{32,28,35};reportCuring=new double[]{8,8,3};
        for(int i=2;i>0;i--){int j=r.nextInt(i+1);double a=reportStrength[i],b=reportCuring[i];reportStrength[i]=reportStrength[j];reportCuring[i]=reportCuring[j];reportStrength[j]=a;reportCuring[j]=b;}
    }
    public double[] backsights(){return backsights.clone();}public double[] foresights(){return foresights.clone();}
    public double[] reportStrength(){return reportStrength.clone();}public double[] reportCuring(){return reportCuring.clone();}
    public double instrumentHeight(){return initialLevel+backsights[0];}
    public double correctedFirstLevel(){return instrumentHeight()-foresights[0]-closure/3;}
    public double reactionKN(){return qKNm*lengthM/2;}
    public double momentKNm(){return qKNm*lengthM*lengthM/8;}
    public double stressMPa(int section){return momentKNm()*1000/MODULUS[section]/1e6;}
    public double deflectionMm(int section){return 5*qKNm*1000*Math.pow(lengthM,4)/(384*ePa*INERTIA[section])*1000;}
    public double deflectionLimitMm(){return lengthM/250*1000;}
    public double sandWet(){return sandSSD/(1+sandAbsorption)*(1+sandMoisture);}
    public double stoneWet(){return stoneSSD/(1+stoneAbsorption)*(1+stoneMoisture);}
    public double addedWater(){return binderKg*wRatio-(sandWet()-sandSSD)-(stoneWet()-stoneSSD);}
    @Override public boolean acceptsChoice(int stage,int slot,int value){return switch(stage){
        case 0 -> slot==0&&value>=0&&value<=2;
        case 2 -> slot==1&&value>=0&&value<4;
        case 3 -> slot==2&&value>=0&&value<=2;
        default -> false;};}
    @Override public boolean acceptsDial(int stage,int slot,double value){return stage==1&&(slot==1&&value>=0&&value<=600||slot==2&&value>=-600&&value<=0||slot==3&&value>=0&&value<=1000);}
    @Override public ProfessionFeedback check(int stage,int[] p,double[] d,double a,double b,double c){switch(stage){
        case 0:
            if(p[0]<0)return MISSING;
            if(Math.abs(a-instrumentHeight())>.001)return CALC_A;
            // Survey elevations require millimetric absolute tolerance, not 1% of a 100 m datum.
            if(Math.abs(b-correctedFirstLevel())>.001)return CALC_B;
            if(Math.abs(c-closure)>.0005)return CALC_C;
            return p[0]!=1?LEVEL:READY;
        case 1:
            if(!near(a,reactionKN(),.1))return CALC_A;
            if(!near(b,momentKNm(),.1))return CALC_B;
            return !near(d[1],reactionKN(),.1)||!near(d[2],-reactionKN(),.1)||!near(d[3],momentKNm(),.1)?DIAGRAM:READY;
        case 2:
            if(p[1]<0)return MISSING;
            if(!near(a,stressMPa(p[1]),.1))return CALC_A;
            if(!near(b,deflectionMm(p[1]),.05))return CALC_B;
            return stressMPa(p[1])>stressLimitMPa||deflectionMm(p[1])>deflectionLimitMm()||MASS[p[1]]>massLimit?SECTION:READY;
        case 3:
            if(p[2]<0)return MISSING;
            if(!near(a,sandWet(),.1))return CALC_A;
            if(!near(b,stoneWet(),.1))return CALC_B;
            if(!near(c,addedWater(),.1))return CALC_C;
            return reportStrength[p[2]]<30||reportCuring[p[2]]<7?CONCRETE:READY;
        default:return MISSING;
    }}
}
