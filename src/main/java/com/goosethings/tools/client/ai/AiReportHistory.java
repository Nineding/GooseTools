package com.goosethings.tools.client.ai;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

final class AiReportHistory {
    private static final int MAX_ENTRY_CHARS = 120;
    private final NavigableMap<Long, JsonObject> reports = new TreeMap<>();

    long add(JsonObject report) {
        long gameId = report.get("game_id").getAsLong();
        reports.put(gameId, report.deepCopy());
        return gameId;
    }

    JsonObject get(long gameId) {
        JsonObject value = reports.get(gameId);
        return value == null ? null : value.deepCopy();
    }

    JsonObject latest() {
        return reports.isEmpty() ? null : reports.lastEntry().getValue().deepCopy();
    }

    int size() {
        return reports.size();
    }

    void clear() {
        reports.clear();
    }

    List<Entry> entriesNewestFirst() {
        List<Entry> result = new ArrayList<>();
        for (JsonObject report : reports.descendingMap().values()) {
            JsonObject global = report.getAsJsonObject("global");
            result.add(new Entry(
                    report.get("game_id").getAsLong(),
                    longValue(report, "generated_at"),
                    string(global, "title"),
                    entrySummary(global),
                    report.deepCopy()));
        }
        result.sort(Comparator.comparingLong(Entry::gameId).reversed());
        return List.copyOf(result);
    }

    static String entrySummary(JsonObject global) {
        String value = string(global, "archive_summary");
        if (value.isBlank()) value = string(global, "summary");
        if (value.isBlank()) value = string(global, "title");
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() <= MAX_ENTRY_CHARS
                ? value : value.substring(0, MAX_ENTRY_CHARS - 1) + "…";
    }

    private static String string(JsonObject object, String key) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                    ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static long longValue(JsonObject object, String key) {
        try {
            return object.has(key) ? object.get(key).getAsLong() : 0L;
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    record Entry(long gameId, long generatedAt, String title, String summary, JsonObject report) {
    }
}
