package com.goosethings.tools.nametag;

/** The existing nametag flags carry a server-authorized meeting headroom bit. */
public final class MeetingPetLayout {
    public static final int HEADROOM=1<<8;
    private MeetingPetLayout() {}
    public static double lift(int flags){return (flags&HEADROOM)!=0?1.25:0;}
}
