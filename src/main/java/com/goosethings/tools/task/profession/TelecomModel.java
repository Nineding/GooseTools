package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;
import java.util.Random;
import static com.goosethings.tools.task.profession.ProfessionFeedback.*;

public final class TelecomModel extends ProfessionModel {
    public static final double[] TX_DBM = {-1, 2, 5}, ATTENUATION_DB = {0, 3, 6, 9};
    public static final double[] CODE_RATES = {.5, .75}, SYMBOL_RATES = {1, 2, 3, 4};
    public final double lengthKm, lossDb, snrDb, rawPhase, rawGain, rawI, rawQ;
    public final double sensitivity = -26, overload = -13, bandwidthMHz = 5, demandMbps;
    private final int[][] edges;
    public TelecomModel(long seed) {
        super(TaskType.TELECOM); Random r = new Random(seed);
        lengthKm = 40 + r.nextInt(26); lossDb = .25 * lengthKm + 2 * .5 + 4 * .1;
        snrDb = new double[]{8, 12, 16, 20, 24}[r.nextInt(5)];
        demandMbps = snrDb < 14 ? new double[]{3,4.5}[r.nextInt(2)] : new double[]{6,7.5,9}[r.nextInt(3)];
        rawPhase = new double[]{-30, -15, 15, 30}[r.nextInt(4)];
        rawGain = new double[]{.8, .9, 1.1, 1.2}[r.nextInt(4)];
        rawI = (r.nextInt(7) - 3) * .1; rawQ = (r.nextInt(7) - 3) * .1;
        int[] order = {0,1,2,3,4,5};
        for (int i=5;i>0;i--) { int j=r.nextInt(i+1), tmp=order[i];order[i]=order[j];order[j]=tmp; }
        edges = new int[9][2];
        for(int i=0;i<6;i++) edges[i]=new int[]{order[i],order[(i+1)%6]};
        for(int i=0;i<3;i++) edges[6+i]=new int[]{order[i],order[i+3]};
    }
    public int[][] edges() { return java.util.Arrays.stream(edges).map(int[]::clone).toArray(int[][]::new); }
    @Override public double[] initialDials() { double[] d=new double[16]; d[1]=1; return d; }
    @Override public boolean acceptsChoice(int stage,int slot,int value) {
        return switch(stage) {
            case 0 -> slot==0&&value>=0&&value<3 || slot==1&&value>=0&&value<4;
            case 1 -> (slot==2||slot==3)&&value>=0&&value<2 || slot==4&&value>=0&&value<4;
            case 3 -> slot>=5&&slot<=10&&value>=0&&value<4;
            default -> false;
        };
    }
    @Override public boolean acceptsDial(int stage,int slot,double value) {
        return stage==2 && (slot==0&&value>=-60&&value<=60 || slot==1&&value>=.5&&value<=1.5
                || (slot==2||slot==3)&&value>=-.6&&value<=.6);
    }
    public double received(int[] p) { return p[0]<0||p[1]<0?Double.NaN:TX_DBM[p[0]]-lossDb-ATTENUATION_DB[p[1]]; }
    public double capacityMbps() { return bandwidthMHz * Math.log1p(Math.pow(10,snrDb/10))/Math.log(2); }
    public double netMbps(int[] p) { return selected(p,2,3)?SYMBOL_RATES[p[4]]*(p[2]==0?2:4)*CODE_RATES[p[3]]*.9:0; }
    public double occupiedMHz(int[] p) { return p[4]<0?0:SYMBOL_RATES[p[4]]*1.25; }
    /** Pilots retain identity/colour through rotations, so quadrant ambiguity is observable. */
    public double[][] constellation(double[] d) {
        double angle=Math.toRadians(rawPhase+d[0]), gain=rawGain*d[1]; double[][] points=new double[4][2];
        for(int i=0;i<4;i++) { double x=(i==0||i==3?1:-1)/Math.sqrt(2),y=(i<2?1:-1)/Math.sqrt(2);
            points[i][0]=gain*(x*Math.cos(angle)-y*Math.sin(angle))+rawI+d[2];
            points[i][1]=gain*(x*Math.sin(angle)+y*Math.cos(angle))+rawQ+d[3]; }
        return points;
    }
    public double evm(double[] d) {
        double sum=0;double[][] points=constellation(d);
        for(int i=0;i<4;i++){double x=(i==0||i==3?1:-1)/Math.sqrt(2),y=(i<2?1:-1)/Math.sqrt(2);
            sum+=Math.pow(points[i][0]-x,2)+Math.pow(points[i][1]-y,2);}
        return Math.sqrt(sum/4);
    }
    @Override public ProfessionFeedback check(int stage,int[] p,double[] d,double a,double b,double c) {
        switch(stage) {
            case 0:
                if(!selected(p,0,2))return MISSING;
                if(!near(a,received(p),.05))return CALC_A;
                if(!near(b,received(p)-sensitivity,.05))return CALC_B;
                return received(p)<sensitivity+3-1e-9||received(p)>overload+1e-9?OPTICAL:READY;
            case 1:
                if(!selected(p,2,3))return MISSING;
                if(!near(a,capacityMbps(),.05))return CALC_A;
                if(!near(b,netMbps(p),.03))return CALC_B;
                if(snrDb<(p[2]==0?6:14))return MODULATION;
                if(occupiedMHz(p)>bandwidthMHz+1e-9)return BANDWIDTH;
                return netMbps(p)<demandMbps||netMbps(p)>capacityMbps()*.8?THROUGHPUT:READY;
            case 2: return evm(d)>.035?IQ:READY;
            case 3:
                if(!selected(p,5,6))return MISSING;
                for(int[] e:edges)if(Math.abs(p[5+e[0]]-p[5+e[1]])<2)return INTERFERENCE;
                return READY;
            default: return MISSING;
        }
    }
}
