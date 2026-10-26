package com.goosethings.tools.spectator;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Viewer-private colours. No positions or identity strings; empty snapshot revokes access. */
public record SpectatorGlowPayload(Map<UUID, Integer> players, Set<UUID> living, Set<UUID> corpses)
        implements CustomPacketPayload {
    public static final int LIMIT = 512;
    public static final SpectatorGlowPayload EMPTY = new SpectatorGlowPayload(Map.of(), Set.of(), Set.of());
    public static final Type<SpectatorGlowPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "spectator_glow_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpectatorGlowPayload> CODEC = StreamCodec.of(SpectatorGlowPayload::write, SpectatorGlowPayload::read);
    public SpectatorGlowPayload {
        players = Map.copyOf(players); living = Set.copyOf(living); corpses = Set.copyOf(corpses);
        if (players.size() > LIMIT || living.size() > LIMIT || corpses.size() > LIMIT)
            throw new IllegalArgumentException("Too many spectator outlines");
    }
    private static void write(RegistryFriendlyByteBuf b, SpectatorGlowPayload p) {
        b.writeVarInt(p.players.size()); p.players.forEach((id, rgb) -> { b.writeUUID(id); b.writeInt(rgb & 0xffffff); });
        b.writeVarInt(p.living.size()); p.living.forEach(b::writeUUID);
        b.writeVarInt(p.corpses.size()); p.corpses.forEach(b::writeUUID);
    }
    private static int count(RegistryFriendlyByteBuf b) {
        int n = b.readVarInt(); if(n < 0 || n > LIMIT) throw new IllegalArgumentException("Invalid outline count"); return n;
    }
    private static SpectatorGlowPayload read(RegistryFriendlyByteBuf b) {
        Map<UUID,Integer> colors = new HashMap<>();
        for(int n=count(b); n>0; n--) colors.put(b.readUUID(), b.readInt() & 0xffffff);
        Set<UUID> living = new HashSet<>(), corpses = new HashSet<>();
        for(int n=count(b); n>0; n--) living.add(b.readUUID());
        for(int n=count(b); n>0; n--) corpses.add(b.readUUID());
        return new SpectatorGlowPayload(colors, living, corpses);
    }
    @Override public Type<SpectatorGlowPayload> type() { return TYPE; }
}
