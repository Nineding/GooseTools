package com.goosethings.tools.appearance.preview;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.*;

/** Bounded, per-connection assembly; out-of-order chunks cannot cross player identities. */
public final class SkinPreviewTransfer {
    public static final int MAX_BYTES=512*1024, CHUNK_BYTES=24*1024;
    private final Map<UUID,Upload> uploads=new HashMap<>();
    private final Map<UUID,Long> started=new HashMap<>();
    public record Snapshot(UUID player,boolean slim,byte[] png) {}
    private static final class Upload {
        final long id,expires; final int total,count; final String hash; final boolean slim;
        final byte[][] pieces; int size,received;
        Upload(long id,long now,int total,int count,String hash,boolean slim) {
            this.id=id;expires=now+30_000;this.total=total;this.count=count;this.hash=hash;this.slim=slim;pieces=new byte[count][];
        }
    }
    public static void validate(int total,int index,int count,String hash,byte[] bytes) {
        if(total<1||total>MAX_BYTES||count!=(total+CHUNK_BYTES-1)/CHUNK_BYTES||index<0||index>=count
                ||hash==null||!hash.matches("[a-f0-9]{64}")||bytes==null
                ||bytes.length!=(index==count-1?total-index*CHUNK_BYTES:CHUNK_BYTES)) throw new IllegalArgumentException("Invalid skin preview chunk");
    }
    public Optional<Snapshot> accept(UUID player,long id,int total,int index,int count,boolean slim,String hash,byte[] bytes,long now) {
        validate(total,index,count,hash,bytes);
        uploads.values().removeIf(upload -> upload.expires<now);
        Upload upload=uploads.get(player);
        if(upload==null||upload.id!=id) {
            if(now-started.getOrDefault(player,Long.MIN_VALUE/2)<10_000)return Optional.empty();
            if(uploads.size()>=128&&!uploads.containsKey(player))return Optional.empty();
            started.put(player,now);upload=new Upload(id,now,total,count,hash,slim);uploads.put(player,upload);
        }
        if(upload.total!=total||upload.count!=count||upload.slim!=slim||!upload.hash.equals(hash)) {uploads.remove(player);return Optional.empty();}
        if(upload.pieces[index]!=null) {
            if(!Arrays.equals(upload.pieces[index],bytes))uploads.remove(player);
            return Optional.empty();
        }
        upload.pieces[index]=bytes.clone();upload.size+=bytes.length;upload.received++;
        if(upload.received!=count)return Optional.empty();
        uploads.remove(player);
        if(upload.size!=total)return Optional.empty();
        var output=new ByteArrayOutputStream(total);
        for(byte[] piece:upload.pieces)output.writeBytes(piece);
        byte[] png=output.toByteArray();
        if(!sha256(png).equals(hash))return Optional.empty();
        return Optional.of(new Snapshot(player,slim,png));
    }
    public void clear(UUID player) {uploads.remove(player);started.remove(player);}
    public void clear() {uploads.clear();started.clear();}
    public static String sha256(byte[] data) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
        catch(Exception error){throw new IllegalStateException(error);}
    }
}
