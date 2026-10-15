package com.goosethings.tools.appearance.preview;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SkinPreviewTest {
    @TempDir Path directory;
    private byte[] png(int width,int height)throws Exception {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);image.setRGB(2,3,0x8032a0ff);
        var out=new ByteArrayOutputStream();ImageIO.write(image,"PNG",out);return out.toByteArray();
    }
    @Test void storesOriginalColorsAlphaAndModelUnderAuthenticatedUuid()throws Exception {
        var id=UUID.randomUUID();byte[] image=png(64,64);
        SkinPreviewStore.save(directory,new SkinPreviewTransfer.Snapshot(id,true,image),12345);
        var value=JsonParser.parseString(Files.readString(directory.resolve(id+".json"))).getAsJsonObject();
        assertEquals(id.toString(),value.get("uuid").getAsString());assertEquals("slim",value.get("model").getAsString());
        byte[] stored=Base64.getDecoder().decode(value.get("png").getAsString());
        assertEquals(0x8032a0ff,ImageIO.read(new ByteArrayInputStream(stored)).getRGB(2,3));
        assertEquals(SkinPreviewTransfer.sha256(stored),value.get("sha256").getAsString());
        assertEquals(12345,value.get("updatedAt").getAsLong());
    }
    @Test void replacementIsOneCompleteSnapshotAndLeavesNoTemporaryFiles()throws Exception {
        var id=UUID.randomUUID();byte[] image=png(128,128);
        SkinPreviewStore.save(directory,new SkinPreviewTransfer.Snapshot(id,true,image),10);
        SkinPreviewStore.save(directory,new SkinPreviewTransfer.Snapshot(id,false,image),20);
        var value=JsonParser.parseString(Files.readString(directory.resolve(id+".json"))).getAsJsonObject();
        assertEquals("default",value.get("model").getAsString());assertEquals(20,value.get("updatedAt").getAsInt());
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void rejectsInvalidImagesBeforeWriting()throws Exception {
        for(byte[] data:List.of(new byte[100],png(32,32),png(65,64),png(64,63))) {
            assertThrows(IOException.class,()->SkinPreviewStore.save(directory,new SkinPreviewTransfer.Snapshot(UUID.randomUUID(),false,data),1));
        }
        try(var files=Files.list(directory)){assertEquals(0,files.count());}
    }
    @Test void rejectsOversizedOrMisdeclaredPng()throws Exception {
        byte[] data=png(64,64);ByteBuffer.wrap(data,16,4).putInt(16384);
        assertThrows(IOException.class,()->SkinPreviewStore.normalizedPng(data));
        assertThrows(IOException.class,()->SkinPreviewStore.normalizedPng(new byte[SkinPreviewTransfer.MAX_BYTES+1]));
    }
    @Test void acceptsLegacyAndHdDimensions()throws Exception {
        assertNotNull(SkinPreviewStore.normalizedPng(png(64,32)));
        assertNotNull(SkinPreviewStore.normalizedPng(png(1024,1024)));
    }
    @Test void handlesOutOfOrderAndIdenticalDuplicateChunks() {
        var receiver=new SkinPreviewTransfer();var id=UUID.randomUUID();
        byte[] all=new byte[30_000];new Random(13).nextBytes(all);String hash=SkinPreviewTransfer.sha256(all);
        byte[] a=Arrays.copyOfRange(all,0,SkinPreviewTransfer.CHUNK_BYTES),b=Arrays.copyOfRange(all,SkinPreviewTransfer.CHUNK_BYTES,all.length);
        assertTrue(receiver.accept(id,1,all.length,1,2,true,hash,b,100_000).isEmpty());
        assertTrue(receiver.accept(id,1,all.length,1,2,true,hash,b,100_001).isEmpty());
        var snapshot=receiver.accept(id,1,all.length,0,2,true,hash,a,100_002).orElseThrow();
        assertEquals(id,snapshot.player());assertTrue(snapshot.slim());assertArrayEquals(all,snapshot.png());
    }
    @Test void neverCombinesChunksFromDifferentConnections() {
        var receiver=new SkinPreviewTransfer();var one=UUID.randomUUID();var two=UUID.randomUUID();byte[] all=new byte[30_000];
        String hash=SkinPreviewTransfer.sha256(all);byte[] a=new byte[SkinPreviewTransfer.CHUNK_BYTES],b=new byte[all.length-a.length];
        assertTrue(receiver.accept(one,2,all.length,0,2,false,hash,a,100_000).isEmpty());
        assertTrue(receiver.accept(two,2,all.length,1,2,false,hash,b,100_001).isEmpty());
        assertArrayEquals(all,receiver.accept(one,2,all.length,1,2,false,hash,b,100_002).orElseThrow().png());
    }
    @Test void rejectsCorruptedHashAndMixedMetadata() {
        var receiver=new SkinPreviewTransfer();var id=UUID.randomUUID();byte[] all=new byte[30_000];String hash=SkinPreviewTransfer.sha256(all);
        byte[] a=new byte[SkinPreviewTransfer.CHUNK_BYTES],b=new byte[all.length-a.length];
        assertTrue(receiver.accept(id,3,all.length,0,2,false,hash,a,100_000).isEmpty());
        assertTrue(receiver.accept(id,3,all.length,1,2,true,hash,b,100_001).isEmpty());
        assertTrue(receiver.accept(id,4,1,0,1,false,"0".repeat(64),new byte[]{1},120_000).isEmpty());
    }
    @Test void expiresPartialUploadsAndRateLimitsReplacements() {
        var receiver=new SkinPreviewTransfer();var id=UUID.randomUUID();byte[] all=new byte[30_000];String hash=SkinPreviewTransfer.sha256(all);
        byte[] a=new byte[SkinPreviewTransfer.CHUNK_BYTES],b=new byte[all.length-a.length];
        assertTrue(receiver.accept(id,5,all.length,0,2,false,hash,a,100_000).isEmpty());
        assertTrue(receiver.accept(id,6,all.length,0,2,false,hash,a,100_001).isEmpty());
        assertTrue(receiver.accept(id,5,all.length,1,2,false,hash,b,140_000).isEmpty());
        receiver.clear(id);
        assertTrue(receiver.accept(id,7,all.length,0,2,false,hash,a,140_001).isEmpty());
        assertTrue(receiver.accept(id,7,all.length,1,2,false,hash,b,140_002).isPresent());
    }
    @Test void expiredUploadsReleaseGlobalCapacityForOtherPlayers() {
        var receiver=new SkinPreviewTransfer();byte[] first=new byte[SkinPreviewTransfer.CHUNK_BYTES];
        String hash=SkinPreviewTransfer.sha256(new byte[30_000]);
        for(int i=0;i<128;i++)receiver.accept(UUID.randomUUID(),i,30_000,0,2,false,hash,first,100_000);
        byte[] image={1};var id=UUID.randomUUID();String smallHash=SkinPreviewTransfer.sha256(image);
        assertTrue(receiver.accept(id,1,1,0,1,false,smallHash,image,100_001).isEmpty());
        assertTrue(receiver.accept(id,2,1,0,1,false,smallHash,image,140_000).isPresent());
    }
    @Test void rejectsUnboundedChunks() {
        assertThrows(IllegalArgumentException.class,()->SkinPreviewTransfer.validate(SkinPreviewTransfer.MAX_BYTES+1,0,1,"a".repeat(64),new byte[1]));
        assertThrows(IllegalArgumentException.class,()->SkinPreviewTransfer.validate(1,-1,1,"a".repeat(64),new byte[1]));
        assertThrows(IllegalArgumentException.class,()->SkinPreviewTransfer.validate(1,0,99,"a".repeat(64),new byte[1]));
        assertThrows(IllegalArgumentException.class,()->SkinPreviewTransfer.validate(1,0,1,"bad",new byte[1]));
    }
}
