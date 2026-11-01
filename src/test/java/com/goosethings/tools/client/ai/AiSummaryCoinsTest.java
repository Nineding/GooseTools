package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiSummaryCoinsTest {
    private static final UUID PLAYER=UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test void restoresThe315CoinRewardFromMatch2582WithoutUsingCurrentWallet() {
        assertEquals(315,AiSummaryCoins.resolve(2582,PLAYER,0,report(2582,PLAYER,"315")));
    }

    @Test void neverBorrowsAnotherPlayersOrAnotherMatchesReward() {
        assertEquals(0,AiSummaryCoins.resolve(2583,PLAYER,0,report(2582,PLAYER,"315")));
        assertEquals(0,AiSummaryCoins.resolve(2582,UUID.randomUUID(),0,report(2582,PLAYER,"315")));
    }

    @Test void preservesAValidNonzeroSnapshotAndRealZeroRewards() {
        assertEquals(315,AiSummaryCoins.resolve(2582,PLAYER,315,report(2582,PLAYER,"10")));
        assertEquals(0,AiSummaryCoins.resolve(2582,PLAYER,0,report(2582,PLAYER,"0")));
    }

    @Test void incompleteAndMalformedOldReportsCannotBecomeZeroOrOverflowRewards() {
        assertEquals(0,AiSummaryCoins.resolve(2582,PLAYER,0,null));
        assertEquals(0,AiSummaryCoins.resolve(2582,PLAYER,0,new JsonObject()));
        for(String value:new String[]{"-1","315.5","2147483648","null","{}","\"invalid\""})
            assertEquals(0,AiSummaryCoins.resolve(2582,PLAYER,0,report(2582,PLAYER,value)),value);
        JsonObject fractional=report(2582,PLAYER,"315");fractional.addProperty("game_id",2582.5);
        assertEquals(0,AiSummaryCoins.resolve(2582,PLAYER,0,fractional));
    }

    private static JsonObject report(long id,UUID viewer,String coins) {
        return JsonParser.parseString("""
                {"game_id":%d,"personal":{"player_uuid":"%s","gained_coins":%s}}
                """.formatted(id,viewer,coins)).getAsJsonObject();
    }
}
