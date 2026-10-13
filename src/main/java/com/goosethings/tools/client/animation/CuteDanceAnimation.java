package com.goosethings.tools.client.animation;

import com.google.gson.JsonParser;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Authored, locally retargeted reference motion. No video or inference runs in the client. */
public final class CuteDanceAnimation {
    public static final double DURATION=20;
    private static final float[][] FRAMES=load();
    public record Pose(Quaternionf[] joints,float x,float y) {}
    private CuteDanceAnimation() {}
    private static float[][] load() {
        try(var stream=Objects.requireNonNull(CuteDanceAnimation.class.getResourceAsStream(
                "/assets/goosetools/animations/cute_dance.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            if(json.get("duration").getAsDouble()!=DURATION)throw new IllegalStateException("Dance duration mismatch");
            var rows=json.getAsJsonArray("frames");
            if(rows.size()<2)throw new IllegalStateException("Empty dance");
            float[][] result=new float[rows.size()][42];
            for(int i=0;i<result.length;i++) {
                var row=rows.get(i).getAsJsonArray();
                if(row.size()!=42)throw new IllegalStateException("Invalid dance frame");
                for(int j=0;j<42;j++) {
                    float value=row.get(j).getAsFloat();
                    if(!Float.isFinite(value))throw new IllegalStateException("Non-finite dance frame");
                    result[i][j]=value;
                }
            }
            return result;
        } catch(Exception error) {throw new IllegalStateException("Cannot load celebration dance",error);}
    }
    public static Pose at(double seconds) {
        double time=Double.isFinite(seconds)?Math.max(0,seconds):0;
        double cursor=(time%DURATION)/DURATION*(FRAMES.length-1);
        int index=(int)cursor;float fraction=(float)(cursor-index);
        float[] a=FRAMES[index],b=FRAMES[Math.min(index+1,FRAMES.length-1)];
        float fade=(float)Math.min(1,time/.45);fade=fade*fade*(3-2*fade);
        var rotations=new Quaternionf[10];
        for(int j=0;j<10;j++) {
            int k=j*4;
            var target=new Quaternionf(a[k],a[k+1],a[k+2],a[k+3]).normalize().slerp(
                    new Quaternionf(b[k],b[k+1],b[k+2],b[k+3]).normalize(),fraction);
            rotations[j]=new Quaternionf().slerp(target,fade);
        }
        // Recompute after SLERP, including the initial fade and loop transition: interpolating
        // stored root heights independently would lift the supporting foot during a blend.
        float ground=Math.max(footHeight(rotations,-1.9F,6),footHeight(rotations,1.9F,8));
        return new Pose(rotations,(a[40]+(b[40]-a[40])*fraction)*fade,24-ground);
    }
    private static float footHeight(Quaternionf[] joints,float hipX,int thigh) {
        var upper=new Vector3f(0,6,0).rotate(joints[thigh]);
        var lower=new Vector3f(0,6,0).rotate(joints[thigh+1]);
        float edge=2*(Math.abs(new Vector3f(1,0,0).rotate(joints[thigh+1]).y)
                +Math.abs(new Vector3f(0,0,1).rotate(joints[thigh+1]).y));
        return 12+new Vector3f(hipX,0,0).rotate(joints[0]).y+upper.y+lower.y+edge;
    }
}
