package com.goosethings.tools.camera;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class CameraPackets {
    private CameraPackets() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GooseTools.MOD_ID, id));
    }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Catalog.TYPE, Catalog.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Watch.TYPE, Watch.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Focus.TYPE, Focus.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Blocks.TYPE, Blocks.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Entities.TYPE, Entities.CODEC);
    }
    public record Catalog(long generation, String json) implements CustomPacketPayload {
        public static final Type<Catalog> TYPE = CameraPackets.type("camera_catalog_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Catalog> CODEC = StreamCodec.of(
                (b, p) -> { b.writeLong(p.generation); b.writeUtf(p.json, 262144); },
                b -> new Catalog(b.readLong(), b.readUtf(262144)));
        @Override public Type<Catalog> type() { return TYPE; }
    }
    public record Watch(long generation, List<String> ids) implements CustomPacketPayload {
        public static final Type<Watch> TYPE = CameraPackets.type("camera_watch_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Watch> CODEC = StreamCodec.of(
                (b, p) -> { b.writeLong(p.generation); b.writeVarInt(p.ids.size()); p.ids.forEach(id -> b.writeUtf(id, 48)); },
                b -> { long gen = b.readLong(); int count = bounded(b.readVarInt(), CameraLimits.MAX_ACTIVE);
                    var ids = new ArrayList<String>(); for (int i = 0; i < count; i++) ids.add(b.readUtf(48));
                    return new Watch(gen, List.copyOf(ids)); });
        @Override public Type<Watch> type() { return TYPE; }
    }
    /** Feeds that are actually drawn on screens inside the viewer's current frame. */
    public record Focus(long generation, List<String> ids) implements CustomPacketPayload {
        public static final Type<Focus> TYPE = CameraPackets.type("camera_focus_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf, Focus> CODEC = StreamCodec.of(
                (b, p) -> { b.writeLong(p.generation); b.writeVarInt(p.ids.size()); p.ids.forEach(id -> b.writeUtf(id, 48)); },
                b -> { long gen = b.readLong(); int count = bounded(b.readVarInt(), CameraLimits.MAX_ACTIVE);
                    var ids = new ArrayList<String>(); for (int i = 0; i < count; i++) ids.add(b.readUtf(48));
                    return new Focus(gen, List.copyOf(ids)); });
        @Override public Type<Focus> type() { return TYPE; }
    }
    public record Blocks(long generation, String camera, byte[] data) implements CustomPacketPayload {
        public static final Type<Blocks> TYPE = CameraPackets.type("camera_blocks_v3");
        public static final StreamCodec<RegistryFriendlyByteBuf, Blocks> CODEC = StreamCodec.of(
                (b, p) -> { b.writeLong(p.generation); b.writeUtf(p.camera, 48); b.writeByteArray(p.data); },
                b -> new Blocks(b.readLong(), b.readUtf(48), b.readByteArray(CameraLimits.MAX_COMPRESSED)));
        @Override public Type<Blocks> type() { return TYPE; }
    }
    public record Actor(String type, UUID uuid, String name, double x, double y, double z,
                        float yaw, float pitch, float headYaw, float bodyYaw,
                        ClientboundSetEntityDataPacket metadata, List<ItemStack> equipment, int light,
                        int age, float attack, boolean offHand, int hurt, int death, int useTicks, float swim, int flyingTicks) {
        private static Actor read(RegistryFriendlyByteBuf b) {
            String type = b.readUtf(128); UUID uuid = b.readUUID(); String name = b.readUtf(64);
            double x = b.readDouble(), y = b.readDouble(), z = b.readDouble();
            CameraLimits.position(x, y, z);
            float yaw = b.readFloat(), pitch = b.readFloat(), head = b.readFloat(), body = b.readFloat();
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Float.isFinite(head) || !Float.isFinite(body))
                throw new IllegalArgumentException("Invalid actor rotation");
            var metadata = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(b);
            var equipment = new ArrayList<ItemStack>();
            for (var slot : EquipmentSlot.values()) equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
            int light = b.readInt(), age = b.readVarInt(); float attack = b.readFloat(); boolean offHand = b.readBoolean();
            int hurt = b.readVarInt(), death = b.readVarInt(), use = b.readVarInt(); float swim = b.readFloat(); int flying = b.readVarInt();
            if (!Float.isFinite(attack) || attack < 0 || attack > 1 || !Float.isFinite(swim) || swim < 0 || swim > 1
                    || age < 0 || hurt < 0 || death < 0 || use < 0 || flying < 0) throw new IllegalArgumentException("Invalid camera animation");
            return new Actor(type, uuid, name, x, y, z, yaw, pitch, head, body, metadata, equipment, light,
                    age, attack, offHand, hurt, death, use, swim, flying);
        }
        private void write(RegistryFriendlyByteBuf b) {
            b.writeUtf(type, 128); b.writeUUID(uuid); b.writeUtf(name, 64);
            b.writeDouble(x); b.writeDouble(y); b.writeDouble(z);
            b.writeFloat(yaw); b.writeFloat(pitch); b.writeFloat(headYaw); b.writeFloat(bodyYaw);
            ClientboundSetEntityDataPacket.STREAM_CODEC.encode(b, metadata);
            equipment.forEach(item -> ItemStack.OPTIONAL_STREAM_CODEC.encode(b, item));
            b.writeInt(light);
            b.writeVarInt(age); b.writeFloat(attack); b.writeBoolean(offHand); b.writeVarInt(hurt); b.writeVarInt(death);
            b.writeVarInt(useTicks); b.writeFloat(swim); b.writeVarInt(flyingTicks);
        }
    }
    public record Entities(long generation, String camera, int skyColor, List<Actor> actors) implements CustomPacketPayload {
        public static final Type<Entities> TYPE = CameraPackets.type("camera_entities_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf, Entities> CODEC = StreamCodec.of(
                (b, p) -> { b.writeLong(p.generation); b.writeUtf(p.camera, 48); b.writeInt(p.skyColor); b.writeVarInt(p.actors.size());
                    p.actors.forEach(a -> a.write(b)); },
                b -> { long gen = b.readLong(); String id = b.readUtf(48); int sky = b.readInt();
                    int count = bounded(b.readVarInt(), CameraLimits.MAX_ENTITIES); var actors = new ArrayList<Actor>();
                    for (int i = 0; i < count; i++) actors.add(Actor.read(b));
                    return new Entities(gen, id, sky, List.copyOf(actors)); });
        @Override public Type<Entities> type() { return TYPE; }
    }
    private static int bounded(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Camera packet count exceeds limit");
        return value;
    }
}
