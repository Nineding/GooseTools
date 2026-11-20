package com.goosethings.tools.nametag;

import com.goosethings.tools.marker.PlayerMarkerCatalog;
import com.goosethings.tools.marker.PlayerMarkerPolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.ScoreHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Skill results belong to the viewer and the current match. */
public final class VerifiedPlayerMarkers {
    public static final int FLAG = 1 << 10;
    private static final Map<MinecraftServer, Match> MATCHES = new WeakHashMap<>();

    private VerifiedPlayerMarkers() {}

    public static int remember(ServerPlayer viewer, int seat, String roleKey, boolean corpseOnly) {
        var role = PlayerMarkerCatalog.byRoleKey(roleKey);
        if (role == null || PlayerMarkerPolicy.objectiveForSeat(seat).isEmpty()
                || !eligible(viewer)) return 0;
        var server = viewer.level().getServer();
        int matchId = matchId(server, "#Global");
        if (matchId < 0 || matchId(server, viewer.getScoreboardName()) != matchId) return 0;
        Match match = MATCHES.get(server);
        if (match == null || match.id() != matchId) {
            match = new Match(matchId, new HashMap<>());
            MATCHES.put(server, match);
        }
        match.results().put(new Target(viewer.getUUID(), seat, corpseOnly), role.code());
        return 1;
    }

    static int code(ServerPlayer viewer, int seat, boolean corpse) {
        if (!eligible(viewer) || !viewer.entityTags().contains("inTalk")) return 0;
        var server = viewer.level().getServer();
        Match match = MATCHES.get(server);
        if (match == null || match.id() != matchId(server, "#Global")
                || match.id() != matchId(server, viewer.getScoreboardName())) return 0;
        int code = corpse ? match.results().getOrDefault(new Target(viewer.getUUID(), seat, true), 0) : 0;
        return code != 0 ? code : match.results().getOrDefault(new Target(viewer.getUUID(), seat, false), 0);
    }

    private static boolean eligible(ServerPlayer viewer) {
        var tags = viewer.entityTags();
        return tags.contains("players") && !tags.contains("spectator") && !tags.contains("deadInMap")
                && !tags.contains("endGame") && !tags.contains("inTutorial");
    }

    private static int matchId(MinecraftServer server, String holder) {
        var board = server.getScoreboard();
        var objective = board.getObjective("ggdGameId");
        var value = objective == null ? null : board.getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return value == null ? -1 : value.value();
    }

    private record Target(UUID viewer, int seat, boolean corpseOnly) {}
    private record Match(int id, Map<Target, Integer> results) {}
}
