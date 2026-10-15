package com.goosethings.tools.appearance.preview;

import com.google.gson.JsonObject;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/** Atomically publishes one self-contained skin snapshot, without fetching client-supplied URLs. */
public final class SkinPreviewStore {
    private SkinPreviewStore() {}
    public static byte[] normalizedPng(byte[] input)throws IOException {
        byte[] signature={(byte)137,80,78,71,13,10,26,10};
        if(input.length<33||input.length>SkinPreviewTransfer.MAX_BYTES||!Arrays.equals(Arrays.copyOf(input,8),signature)
                ||ByteBuffer.wrap(input,8,4).getInt()!=13||ByteBuffer.wrap(input,12,4).getInt()!=0x49484452)throw new IOException("Invalid skin PNG");
        int width=ByteBuffer.wrap(input,16,4).getInt(),height=ByteBuffer.wrap(input,20,4).getInt();
        if(width<64||width>1024||(width&(width-1))!=0||(height!=width&&height*2!=width))throw new IOException("Unsupported skin dimensions");
        var image=ImageIO.read(new ByteArrayInputStream(input));
        if(image==null||image.getWidth()!=width||image.getHeight()!=height)throw new IOException("Invalid skin image");
        var output=new ByteArrayOutputStream();
        if(!ImageIO.write(image,"PNG",output))throw new IOException("PNG encoder unavailable");
        byte[] normalized=output.toByteArray();
        if(normalized.length>SkinPreviewTransfer.MAX_BYTES)throw new IOException("Skin is too large");
        return normalized;
    }
    public static void save(Path directory,SkinPreviewTransfer.Snapshot snapshot,long now)throws IOException {
        byte[] png=normalizedPng(snapshot.png());
        var value=new JsonObject();value.addProperty("schema",1);value.addProperty("uuid",snapshot.player().toString());
        value.addProperty("model",snapshot.slim()?"slim":"default");value.addProperty("updatedAt",now);
        value.addProperty("sha256",SkinPreviewTransfer.sha256(png));value.addProperty("png",Base64.getEncoder().encodeToString(png));
        Files.createDirectories(directory);
        Path temporary=Files.createTempFile(directory,".skin-",".tmp");
        try {
            Files.writeString(temporary,value.toString());
            Files.move(temporary,directory.resolve(snapshot.player()+".json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } finally {Files.deleteIfExists(temporary);}
    }
}
