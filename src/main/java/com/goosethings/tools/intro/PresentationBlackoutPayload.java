package com.goosethings.tools.intro;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** An opaque handoff, released only after the server completes its transition. */
public record PresentationBlackoutPayload(boolean active) implements CustomPacketPayload {
    public static final Type<PresentationBlackoutPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("goosetools", "presentation_blackout_s2c_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PresentationBlackoutPayload> CODEC =
            StreamCodec.of((buf, value) -> buf.writeBoolean(value.active()),
                    buf -> new PresentationBlackoutPayload(buf.readBoolean()));
    @Override public Type<PresentationBlackoutPayload> type() { return TYPE; }
}
