package com.goosethings.tools.ai;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class AiReportHistoryConfig {
    private static final int DEFAULT_MAX_REPORTS = 50;

    private AiReportHistoryConfig() {
    }

    static int loadMaximumReports() {
        Path file = FabricLoader.getInstance().getConfigDir()
                .resolve("goosetools").resolve("ai-report-history.json");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                JsonObject defaults = new JsonObject();
                defaults.addProperty("max_reports_per_player", DEFAULT_MAX_REPORTS);
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(defaults) + "\n",
                        StandardCharsets.UTF_8);
                return DEFAULT_MAX_REPORTS;
            }
            JsonObject value = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            int configured = value.has("max_reports_per_player")
                    ? value.get("max_reports_per_player").getAsInt() : DEFAULT_MAX_REPORTS;
            return Math.clamp(configured, 1, 200);
        } catch (IOException | RuntimeException exception) {
            GooseTools.LOGGER.warn("Unable to load AI report history config; using {}: {}",
                    DEFAULT_MAX_REPORTS, exception.toString());
            return DEFAULT_MAX_REPORTS;
        }
    }
}
