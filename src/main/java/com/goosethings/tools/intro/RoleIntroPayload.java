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
public record RoleIntroPayload(boolean active, String role, int theme, int mode, int map, boolean fullMap, boolean loneDuck, MatchCounts counts, List<Appearance> roster)
        implements CustomPacketPayload {
    public static final Type<RoleIntroPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("goosetools", "role_intro_s2c_v7"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RoleIntroPayload> CODEC =
            StreamCodec.of(RoleIntroPayload::write, RoleIntroPayload::read);
    public static final RoleIntroPayload CLOSE = new RoleIntroPayload(false, "", 0, 0, List.of());

    public RoleIntroPayload(boolean active, String role, int theme, int mode, List<Appearance> roster) {
        this(active, role, theme, mode, 0, false, false, roster);
    }

    public RoleIntroPayload(boolean active, String role, int theme, int mode, int map, boolean fullMap, List<Appearance> roster) {
        this(active, role, theme, mode, map, fullMap, false, roster);
    }

    public RoleIntroPayload(boolean active, String role, int theme, int mode, int map, boolean fullMap, boolean loneDuck, List<Appearance> roster) {
        this(active, role, theme, mode, map, fullMap, loneDuck, new MatchCounts(0,0,0), roster);
    }

    public RoleIntroPayload {
        if (role == null || role.length() > 64 || theme < 0 || theme > RoleIntroPolicy.SPECTATOR || mode < 0 || mode > 2 || map < 0 || map > 11) {
            throw new IllegalArgumentException("Invalid role intro header");
        }
        java.util.Objects.requireNonNull(counts);
        roster = List.copyOf(roster);
        if (roster.size() > RoleIntroPolicy.MAX_PLAYERS || active && roster.isEmpty()) {
            throw new IllegalArgumentException("Invalid role intro roster size");
        }
        HashSet<UUID> ids = new HashSet<>();
        for (Appearance player : roster) {
            if (player.anonymous() && (theme != RoleIntroPolicy.DUCK
                    && (theme != RoleIntroPolicy.LOVER || !player.small())))
                throw new IllegalArgumentException("Anonymous appearances are Duck teammates only");
            if (theme != RoleIntroPolicy.SPECTATOR && (!player.observerRole().isEmpty() || player.observerGlowRgb() != 0))
                throw new IllegalArgumentException("Other roles are spectator-only");
        }
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
        buffer.writeVarInt(payload.counts().players());
        buffer.writeVarInt(payload.counts().ducks());
        buffer.writeVarInt(payload.counts().birds());
        buffer.writeVarInt(payload.roster().size());
        for (Appearance player : payload.roster()) {
            buffer.writeUUID(player.id());
            buffer.writeUtf(player.name(), 64);
            buffer.writeBoolean(player.small());
            buffer.writeBoolean(player.anonymous());
            for (ItemStack item : player.equipment()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, item);
            buffer.writeUtf(player.observerRole(),64); buffer.writeInt(player.observerGlowRgb());
        }
    }

    private static RoleIntroPayload read(RegistryFriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        String role = buffer.readUtf(64);
        int theme = buffer.readUnsignedByte(), mode = buffer.readUnsignedByte();
        int map = buffer.readUnsignedByte();
        boolean fullMap = buffer.readBoolean();
        boolean loneDuck = buffer.readBoolean();
        MatchCounts counts = new MatchCounts(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        int count = buffer.readVarInt();
        if (count < 0 || count > RoleIntroPolicy.MAX_PLAYERS) {
            throw new IllegalArgumentException("Role intro roster exceeds limit");
        }
        List<Appearance> roster = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf(64);
            boolean small = buffer.readBoolean();
            boolean anonymous = buffer.readBoolean();
            List<ItemStack> equipment = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            roster.add(new Appearance(id, name, small, equipment, buffer.readUtf(64), buffer.readInt(), anonymous));
        }
        return new RoleIntroPayload(active, role, theme, mode, map, fullMap, loneDuck, counts, roster);
    }

    @Override public Type<RoleIntroPayload> type() { return TYPE; }

    public record Appearance(UUID id, String name, boolean small, List<ItemStack> equipment, String observerRole, int observerGlowRgb, boolean anonymous) {
        public Appearance(UUID id, String name, boolean small, List<ItemStack> equipment, String observerRole, int observerGlowRgb) {
            this(id, name, small, equipment, observerRole, observerGlowRgb, false);
        }
        public static Appearance anonymousTeammate(boolean small) {
            // Never transmit the real UUID: skin/name caches can otherwise identify the teammate.
            return new Appearance(UUID.randomUUID(), "Unknown", small,
                    PresentationAppearance.emptyEquipment(), "", 0, true);
        }
        public Appearance(UUID id, String name, boolean small, List<ItemStack> equipment) {
            this(id,name,small,equipment,"",0);
        }
        public Appearance(UUID id, String name, boolean small) {
            this(id, name, small, PresentationAppearance.emptyEquipment());
        }
        public Appearance {
            if (anonymous && (!"Unknown".equals(name) || observerRole != null && !observerRole.isEmpty()
                    || observerGlowRgb != 0 || equipment == null
                    || equipment.stream().anyMatch(item -> item == null || !item.isEmpty())))
                throw new IllegalArgumentException("Anonymous appearance contains identifying data");
            observerRole = observerRole == null ? "" : observerRole;
            if (!observerRole.isEmpty() && RoleIntroCatalog.findByStoredId(observerRole).isEmpty() && !observerRole.equals("Duck"))
                throw new IllegalArgumentException("Unknown spectator intro role");
            observerGlowRgb &= 0xffffff;
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
