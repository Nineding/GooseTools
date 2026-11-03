package com.goosethings.tools.xaero;

/** Shared transparent pixel art for both map renderers; no entity or texture required. */
public final class MeetingMapMarkerStyle {
    public static final double NEARBY_BLOCKS = 6;
    private static final String[] PIN = {
        "    dddddd    ","  ddwwwwwwdd  "," dwwrrrrrrwwd "," dwr rrrr rwd ",
        "dwwrrrrrrrrwwd","dwrrrddddrrrwd","dwrrrdwwdrrrwd","dwrrrdwwdrrrwd",
        "dwrrrddddrrrwd","dwwrrrrrrrrwwd"," dwwrrrrrrwwd ","  dwwrrrrwwd  ",
        "   dwwrrwwd   ","    dwwwwd    ","     dwwd     ","      dd      ","      dd      "};
    private static final String[] SKULL = {
        "   ddddd   ","  dbbbbbd  "," dbbbbbbbd ","dbbbbbbbbbd","dbrrbbrrbbd","dbrrbbrrbbd",
        " dbbbdbbbd ","  dbbbbbd  ","  dbrbrbd  ","   ddddd   ","r         r"," rrrrrrrrr "};
    private MeetingMapMarkerStyle() {}
    @FunctionalInterface public interface Pixel { void fill(int x,int y,int color); }
    public static void draw(boolean lastPosition,Pixel pixel){
        String[] rows=lastPosition?PIN:SKULL;
        int left=-(rows[0].length()/2), top=lastPosition?-17:-6;
        for(int y=0;y<rows.length;y++) for(int x=0;x<rows[y].length();x++) {
            int color=switch(rows[y].charAt(x)){case 'd'->0xff741b28;case 'w'->0xfffff5ef;
                case 'r'->0xffff454f;case 'b'->0xfff2eeeb;default->0;};
            if(color!=0)pixel.fill(left+x,top+y,color);
        }
    }
    /** Compare X/Z only: map elevation must not defeat priority at the same position. */
    public static boolean hideBody(double x,double z,double lastX,double lastZ){
        double dx=x-lastX,dz=z-lastZ;return dx*dx+dz*dz<=NEARBY_BLOCKS*NEARBY_BLOCKS;
    }
}
