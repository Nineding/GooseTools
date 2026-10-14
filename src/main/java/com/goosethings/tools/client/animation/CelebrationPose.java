package com.goosethings.tools.client.animation;

/** Angles in radians, independent of framerate. */
public record CelebrationPose(float rightArmX, float rightArmZ, float leftArmX, float leftArmZ,
        float rightLegX, float leftLegX, float bodyZ, float headZ, float lift) {
    public static CelebrationPose wave(double seconds) { return at(0,seconds); }
    public static CelebrationPose at(int action,double seconds) {
        double t=Math.max(0,seconds), fade=Math.min(1,t/.45); fade=fade*fade*(3-2*fade);
        float f=(float)fade;
        if(action==1) {
            // Shoulder pivots are separate root children: keep the torso square and move
            // both arms OUT from the chest. A quiet two-step leaves room for thick sleeves.
            float beat=(float)Math.sin(t*Math.PI), sway=(float)Math.sin(t*Math.PI*.5);
            return new CelebrationPose((-.28F+.24F*beat)*f,(.48F+.12F*sway)*f,
                    (-.28F-.24F*beat)*f,(-.48F+.12F*sway)*f,.20F*beat*f,-.20F*beat*f,
                    0,0,(float)(Math.abs(Math.sin(t*Math.PI))*.022)*f);
        }
        // Minecraft's right shoulder is at negative X: positive Z opens the arm outward.
        // Small shoulder sweeps read as a wave without driving the hand through the head.
        return new CelebrationPose(-.16F*f,(2.30F+.15F*(float)Math.sin(t*4.5))*f,
                -.03F*f,-.10F*f,0,0,0,-.025F*f,0);
    }
}
