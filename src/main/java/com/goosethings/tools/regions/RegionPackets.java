package com.goosethings.tools.regions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Saved public region data only; editor traffic belongs to the independent mod. */
public final class RegionPackets {
    private RegionPackets(){}
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(Catalog.TYPE,Catalog.CODEC);}
    public record Catalog(String json)implements CustomPacketPayload{
        public static final Type<Catalog> TYPE=new Type<>(Identifier.parse("goosetools:region_catalog_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Catalog> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,RegionCatalog.MAX_JSON),b->new Catalog(b.readUtf(RegionCatalog.MAX_JSON)));
        public Type<Catalog> type(){return TYPE;}
    }
}
