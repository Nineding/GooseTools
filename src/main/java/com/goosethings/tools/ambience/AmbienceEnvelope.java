package com.goosethings.tools.ambience;

/** Interruptible, fixed-duration fades. Repeated identical targets do not restart a fade. */
public final class AmbienceEnvelope {
    private float level,target,step;private int remaining,duration;
    public void target(float value,int ticks){
        if(!Float.isFinite(value)||value<0||value>1||ticks<1)throw new IllegalArgumentException("Invalid ambience envelope");
        if(target==value&&duration==ticks)return;
        target=value;duration=ticks;remaining=ticks;step=(target-level)/ticks;
    }
    public float tick(){if(remaining>0){level+=step;if(--remaining==0)level=target;}return Math.max(0,Math.min(1,level));}
    public float level(){return level;}
    public boolean silent(){return target==0&&remaining==0;}
}
