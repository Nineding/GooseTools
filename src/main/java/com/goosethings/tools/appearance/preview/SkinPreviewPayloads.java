package com.goosethings.tools.appearance.preview;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Identity is the authenticated connection, never a UUID supplied by the client. */
public final class SkinPreviewPayloads {
    private SkinPreviewPayloads() {}
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "skin_preview_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b,p)->{}, b->new Request());
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Chunk(long upload, int total, int index, int count, boolean slim, String hash, byte[] bytes) implements CustomPacketPayload {
        public static final Type<Chunk> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "skin_preview_chunk_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Chunk> CODEC = StreamCodec.of(Chunk::write, Chunk::read);
        public Chunk {
            SkinPreviewTransfer.validate(total,index,count,hash,bytes);
            bytes=bytes.clone();
        }
        @Override public byte[] bytes() { return bytes.clone(); }
        private static void write(RegistryFriendlyByteBuf b,Chunk p) {
            b.writeLong(p.upload); b.writeVarInt(p.total); b.writeVarInt(p.index); b.writeVarInt(p.count);
            b.writeBoolean(p.slim); b.writeUtf(p.hash,64); b.writeByteArray(p.bytes);
        }
        private static Chunk read(RegistryFriendlyByteBuf b) {
            return new Chunk(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readUtf(64),b.readByteArray(SkinPreviewTransfer.CHUNK_BYTES));
        }
        @Override public Type<Chunk> type() { return TYPE; }
    }
}
