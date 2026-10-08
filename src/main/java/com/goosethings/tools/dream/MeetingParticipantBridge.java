package com.goosethings.tools.dream;

import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Method;
import java.util.UUID;

/** Reads authoritative offline seat state from the optional server/host mod. */
final class MeetingParticipantBridge {
    private static final Method QUERY = load();
    private MeetingParticipantBridge() {}

    static boolean living(MinecraftServer server, UUID uuid) {
        if (QUERY != null) {
            try { return Boolean.TRUE.equals(QUERY.invoke(null, server, uuid)); }
            catch (ReflectiveOperationException | RuntimeException exception) { return false; }
        }
        var player = server.getPlayerList().getPlayer(uuid);
        return player != null && !player.entityTags().contains("spectator");
    }

    private static Method load() {
        try {
            return Class.forName("com.goosethings.meeting.MeetingParticipants")
                    .getMethod("isLivingParticipant", MinecraftServer.class, UUID.class);
        } catch (ReflectiveOperationException | LinkageError exception) { return null; }
    }
}
