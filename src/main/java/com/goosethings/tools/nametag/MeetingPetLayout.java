package com.goosethings.tools.nametag;

/** The existing nametag flags carry a server-authorized meeting headroom bit. */
public final class MeetingPetLayout {
    public static final int HEADROOM=1<<8;
    private MeetingPetLayout() {}
    private static final int DYNAMIC=1<<9;
    public static int flags(double lift){return HEADROOM|DYNAMIC|(Math.clamp((int)Math.ceil(lift*64),0,255)<<16);}
    public static double lift(int flags){return (flags&HEADROOM)==0?0:(flags&DYNAMIC)==0?.12:((flags>>>16)&255)/64d;}
}
