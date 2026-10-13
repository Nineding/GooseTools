package com.goosethings.tools.result;

import com.goosethings.tools.intro.RoleIntroPolicy;
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

/** A frozen public result, sent only once the authoritative round has ended. */
public record MatchResultPayload(boolean active, String outcome, List<Player> roster) implements CustomPacketPayload {
    public static final Type<MatchResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("goosetools", "match_result_s2c_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MatchResultPayload> CODEC =
            StreamCodec.of(MatchResultPayload::write, MatchResultPayload::read);
    public static final MatchResultPayload CLOSE = new MatchResultPayload(false, "", List.of());
    public MatchResultPayload {
        if (outcome == null || active && !MatchResultPolicy.validOutcome(outcome)
                || !active && !outcome.isEmpty()) throw new IllegalArgumentException("Invalid result outcome");
        roster = List.copyOf(roster);
        if (roster.size() > RoleIntroPolicy.MAX_PLAYERS || active && roster.isEmpty() || !active && !roster.isEmpty())
            throw new IllegalArgumentException("Invalid result roster");
        var ids = new HashSet<UUID>();
        for (Player player : roster) if (!ids.add(player.id())) throw new IllegalArgumentException("Duplicate result player");
    }
    private static void write(RegistryFriendlyByteBuf buf, MatchResultPayload value) {
        buf.writeBoolean(value.active()); buf.writeUtf(value.outcome(), 32); buf.writeVarInt(value.roster().size());
        for (Player player : value.roster()) {
            buf.writeUUID(player.id()); buf.writeUtf(player.name(), 64); buf.writeUtf(player.role(), 64);
            buf.writeInt(player.rgb()); buf.writeVarInt(player.serial());
            buf.writeBoolean(player.lover()); buf.writeBoolean(player.winner()); buf.writeBoolean(player.dead());
            for (ItemStack item : player.equipment()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item);
        }
    }
    private static MatchResultPayload read(RegistryFriendlyByteBuf buf) {
        boolean active = buf.readBoolean(); String outcome = buf.readUtf(32); int count = buf.readVarInt();
        if (count < 0 || count > RoleIntroPolicy.MAX_PLAYERS) throw new IllegalArgumentException("Result roster exceeds limit");
        List<Player> roster = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID(); String name = buf.readUtf(64), role = buf.readUtf(64);
            int rgb = buf.readInt(), serial = buf.readVarInt();
            boolean lover = buf.readBoolean(), winner = buf.readBoolean(), dead = buf.readBoolean();
            List<ItemStack> equipment = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            roster.add(new Player(id, name, role, rgb, serial, lover, winner, dead, equipment));
        }
        return new MatchResultPayload(active, outcome, roster);
    }
    @Override public Type<MatchResultPayload> type() { return TYPE; }
    public record Player(UUID id, String name, String role, int rgb, int serial,
                         boolean lover, boolean winner, boolean dead, List<ItemStack> equipment) {
        public Player {
            if (id == null || name == null || name.isBlank() || name.length() > 64
                    || role == null || role.length() > 64 || rgb < 0 || rgb > 0xffffff || serial < 0 || serial > 99)
                throw new IllegalArgumentException("Invalid result player");
            if (equipment == null || equipment.size() != EquipmentSlot.values().length
                    || equipment.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("Invalid result equipment");
            equipment = equipment.stream().map(ItemStack::copy).toList();
        }
    }
}
