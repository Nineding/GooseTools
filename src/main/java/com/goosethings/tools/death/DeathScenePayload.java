package com.goosethings.tools.death;

import com.goosethings.tools.intro.RoleIntroPayload.Appearance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

/** Victim-only appearance. No attacker identity, role, position, or private roster. */
public record DeathScenePayload(long event, DeathSceneKind kind, Appearance victim) implements CustomPacketPayload {
    public static final Type<DeathScenePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("goosetools", "death_scene_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeathScenePayload> CODEC = StreamCodec.of(DeathScenePayload::write, DeathScenePayload::read);
    private static void write(RegistryFriendlyByteBuf b, DeathScenePayload p) {
        b.writeLong(p.event()); b.writeUtf(p.kind().id(), 24); b.writeBoolean(p.victim() != null);
        if (p.victim() == null) return;
        b.writeUUID(p.victim().id()); b.writeUtf(p.victim().name(), 64);
        for (var item : p.victim().equipment()) ItemStack.OPTIONAL_STREAM_CODEC.encode(b, item);
    }
    private static DeathScenePayload read(RegistryFriendlyByteBuf b) {
        long event = b.readLong(); var kind = DeathSceneKind.parse(b.readUtf(24));
        if (!b.readBoolean()) return new DeathScenePayload(event, kind, null);
        var id = b.readUUID(); String name = b.readUtf(64); var gear = new ArrayList<ItemStack>();
        for (var slot : EquipmentSlot.values()) gear.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
        return new DeathScenePayload(event, kind, new Appearance(id, name, false, gear));
    }
    @Override public Type<DeathScenePayload> type() { return TYPE; }
}
