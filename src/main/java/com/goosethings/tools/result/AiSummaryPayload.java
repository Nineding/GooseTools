package com.goosethings.tools.result;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A read-only previous-match summary, cached for the matching AI report rather than forced open. */
public record AiSummaryPayload(long gameId,SummaryPayload summary) implements CustomPacketPayload {
    public static final Type<AiSummaryPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("goosetools","ai_summary_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AiSummaryPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarLong(p.gameId());SummaryPayload.CODEC.encode(b,p.summary());},
            b->new AiSummaryPayload(b.readVarLong(),SummaryPayload.CODEC.decode(b)));
    @Override public Type<AiSummaryPayload> type(){return TYPE;}
}
