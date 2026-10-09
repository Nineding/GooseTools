package com.goosethings.tools.game;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.goosethings.tools.GooseTools;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class GameRecords {
    record Best(long score, long time) {}
    private final Map<String, Best> records = new HashMap<>();
    private final Path file;
    private boolean dirty;
    GameRecords(Path file) {
        this.file = file;
        try {
            if (Files.isRegularFile(file) && Files.size(file) <= 8 * 1024 * 1024) {
                Map<String, Best> saved = new Gson().fromJson(Files.readString(file), new TypeToken<Map<String, Best>>() {}.getType());
                if (saved != null && saved.size() <= 20_000) saved.forEach((k, v) -> {
                    if (k != null && k.length() < 100 && v != null && v.score >= 0 && v.time >= 0) records.put(k, v);
                });
            }
        } catch (Exception e) { GooseTools.LOGGER.warn("Could not read arcade records", e); }
    }
    private String key(UUID player, GameSession session) { return player + "/" + session.type.id + "/" + session.mode() + "/" + session.difficulty(); }
    Best get(UUID player, GameSession session) { return records.getOrDefault(key(player, session), new Best(0, 0)); }
    void update(UUID player, GameSession session) {
        String key = key(player, session); Best best = get(player, session);
        long score = Math.max(best.score, session.game.score), time = best.time, elapsed = session.activeMillis();
        if (session.type == GameType.MINES && session.phase() == GameSession.WON && elapsed > 0 && (time == 0 || elapsed < time)) time = elapsed;
        if ((score != best.score || time != best.time) && (records.containsKey(key) || records.size() < 20_000)) { records.put(key, new Best(score, time)); dirty = true; }
    }
    void save() {
        if (!dirty) return;
        try {
            Files.createDirectories(file.getParent()); Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, new Gson().toJson(records));
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (Exception e) { GooseTools.LOGGER.warn("Could not save arcade records", e); }
    }
}
