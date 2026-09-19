package com.goosethings.tools.camera;

/** Animation history follows server ticks, independent of render rate and packet arrival jitter. */
public final class CameraMotion {
    private int tick = -1;
    private double x, z;
    private float speed, position;
    public void update(int nextTick, double nextX, double nextZ) {
        if (nextTick == tick) return;
        int elapsed = nextTick-tick;
        double distance = Math.hypot(nextX-x,nextZ-z);
        if (tick < 0 || elapsed <= 0 || elapsed > 20 || distance > 8) { speed=0; position=0; }
        else {
            float target = (float)Math.min(1,distance/elapsed*4);
            for (int i=0;i<elapsed;i++) { speed += (target-speed)*.4f; position += speed; }
        }
        tick=nextTick; x=nextX; z=nextZ;
    }
    public float speed() { return speed; }
    public float position() { return position; }
}
