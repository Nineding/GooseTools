package com.goosethings.tools.task;

/** Public instrumentation and nameplates, never a precomputed answer or solver assignment. */
public record PowerStationSnapshot(int stage, int feedback, int errors, boolean started, long elapsed, long clock, long event,
        double voltage, double power, double initialPf, double capacitorUnit, int capacitors, int capacity,
        double generatorVoltage, double frequency, double phase, int phaseSequence, long stable,
        double phaseLimit, double[] loadP, double[] loadQ, int[] assignments, double[] phaseI) {
    public PowerStationSnapshot {
        if(stage<0||stage>4||feedback<0||feedback>PowerStationSession.STABLE||errors<0||errors>100_000
                ||elapsed<0||elapsed>3_600_000||clock<0||clock>3_600_000||event<0||capacitors<0||capacitors>15
                ||capacity<0||capacity>4||phaseSequence<0||phaseSequence>1||stable<0||stable>2000
                ||loadP==null||loadP.length!=6||loadQ==null||loadQ.length!=6||assignments==null||assignments.length!=6||phaseI==null||phaseI.length!=3)
            throw new IllegalArgumentException("Invalid station state");
        for(double v:new double[]{voltage,power,initialPf,capacitorUnit,generatorVoltage,frequency,phase,phaseLimit})finite(v);
        for(double v:loadP)finite(v);for(double v:loadQ)finite(v);for(double v:phaseI)finite(v);
        for(int v:assignments)if(v< -1||v>2)throw new IllegalArgumentException("Invalid phase assignment");
        loadP=loadP.clone();loadQ=loadQ.clone();assignments=assignments.clone();phaseI=phaseI.clone();
    }
    private static void finite(double v){if(!Double.isFinite(v)||Math.abs(v)>1_000_000)throw new IllegalArgumentException("Invalid station reading");}
    @Override public double[] loadP(){return loadP.clone();}
    @Override public double[] loadQ(){return loadQ.clone();}
    @Override public int[] assignments(){return assignments.clone();}
    @Override public double[] phaseI(){return phaseI.clone();}
}
