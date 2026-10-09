package com.goosethings.tools.task.profession;

public record ProfessionSnapshot(int stage,int feedback,int errors,boolean started,long elapsed,long event,
                                 long stable,long testElapsed,double temperature,int[] choices,double[] dials) {
    public ProfessionSnapshot {
        if(stage<0||stage>4||feedback<0||feedback>=ProfessionFeedback.values().length||errors<0||errors>100_000
                ||elapsed<0||elapsed>3_600_000||event<0||stable<0||stable>2000||testElapsed<0||testElapsed>6000
                ||!Double.isFinite(temperature)||temperature<0||temperature>1000
                ||choices==null||choices.length!=16||dials==null||dials.length!=16)
            throw new IllegalArgumentException("Invalid professional snapshot");
        for(int v:choices)if(v< -1||v>15)throw new IllegalArgumentException("Invalid choice");
        for(double v:dials)if(!Double.isFinite(v)||Math.abs(v)>1_000_000)throw new IllegalArgumentException("Invalid dial");
        choices=choices.clone();dials=dials.clone();
    }
    @Override public int[] choices(){return choices.clone();}
    @Override public double[] dials(){return dials.clone();}
}
