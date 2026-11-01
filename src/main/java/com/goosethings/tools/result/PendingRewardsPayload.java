package com.goosethings.tools.result;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Own-account aggregate, deliberately separate from one match's earned coins. */
public record PendingRewardsPayload(boolean available,long coins) implements CustomPacketPayload {
    public static final Type<PendingRewardsPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("goosetools","pending_rewards_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PendingRewardsPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeBoolean(p.available());b.writeVarLong(p.coins());},
            b->new PendingRewardsPayload(b.readBoolean(),b.readVarLong()));
    public PendingRewardsPayload {
        if(coins<0 || !available&&coins!=0)throw new IllegalArgumentException("Invalid pending reward status");
    }
    @Override public Type<PendingRewardsPayload> type(){return TYPE;}

    /** No target UUID or account data is accepted from the client. */
    public enum Request implements CustomPacketPayload {
        INSTANCE;
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("goosetools","pending_rewards_c2s"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{},b->INSTANCE);
        @Override public Type<Request> type(){return TYPE;}
    }
}
