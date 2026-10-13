package com.goosethings.tools.intro;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Separate channel keeps the instant opening handoff wire format unchanged. */
public record PresentationFadePayload(boolean active) implements CustomPacketPayload {
    public static final Type<PresentationFadePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("goosetools", "presentation_fade_s2c_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PresentationFadePayload> CODEC =
            StreamCodec.of((buf, value) -> buf.writeBoolean(value.active()),
                    buf -> new PresentationFadePayload(buf.readBoolean()));
    @Override public Type<PresentationFadePayload> type() { return TYPE; }
}
