package com.goosethings.tools.client.hud;

import com.goosethings.tools.client.animation.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.util.ArrayList;

/** Optional local skin fixture and real mixed-in vertex checks; never shipped in the mod. */
final class DanceSkinRegression {
    static PlayerSkin skin(Minecraft mc,PlayerSkin fallback) throws Exception {
        var path=mc.gameDirectory.toPath().resolve("dance-skin.png");
        if(!Files.isRegularFile(path))return fallback;
        var id=Identifier.fromNamespaceAndPath("goosetools","dance_skin_regression");
        try(var input=Files.newInputStream(path)) {
            mc.getTextureManager().register(id,new DynamicTexture(()->"Dance skin regression",NativeImage.read(input)));
        }
        return new PlayerSkin(new ClientAsset.DownloadedTexture(id,"local-regression"),fallback.body(),null,
                net.minecraft.world.entity.player.PlayerModelType.SLIM,false);
    }
    static void verify(Minecraft mc,MeetingAlertRemotePlayer player,PostgameSummaryScreen screen) throws Exception {
        var renderer=(AvatarRenderer<?>)mc.getEntityRenderDispatcher().getRenderer(player);
        PlayerModel model=renderer.getModel();
        var injector=Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector");
        var limbs=new ModelPart[]{model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};
        var overlays=new ModelPart[]{model.rightSleeve,model.leftSleeve,model.rightPants,model.leftPants};
        int vertices=0;
        for(double seconds:new double[]{1.8,12.5,19.5}) {
            model.setupAnim((AvatarRenderState)screen.previewState(seconds));
            var joints=CuteDanceAnimation.at(seconds).joints();
            for(int i=0;i<4;i++) {
                var part=limbs[i];var outer=overlays[i];
                Object mesh=injector.getMethod("getInjectedMesh").invoke(outer);
                require(mesh!=null,"missing 3D mesh for limb "+i);
                var rotation=new Quaternionf(joints[2+i*2]).invert().mul(joints[3+i*2]);
                float joint=i<2?5:6;
                // Check the base and the actual voxel overlay independently: hiding one must not hide the other.
                for(boolean voxel:new boolean[]{false,true}) {
                    boolean oldSkip=part.skipDraw,oldVisible=outer.visible;
                    try {
                        part.skipDraw=voxel;outer.visible=voxel;
                        ((DancePart)(Object)part).goosetools$bend(null,0);
                        var raw=render(part);
                        require(raw.points.size()>(voxel?24:0),"3D layer flattened or geometry absent: "+i);
                        ((DancePart)(Object)part).goosetools$bend(rotation,joint);
                        var bent=render(part);vertices+=bent.points.size();
                        var poses=new PoseStack();part.translateAndRotate(poses);
                        var world=new Matrix4f(poses.last().pose());var local=new Matrix4f(world).invert();
                        for(var point:raw.points) {
                            var expected=local.transformPosition(new Vector3f(point)).mul(16);
                            world.transformPosition(DanceMesh.bend(expected,rotation,joint).div(16));
                            require(bent.points.stream().anyMatch(p->p.distanceSquared(expected)<1e-8F),
                                    "unbent or double-bent "+(voxel?"3D overlay":"base skin")+" on limb "+i+" at "+seconds);
                        }
                        for(var normal:bent.normals)require(normal.isFinite()&&Math.abs(normal.length()-1)<.001,"invalid bent normal");
                        require(mesh==injector.getMethod("getInjectedMesh").invoke(outer),"cached voxel mesh replaced");
                    } finally {part.skipDraw=oldSkip;outer.visible=oldVisible;}
                }
            }
        }
        var normal=(AvatarRenderState)mc.getEntityRenderDispatcher().getRenderer(player).createRenderState(player,1);
        model.setupAnim(normal);
        for(var part:limbs) {
            var after=render(part);((DancePart)(Object)part).goosetools$bend(null,0);var cleared=render(part);
            require(after.points.equals(cleared.points),"dance bend leaked into ordinary model setup");
        }
        model.setupAnim((AvatarRenderState)screen.previewState(1.8));
        com.goosethings.tools.GooseTools.LOGGER.info("PASS actual SkinLayers3D vertices: {} across four limbs, base/outer layers, three poses; normal model reset",vertices);
    }
    private static Capture render(ModelPart part) {
        var out=new Capture();part.render(new PoseStack(),out,0xF000F0,0,-1);return out;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static final class Capture implements VertexConsumer {
        final ArrayList<Vector3f> points=new ArrayList<>(),normals=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new Vector3f(x,y,z));return this;}
        public VertexConsumer setColor(int c){return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float x,float y){return this;}
        public VertexConsumer setUv1(int x,int y){return this;}
        public VertexConsumer setUv2(int x,int y){return this;}
        public VertexConsumer setUv3(float x,float y){return this;}
        public VertexConsumer setNormal(float x,float y,float z){normals.add(new Vector3f(x,y,z));return this;}
        public VertexConsumer setLineWidth(float w){return this;}
    }
}
