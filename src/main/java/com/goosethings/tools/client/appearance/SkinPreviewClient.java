package com.goosethings.tools.client.appearance;

import com.goosethings.tools.appearance.preview.SkinPreviewPayloads;
import com.goosethings.tools.appearance.preview.SkinPreviewTransfer;
import com.goosethings.tools.client.csl.SkinOverrideBridge;
import com.goosethings.tools.GooseTools;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/** Uses the local player's final, loaded texture, including CSL and local skin providers. */
public final class SkinPreviewClient {
    private static boolean requested,pending;
    private static long nextCheck,generation;
    private static String lastHash="";
    private SkinPreviewClient() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SkinPreviewPayloads.Request.TYPE,(packet,context)->context.client().execute(()->{
            requested=true;nextCheck=System.currentTimeMillis()+5_000;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{generation++;requested=false;pending=false;lastHash="";});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            long now=System.currentTimeMillis();
            if(!requested||pending||client.player==null||client.getConnection()==null||now<nextCheck)return;
            nextCheck=now+15_000;
            var info=client.getConnection().getPlayerInfo(client.player.getUUID());
            if(info==null||SkinOverrideBridge.hasOverrideMarker(info.getProfile()))return;
            var skin=info.getSkin();
            NativeImage image=null;boolean close=false;
            try {
                var texture=client.getTextureManager().getTexture(skin.body().texturePath());
                if(texture instanceof DynamicTexture dynamic)image=dynamic.getPixels();
                else {
                    var resource=client.getResourceManager().getResource(skin.body().texturePath());
                    if(resource.isPresent())try(var input=resource.get().open()){image=NativeImage.read(input);close=true;}
                }
                if(image==null||image.isClosed())return;
                int width=image.getWidth(),height=image.getHeight();
                if(width<64||width>1024||(width&(width-1))!=0||(height!=width&&height*2!=width))return;
                int[] pixels=image.getPixels();
                boolean slim=skin.model()==PlayerModelType.SLIM;
                // Hash the copied ARGB pixels as well as the model; reloads may reuse a texture identifier.
                var buffer=java.nio.ByteBuffer.allocate(pixels.length*4+1);buffer.put((byte)(slim?1:0));for(int pixel:pixels)buffer.putInt(pixel);
                String pixelHash=SkinPreviewTransfer.sha256(buffer.array());
                if(pixelHash.equals(lastHash))return;
                final String fingerprint=pixelHash;final long connection=generation;
                pending=true;
                CompletableFuture.supplyAsync(()->encode(pixels,width,height),Util.nonCriticalIoPool()).whenComplete((png,error)->client.execute(()->{
                    if(connection!=generation)return;
                    pending=false;
                    if(error!=null||png==null||!requested||client.getConnection()==null||!ClientPlayNetworking.canSend(SkinPreviewPayloads.Chunk.TYPE))return;
                    long upload=ThreadLocalRandom.current().nextLong();String hash=SkinPreviewTransfer.sha256(png);
                    int count=(png.length+SkinPreviewTransfer.CHUNK_BYTES-1)/SkinPreviewTransfer.CHUNK_BYTES;
                    for(int index=0;index<count;index++) {
                        byte[] part=java.util.Arrays.copyOfRange(png,index*SkinPreviewTransfer.CHUNK_BYTES,Math.min(png.length,(index+1)*SkinPreviewTransfer.CHUNK_BYTES));
                        ClientPlayNetworking.send(new SkinPreviewPayloads.Chunk(upload,png.length,index,count,slim,hash,part));
                    }
                    lastHash=fingerprint;
                }));
            } catch(Exception error){GooseTools.LOGGER.debug("Skin preview is waiting for a loaded player texture: {}",error.getMessage());}
            finally{if(close&&image!=null)image.close();}
        });
    }
    private static byte[] encode(int[] pixels,int width,int height) {
        try {
            var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,width,height,pixels,0,width);
            var output=new ByteArrayOutputStream();ImageIO.write(image,"PNG",output);byte[] bytes=output.toByteArray();
            return bytes.length>0&&bytes.length<=SkinPreviewTransfer.MAX_BYTES?bytes:null;
        }catch(Exception error){return null;}
    }
}
