package com.goosethings.tools.outfit;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.UUID;

/** Public wardrobe rule and already visible meeting corpse identities. */
public record OutfitSnapshot(boolean enabled, List<Corpse> corpses) implements CustomPacketPayload {
    public static final Type<OutfitSnapshot> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "outfit_display_v1"));
    public static final int MAX_CORPSES = 128;
    public record Corpse(UUID entity, UUID owner, String accountName) {}
    public OutfitSnapshot { corpses = List.copyOf(corpses); if (corpses.size() > MAX_CORPSES) throw new IllegalArgumentException("Too many corpses"); }
    public static final StreamCodec<RegistryFriendlyByteBuf, OutfitSnapshot> CODEC = new StreamCodec<>() {
        public OutfitSnapshot decode(RegistryFriendlyByteBuf b) {
            boolean enabled = b.readBoolean(); int count = b.readVarInt();
            if (count < 0 || count > MAX_CORPSES) throw new IllegalArgumentException("Invalid corpse count");
            var rows = new java.util.ArrayList<Corpse>();
            for (int i=0;i<count;i++) rows.add(new Corpse(b.readUUID(),b.readUUID(),b.readUtf(16)));
            return new OutfitSnapshot(enabled,rows);
        }
        public void encode(RegistryFriendlyByteBuf b, OutfitSnapshot p) {
            b.writeBoolean(p.enabled); b.writeVarInt(p.corpses.size());
            for (var row:p.corpses) { b.writeUUID(row.entity); b.writeUUID(row.owner); b.writeUtf(row.accountName,16); }
        }
    };
    @Override public Type<OutfitSnapshot> type() { return TYPE; }
}
