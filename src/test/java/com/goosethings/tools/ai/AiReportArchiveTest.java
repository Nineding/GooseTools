package com.goosethings.tools.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReportArchiveTest {
    @TempDir
    Path temporary;

    @Test
    void persistsPrivateReportsAndPrunesOldestPerPlayer() throws Exception {
        UUID alice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID bob = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AiReportArchive archive = new AiReportArchive(temporary.resolve("reports"), 2);

        archive.store(alice, report(1, 1000, "Alice 1"));
        archive.store(alice, report(2, 2000, "Alice 2"));
        archive.store(alice, report(3, 3000, "Alice 3"));
        archive.store(bob, report(9, 9000, "Bob 9"));

        Map<UUID, NavigableMap<Long, AiReportArchive.StoredReport>> loaded = archive.loadAll();

        assertEquals(2, loaded.get(alice).size());
        assertFalse(loaded.get(alice).containsKey(1L));
        assertTrue(loaded.get(alice).containsKey(2L));
        assertTrue(loaded.get(alice).containsKey(3L));
        assertEquals(1, loaded.get(bob).size());
        assertTrue(loaded.get(bob).containsKey(9L));
    }

    @Test
    void damagedReportDoesNotPreventOtherMatchesLoading() throws Exception {
        UUID player = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AiReportArchive archive = new AiReportArchive(temporary.resolve("reports"), 50);
        archive.store(player, report(4, 4000, "Valid"));
        Path playerDirectory = temporary.resolve("reports").resolve(player.toString());
        Files.writeString(playerDirectory.resolve("5.json.gz"), "not gzip");

        Map<UUID, NavigableMap<Long, AiReportArchive.StoredReport>> loaded = archive.loadAll();

        assertEquals(1, loaded.get(player).size());
        assertTrue(loaded.get(player).containsKey(4L));
    }

    private static String report(long gameId, long generatedAt, String playerName) {
        return """
                {
                  "game_id":%d,
                  "generated_at":%d,
                  "global":{"title":"Title","archive_summary":"Summary","summary":"Full","turning_points":[]},
                  "highlights":[],
                  "personal":{"player_uuid":"00000000-0000-0000-0000-000000000000","player_name":"%s"}
                }
                """.formatted(gameId, generatedAt, playerName);
    }
}
