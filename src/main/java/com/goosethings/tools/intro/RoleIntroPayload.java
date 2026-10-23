package com.goosethings.tools.intro;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Only the viewer's role and the filtered public appearance roster are transmitted. */
public record RoleIntroPayload(boolean active, String role, int theme, int mode, int map, boolean fullMap, boolean loneDuck, List<Appearance> roster)
        implements CustomPacketPayload {
    public static final Type<RoleIntroPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("goosetools", "role_intro_s2c_v4"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RoleIntroPayload> CODEC =
            StreamCodec.of(RoleIntroPayload::write, RoleIntroPayload::read);
    public static final RoleIntroPayload CLOSE = new RoleIntroPayload(false, "", 0, 0, List.of());

    public RoleIntroPayload(boolean active, String role, int theme, int mode, List<Appearance> roster) {
        this(active, role, theme, mode, 0, false, false, roster);
    }

    public RoleIntroPayload(boolean active, String role, int theme, int mode, int map, boolean fullMap, List<Appearance> roster) {
        this(active, role, theme, mode, map, fullMap, false, roster);
    }

    public RoleIntroPayload {
        if (role == null || role.length() > 64 || theme < 0 || theme > 3 || mode < 0 || mode > 2 || map < 0 || map > 11) {
            throw new IllegalArgumentException("Invalid role intro header");
        }
        roster = List.copyOf(roster);
        if (roster.size() > RoleIntroPolicy.MAX_PLAYERS || active && roster.isEmpty()) {
            throw new IllegalArgumentException("Invalid role intro roster size");
        }
        HashSet<UUID> ids = new HashSet<>();
        for (Appearance player : roster) if (!ids.add(player.id())) {
            throw new IllegalArgumentException("Duplicate role intro player");
        }
        if (!active && (!roster.isEmpty() || !role.isEmpty())) {
            throw new IllegalArgumentException("Inactive intro carries no identities");
        }
    }

    private static void write(RegistryFriendlyByteBuf buffer, RoleIntroPayload payload) {
        buffer.writeBoolean(payload.active());
        buffer.writeUtf(payload.role(), 64);
        buffer.writeByte(payload.theme());
        buffer.writeByte(payload.mode());
        buffer.writeByte(payload.map());
        buffer.writeBoolean(payload.fullMap());
        buffer.writeBoolean(payload.loneDuck());
        buffer.writeVarInt(payload.roster().size());
        for (Appearance player : payload.roster()) {
            buffer.writeUUID(player.id());
            buffer.writeUtf(player.name(), 64);
            buffer.writeBoolean(player.small());
            for (ItemStack item : player.equipment()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item);
        }
    }

    private static RoleIntroPayload read(RegistryFriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        String role = buffer.readUtf(64);
        int theme = buffer.readUnsignedByte(), mode = buffer.readUnsignedByte();
        int map = buffer.readUnsignedByte();
        boolean fullMap = buffer.readBoolean();
        boolean loneDuck = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > RoleIntroPolicy.MAX_PLAYERS) {
            throw new IllegalArgumentException("Role intro roster exceeds limit");
        }
        List<Appearance> roster = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf(64);
            boolean small = buffer.readBoolean();
            List<ItemStack> equipment = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            roster.add(new Appearance(id, name, small, equipment));
        }
        return new RoleIntroPayload(active, role, theme, mode, map, fullMap, loneDuck, roster);
    }

    @Override public Type<RoleIntroPayload> type() { return TYPE; }

    public record Appearance(UUID id, String name, boolean small, List<ItemStack> equipment) {
        public Appearance(UUID id, String name, boolean small) {
            this(id, name, small, PresentationAppearance.emptyEquipment());
        }
        public Appearance {
            if (id == null || name == null || name.isBlank() || name.length() > 64) {
                throw new IllegalArgumentException("Invalid intro appearance");
            }
            if (equipment == null || equipment.size() != EquipmentSlot.values().length
                    || equipment.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Invalid intro equipment");
            }
            equipment = equipment.stream().map(ItemStack::copy).toList();
        }
    }
}
