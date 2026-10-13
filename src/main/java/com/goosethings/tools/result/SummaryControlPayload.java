package com.goosethings.tools.result;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** A close acknowledgement cannot choose rewards or reopen a completed round. */
public record SummaryControlPayload(UUID round) implements CustomPacketPayload {
    public static final Type<SummaryControlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "summary_close_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SummaryControlPayload> CODEC = StreamCodec.of(
            (b,p)->b.writeUUID(p.round), b->new SummaryControlPayload(b.readUUID()));
    @Override public Type<SummaryControlPayload> type() { return TYPE; }
}
