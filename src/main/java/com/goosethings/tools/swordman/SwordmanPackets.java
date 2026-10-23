package com.goosethings.tools.swordman;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Bounded input and visual-only corpse snapshots. Gameplay never accepts client geometry. */
public final class SwordmanPackets {
    private SwordmanPackets() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, path));
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Input.TYPE, Input.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Corpse.TYPE, Corpse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Motion.TYPE, Motion.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PoseReply.TYPE, PoseReply.CODEC);
    }
    public record Input(boolean held, boolean cancelled, long sequence) implements CustomPacketPayload {
        public static final Type<Input> TYPE = SwordmanPackets.type("swordman_input_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
                (b,p) -> { b.writeBoolean(p.held); b.writeBoolean(p.cancelled); b.writeLong(p.sequence); },
                b -> new Input(b.readBoolean(), b.readBoolean(), b.readLong()));
        @Override public Type<Input> type() { return TYPE; }
    }
    /** Only server-approved positions are sent; clients never submit dash geometry. */
    public record Motion(double x, double y, double z, boolean active) implements CustomPacketPayload {
        public static final Type<Motion> TYPE = SwordmanPackets.type("swordman_motion_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Motion> CODEC = StreamCodec.of(
                (b,p) -> { b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeBoolean(p.active); },
                b -> new Motion(b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean()));
        public Motion {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Non-finite Swordman position");
        }
        @Override public Type<Motion> type() { return TYPE; }
    }
    public record Corpse(UUID corpse, UUID owner, boolean frozen, float[] parts) implements CustomPacketPayload {
        public static final Type<Corpse> TYPE = SwordmanPackets.type("swordman_corpse_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Corpse> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUUID(p.corpse); b.writeUUID(p.owner); b.writeBoolean(p.frozen); write(b,p.parts); },
                b -> new Corpse(b.readUUID(), b.readUUID(), b.readBoolean(), read(b)));
        public Corpse { parts = checked(parts); }
        @Override public Type<Corpse> type() { return TYPE; }
    }
    public record PoseReply(UUID corpse, float[] parts) implements CustomPacketPayload {
        public static final Type<PoseReply> TYPE = SwordmanPackets.type("swordman_pose_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, PoseReply> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUUID(p.corpse); write(b,p.parts); },
                b -> new PoseReply(b.readUUID(), read(b)));
        public PoseReply { parts = checked(parts); }
        @Override public Type<PoseReply> type() { return TYPE; }
    }
    private static void write(RegistryFriendlyByteBuf b,float[] values) {
        b.writeVarInt(values.length); for(float value:values)b.writeFloat(value);
    }
    private static float[] read(RegistryFriendlyByteBuf b) {
        int length=b.readVarInt();
        if(length!=0&&length!=36)throw new IllegalArgumentException("Invalid frozen pose length");
        float[] result=new float[length]; for(int i=0;i<length;i++)result[i]=b.readFloat();
        return checked(result);
    }
    public static float[] checked(float[] values) {
        if(values==null||(values.length!=0&&values.length!=36))throw new IllegalArgumentException("Invalid frozen pose");
        for(float value:values)if(!Float.isFinite(value)||Math.abs(value)>64)throw new IllegalArgumentException("Unbounded frozen pose");
        return values.clone();
    }
}
