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
        var frame=SakuraIaidoPose.firstPerson(SakuraIaidoClient.frame(partial));
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
            arm(model.rightArm,frame.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0),false,thin,1,true);
            arm(model.leftArm,frame.sheath().point(0,SakuraIaidoPose.SHEATH_GRIP,0),true,thin,1,true);
            model.rightSleeve.visible=state.showRightSleeve;model.leftSleeve.visible=state.showLeftSleeve;
            model.rightArm.render(local,out,state.lightCoords,OverlayTexture.NO_OVERLAY);
            model.leftArm.render(local,out,state.lightCoords,OverlayTexture.NO_OVERLAY);
        });
        poses.popPose();return true;
    }
    public static void thirdPerson(PoseStack poses,SubmitNodeCollector collector,AvatarRenderState state,PlayerModel model) {
        var frame=((SakuraVisualState)state).goosetools$iaido();
        if(frame==null || !ready() || !SakuraIaidoClient.owns(state)) return;
        if(frame.weight()<=0) {
            // Use the same final hand transform as native items (walk, idle bob and Punchy Moves).
            var hand=Minecraft.getInstance().player.getMainArm().getOpposite();
            poses.pushPose();model.translateToHand(state,hand,poses);
            poses.scale(1/16F,1/16F,1/16F);
            poses.translate(hand==HumanoidArm.LEFT?1:-1,8,0);
            var rotation=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(0,.12F,.99F).normalize());
            var offset=rotation.transform(new Vector3f(0,-SakuraIaidoPose.SHEATH_GRIP*SakuraIaidoPose.SCALE,0));
            mesh(poses,collector,sheath,new SakuraIaidoPose.Transform(offset,rotation),state.lightCoords);
            poses.popPose();return;
        }
        poses.pushPose();poses.scale(1/16F,1/16F,1/16F);
        if(Minecraft.getInstance().player.getMainArm()==HumanoidArm.LEFT) poses.scale(-1,1,1);
        submitWeapon(poses,collector,frame,state.lightCoords,frame.weight()>0,false);
        poses.popPose();
    }
    private static void submitWeapon(PoseStack poses,SubmitNodeCollector collector,SakuraIaidoPose.Frame frame,int light,boolean showBlade,boolean firstPerson) {
        mesh(poses,collector,sheath,frame.sheath(),light);
        if(showBlade) mesh(poses,collector,blade,frame.blade(),light);
        if(frame.slash()>=SakuraIaidoPose.CUT_START && frame.slash()<.54F) {
            poses.pushPose();
            if(firstPerson) poses.rotate(SakuraIaidoPose.firstPersonRotation());
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
        int alpha=(int)(195*(1-SakuraIaidoPose.ease((seconds-SakuraIaidoPose.CUT_END)/.22F)));
        List<Vector3f[]> strips=new ArrayList<>();
        float end=Math.min(SakuraIaidoPose.CUT_END,seconds),start=Math.max(SakuraIaidoPose.CUT_START,end-.12F);
        for(int i=0;i<12;i++) {
            float a=start+(end-start)*i/12,b=start+(end-start)*(i+1)/12;
            var one=SakuraIaidoPose.slash(a).blade();var two=SakuraIaidoPose.slash(b).blade();
            strips.add(new Vector3f[]{one.point(0,21.6F,0),one.point(0,24.2F,0),two.point(0,24.2F,0),two.point(0,21.6F,0)});
        }
        collector.submitCustomGeometry(poses,RenderTypes.entityTranslucentEmissive(TEXTURE),(pose,out)->{
            for(var strip:strips) for(var p:strip) vertex(out,pose,p.x,p.y,p.z,.112F,.12F,(alpha<<24)|0xffc3e1,0xf000f0,new float[]{0,1,0});
            for(int i=0;i<7;i++) {
                float emitted=SakuraIaidoPose.CUT_START+i*.025F,t=seconds-emitted;
                if(t<0)continue;
                var tip=SakuraIaidoPose.slash(emitted).blade().point(0,23,0);
                float x=tip.x-t*2,y=tip.y+t*t*14,z=tip.z-t*4;
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
        Vector3f right=frame.blade().point(0,SakuraIaidoPose.BLADE_GRIP,0),left=frame.sheath().point(0,SakuraIaidoPose.SHEATH_GRIP,0);
        if(mirror) {right.x=-right.x;left.x=-left.x;var tmp=right;right=left;left=tmp;}
        boolean thin=state.skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM;
        arm(model.rightArm,right,false,thin,w,false);arm(model.leftArm,left,true,thin,w,false);
        model.rightLeg.xRot=lerp(model.rightLeg.xRot,-.13F,w);model.leftLeg.xRot=lerp(model.leftLeg.xRot,.17F,w);
    }
    private static void arm(ModelPart part,Vector3f hand,boolean left,boolean thin,float weight,boolean firstPerson) {
        // Two-bone reach, using the project's existing skin/sleeve articulation.
        Vector3f shoulder=firstPerson?new Vector3f(left?9:-9,18,3):new Vector3f(left?5.5F:-5.5F,2.8F,left?1:-2);
        Vector3f direction=new Vector3f(hand).sub(shoulder);float distance=direction.length();
        float upperStretch=firstPerson?Math.max(1,(distance-3.5F)/4):1;
        float upperLength=4*upperStretch,lowerLength=4,maxReach=upperLength+lowerLength-.1F;
        // The grip is at the palm centre (Y=8), not at the end of the sleeve (Y=10).
        if(distance>maxReach) {shoulder.fma(distance-maxReach,direction.normalize());direction.set(hand).sub(shoulder);distance=maxReach;}
        direction.normalize();
        Vector3f bend=new Vector3f(left?1:-1,.45F,.25F);
        bend.fma(-bend.dot(direction),direction).normalize();
        float along=(upperLength*upperLength-lowerLength*lowerLength+distance*distance)/(2*distance);
        Vector3f elbow=new Vector3f(shoulder).fma(along,direction).fma((float)Math.sqrt(Math.max(0,upperLength*upperLength-along*along)),bend);
        Quaternionf upper=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(elbow).sub(shoulder).normalize());
        Quaternionf lower=new Quaternionf().rotationTo(new Vector3f(0,1,0),new Vector3f(hand).sub(elbow).normalize());
        float centre=(left?1:-1)*(thin?.5F:1);
        // Account for the lower arm's lateral centre after bending around the model's pivot.
        Vector3f origin=new Vector3f(hand).sub(upper.transform(new Vector3f(0,upperLength,0)))
                .sub(lower.transform(new Vector3f(centre,4,0)));
        Vector3f angles=upper.getEulerAnglesZYX(new Vector3f());
        part.setPos(lerp(part.x,origin.x,weight),lerp(part.y,origin.y,weight),lerp(part.z,origin.z,weight));
        part.setRotation(lerp(part.xRot,angles.x,weight),lerp(part.yRot,angles.y,weight),lerp(part.zRot,angles.z,weight));
        ((DancePart)(Object)part).goosetools$bendWithStableFaces(new Quaternionf().slerp(new Quaternionf(upper).invert().mul(lower),weight),4,upperStretch);
    }
    private static float lerp(float a,float b,float t) { return a+(b-a)*t; }
}
