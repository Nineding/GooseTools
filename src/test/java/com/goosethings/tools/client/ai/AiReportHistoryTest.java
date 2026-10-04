package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReportHistoryTest {
    @Test
    void listsNewestMatchFirstAndUsesArchiveSummary() {
        AiReportHistory history = new AiReportHistory();
        history.add(report(4, "第四局一句话"));
        history.add(report(7, "第七局一句话"));

        assertEquals(7L, history.entriesNewestFirst().get(0).gameId());
        assertEquals("第七局一句话", history.entriesNewestFirst().get(0).summary());
        assertEquals(2, history.size());
    }

    @Test
    void oldReportsFallBackToFullSummary() {
        JsonObject global = JsonParser.parseString(
                "{\"title\":\"旧报告\",\"summary\":\"旧版本只有完整总结。\"}").getAsJsonObject();
        assertEquals("旧版本只有完整总结。", AiReportHistory.entrySummary(global));
    }

    @Test
    void returnedReportIsDefensiveCopy() {
        AiReportHistory history = new AiReportHistory();
        history.add(report(3, "原始摘要"));
        JsonObject copy = history.latest();
        copy.getAsJsonObject("global").addProperty("archive_summary", "已修改");
        assertTrue(history.latest().toString().contains("原始摘要"));
    }

    private static JsonObject report(long gameId, String archiveSummary) {
        return JsonParser.parseString("""
                {
                  "game_id":%d,
                  "generated_at":1234,
                  "global":{"title":"标题","archive_summary":"%s","summary":"完整总结","turning_points":[]},
                  "highlights":[],
                  "personal":{}
                }
                """.formatted(gameId, archiveSummary)).getAsJsonObject();
    }
}
