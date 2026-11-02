package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import java.util.UUID;

/** A matching private report can repair a zero in the separate presentation cache. */
final class AiSummaryCoins {
    private AiSummaryCoins() {}

    static int resolve(long gameId, UUID viewer, int snapshotCoins, JsonObject report) {
        if (snapshotCoins != 0 || viewer == null || report == null) return snapshotCoins;
        try {
            if (report.get("game_id").getAsBigDecimal().longValueExact() != gameId) return snapshotCoins;
            JsonObject personal = report.getAsJsonObject("personal");
            if (!viewer.equals(UUID.fromString(personal.get("player_uuid").getAsString()))) return snapshotCoins;
            int coins = personal.get("gained_coins").getAsBigDecimal().intValueExact();
            return coins > 0 ? coins : snapshotCoins;
        } catch (RuntimeException ignored) {
            // Older, incomplete, or unrelated reports must never manufacture a reward amount.
            return snapshotCoins;
        }
    }
}
