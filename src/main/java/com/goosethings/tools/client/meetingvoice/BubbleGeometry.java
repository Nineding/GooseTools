package com.goosethings.tools.client.meetingvoice;

/** Pure geometry contract for the native-pixel A atlas. One source pixel = 0.8 nameplate UI pixels. */
final class BubbleGeometry {
    static final float PIXEL = 0.8F;
    @FunctionalInterface interface Sink {
        void quad(float x,float y,float w,float h,float u,float v,float uw,float vh,float z);
    }
    static void emit(VoiceBubbleAnimator.Frame frame, Sink out) {
        out.quad(-20,-28,40,28,0,frame.disconnected()?32:0,40,28,0);
        if (frame.disconnected()) return;
        bar(out,0,frame.h0()); bar(out,5,frame.h1()); bar(out,10,frame.h2());
    }
    static int pixels(double height) {
        if (!Double.isFinite(height)) return 3;
        return (int)Math.max(3,Math.min(12,Math.round(height * 12 / 10.8)));
    }
    private static void bar(Sink out,float x,double levelHeight) {
        int h=pixels(levelHeight);
        // Fixed bottom, square pixel corners. Each pillar uses the same accepted texture crop.
        out.quad(x,-9-h,5,h,48,0,5,13,0.01F);
    }
}
