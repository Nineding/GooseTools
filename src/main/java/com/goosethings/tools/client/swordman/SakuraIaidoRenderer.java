package com.goosethings.tools.client.swordman;

import com.google.gson.JsonParser;
import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.animation.DancePart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Standard entity submissions only: no framebuffer changes and no server/world entities. */
public final class SakuraIaidoRenderer {
    private static final Identifier GEOMETRY=Identifier.fromNamespaceAndPath("goosetools","sakura/geometry.json");
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("goosetools","textures/item/sakura_iaido.png");
    private record Quad(float[][] points,float[] uv,float[] normal,int color) { }
    private static volatile List<Quad> blade=List.of(), sheath=List.of();
    private static PlayerModel wide,slim;
    private SakuraIaidoRenderer() { }
    public static void reload(ResourceManager manager) {
        try(var reader=manager.openAsReader(GEOMETRY)) {
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            List<List<Quad>> meshes=new ArrayList<>();
            for(String name:new String[]{"blade","sheath"}) {
                List<Quad> list=new ArrayList<>();
                for(var entry:root.getAsJsonArray(name)) {
                    var q=entry.getAsJsonObject();float[][] p=new float[4][3];float[] uv=new float[4],n=new float[3];
                    for(int i=0;i<4;i++) {
                        for(int j=0;j<3;j++) p[i][j]=q.getAsJsonArray("p").get(i).getAsJsonArray().get(j).getAsFloat();
                        uv[i]=q.getAsJsonArray("uv").get(i).getAsFloat();
                    }
                    for(int i=0;i<3;i++) n[i]=q.getAsJsonArray("n").get(i).getAsFloat();
                    list.add(new Quad(p,uv,n,(int)q.get("color").getAsLong()));
                }
                meshes.add(List.copyOf(list));
            }
            blade=meshes.get(0);sheath=meshes.get(1);wide=slim=null;
        } catch(Exception error) {
            blade=sheath=List.of();GooseTools.LOGGER.warn("Cannot load Sakura iaido mesh; keeping ordinary item rendering",error);
        }
    }
    public static boolean ready() { return !blade.isEmpty() && !sheath.isEmpty(); }
    public static boolean firstPerson(float partial,PoseStack poses,SubmitNodeCollector collector,AvatarRenderState state) {
        if(!SakuraIaidoClient.active() || !ready() || state==null) return false;
        var mc=Minecraft.getInstance();
        if(!mc.options.getCameraType().isFirstPerson()) return false;
        var worldFrame=SakuraIaidoClient.frame(partial);
        var viewRotation=new Quaternionf().rotationY((float)Math.toRadians(60));
        var frame=new SakuraIaidoPose.Frame(view(worldFrame.blade(),viewRotation),view(worldFrame.sheath(),viewRotation),
                worldFrame.weight(),worldFrame.lean(),worldFrame.turn(),worldFrame.slash());
        boolean left=mc.player.getMainArm()==HumanoidArm.LEFT;
        poses.pushPose();
        // The camera rig is lower and farther forward than the third-person waist rig.
        poses.translate(0,.12F,-1.12F);
        poses.scale(-.87F,-.87F,.87F);
        poses.scale(1/16F,1/16F,1/16F);
        if(left) poses.scale(-1,1,1);
        submitWeapon(poses,collector,frame,state.lightCoords,true,true);
        var skin=state.skin.body().texturePath();
        boolean thin=state.skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM;
        if(thin && slim==null) slim=new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM),true);
        if(!thin && wide==null) wide=new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
        PlayerModel model=thin?slim:wide;
        // Skin UVs and sleeves come from the owner's actual skin, including left-handed play.
        collector.submitCustomGeometry(poses,RenderTypes.entityCutout(skin),(pose,out)->{
            PoseStack local=new PoseStack();local.last().pose().set(pose.pose());local.last().normal().set(pose.normal());
            local.scale(16,16,16);
            arm(model.rightArm,frame.blade().position(),false,frame,1,true);
            arm(model.leftArm,frame.sheath().point(0,4.5F,0),true,frame,1,true);
            model.rightSleeve.visible=state.showRightSleeve;model.leftSleeve.visible=state.showLeftSleeve;
            model.rightArm.render(local,out,state.lightCoords,OverlayTexture.NO_OVERLAY);
            model.leftArm.render(local,out,state.lightCoords,OverlayTexture.NO_OVERLAY);
        });
        poses.popPose();return true;
    }
    public static void thirdPerson(PoseStack poses,SubmitNodeCollector collector,AvatarRenderState state) {
        var frame=((SakuraVisualState)state).goosetools$iaido();
        if(frame==null || !ready() || !SakuraIaidoClient.owns(state)) return;
        poses.pushPose();poses.scale(1/16F,1/16F,1/16F);
        if(Minecraft.getInstance().player.getMainArm()==HumanoidArm.LEFT) poses.scale(-1,1,1);
        submitWeapon(poses,collector,frame,state.lightCoords,frame.weight()>0,false);
        poses.popPose();
    }
    private static void submitWeapon(PoseStack poses,SubmitNodeCollector collector,SakuraIaidoPose.Frame frame,int light,boolean showBlade,boolean firstPerson) {
        mesh(poses,collector,sheath,frame.sheath(),light);
        if(showBlade) mesh(poses,collector,blade,frame.blade(),light);
        if(frame.slash()>=.14F && frame.slash()<.5F) {
            poses.pushPose();
            if(firstPerson) poses.rotate(new Quaternionf().rotationY((float)Math.toRadians(60)));
            trail(poses,collector,frame.slash());poses.popPose();
        }
    }
    private static void mesh(PoseStack poses,SubmitNodeCollector collector,List<Quad> quads,SakuraIaidoPose.Transform transform,int light) {
        poses.pushPose();var p=transform.position();poses.translate(p.x,p.y,p.z);poses.rotate(transform.rotation());
        poses.scale(SakuraIaidoPose.SCALE,SakuraIaidoPose.SCALE,SakuraIaidoPose.SCALE);
        collector.submitCustomGeometry(poses,RenderTypes.entityCutout(TEXTURE),(pose,out)->{
            for(var q:quads) for(int i=0;i<4;i++) {
                var v=q.points[i];float u=(i==0||i==1)?q.uv[0]:q.uv[2],w=(i==0||i==3)?q.uv[1]:q.uv[3];
                vertex(out,pose,v[0],v[1],v[2],u,w,q.color,light,q.normal);
            }
        });poses.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,int color,int light,float[] n) {
        out.addVertex(pose,x,y,z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,n[0],n[1],n[2]);
    }
    private static void trail(PoseStack poses,SubmitNodeCollector collector,float seconds) {
        int alpha=(int)(195*(1-SakuraIaidoPose.ease((seconds-.27F)/.23F)));
        List<Vector3f[]> strips=new ArrayList<>();
        for(int i=0;i<12;i++) {
            float a=Math.max(.14F,Math.min(.32F,seconds)-.12F+i*.01F),b=a+.01F;
            var one=SakuraIaidoPose.slash(a).blade();var two=SakuraIaidoPose.slash(b).blade();
            strips.add(new Vector3f[]{one.point(0,21.6F,0),one.point(0,24.2F,0),two.point(0,24.2F,0),two.point(0,21.6F,0)});
        }
        collector.submitCustomGeometry(poses,RenderTypes.entityTranslucentEmissive(TEXTURE),(pose,out)->{
            for(var strip:strips) for(var p:strip) vertex(out,pose,p.x,p.y,p.z,.112F,.12F,(alpha<<24)|0xffc3e1,0xf000f0,new float[]{0,1,0});
            for(int i=0;i<7;i++) {
                float t=Math.max(0,seconds-.16F),x=-5-i*1.2F-t*8,y=7-i*.5F+t*t*14,z=-8+(i%3)*1.5F;
                float r=.14F+i*.018F;
                for(float[] v:new float[][]{{-r,0},{0,-r*1.7F},{r,0},{0,r*1.7F}})
                    vertex(out,pose,x+v[0],y+v[1],z,.112F,.12F,(alpha<<24)|0xffb1d5,0xf000f0,new float[]{0,0,-1});
            }
        });
    }
    public static void pose(PlayerModel model,AvatarRenderState state) {
        var frame=((SakuraVisualState)state).goosetools$iaido();
        if(frame==null || frame.weight()<=0 || !ready() || !SakuraIaidoClient.owns(state)) return;
        float w=frame.weight();boolean mirror=Minecraft.getInstance().player.getMainArm()==HumanoidArm.LEFT;
        model.body.xRot=lerp(model.body.xRot,frame.lean(),w);
        model.body.yRot=lerp(model.body.yRot,mirror?-frame.turn():frame.turn(),w);
        Vector3f right=new Vector3f(frame.blade().position()),left=frame.sheath().point(0,4.5F,0);
        if(mirror) {right.x=-right.x;left.x=-left.x;var tmp=right;right=left;left=tmp;}
        arm(model.rightArm,right,false,frame,w,false);arm(model.leftArm,left,true,frame,w,false);
        model.rightLeg.xRot=lerp(model.rightLeg.xRot,-.13F,w);model.leftLeg.xRot=lerp(model.leftLeg.xRot,.17F,w);
    }
    private static SakuraIaidoPose.Transform view(SakuraIaidoPose.Transform pose,Quaternionf rotation) {
        return new SakuraIaidoPose.Transform(rotation.transform(new Vector3f(pose.position())),new Quaternionf(rotation).mul(pose.rotation()));
    }
    private static void arm(ModelPart part,Vector3f hand,boolean left,SakuraIaidoPose.Frame frame,float weight,boolean firstPerson) {
        // Two-bone reach, using the project's existing skin/sleeve articulation.
        Vector3f shoulder=firstPerson?new Vector3f(left?9:-9,18,3):new Vector3f(left?5.5F:-5.5F,2.8F,left?1:-2);
        Vector3f direction=new Vector3f(hand).sub(shoulder);float distance=direction.length();
        if(distance>9.9F) {shoulder.fma(distance-9.9F,direction.normalize());direction.set(hand).sub(shoulder);distance=9.9F;}
        direction.normalize();
        Vector3f bend=new Vector3f(left?1:-1,.45F,.25F);
        bend.fma(-bend.dot(direction),direction).normalize();
        Vector3f elbow=new Vector3f(shoulder).fma(distance*.5F,direction).fma((float)Math.sqrt(Math.max(0,25-distance*distance*.25F)),bend);
        Quaternionf upper=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(elbow).sub(shoulder).normalize());
        Quaternionf lower=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(hand).sub(elbow).normalize());
        Vector3f origin=upper.transform(new Vector3f(left?-1:1,0,0)).add(shoulder);
        Vector3f angles=upper.getEulerAnglesZYX(new Vector3f());
        part.setPos(lerp(part.x,origin.x,weight),lerp(part.y,origin.y,weight),lerp(part.z,origin.z,weight));
        part.setRotation(lerp(part.xRot,angles.x,weight),lerp(part.yRot,angles.y,weight),lerp(part.zRot,angles.z,weight));
        ((DancePart)(Object)part).goosetools$bend(new Quaternionf().slerp(new Quaternionf(upper).invert().mul(lower),weight),5);
    }
    private static float lerp(float a,float b,float t) { return a+(b-a)*t; }
}
