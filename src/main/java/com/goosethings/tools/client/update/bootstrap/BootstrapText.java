package com.goosethings.tools.client.update.bootstrap;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Launcher strings use the same English/Chinese resources as the Minecraft screens. */
final class BootstrapText {
    private final Map<String, String> values = new HashMap<>();
    BootstrapText(Path game) {
        String language = Locale.getDefault().getLanguage().equals("zh") ? "zh_cn" : "en_us";
        try {
            for (String line : Files.readAllLines(game.resolve("options.txt"))) {
                if (line.startsWith("lang:")) language = line.substring(5).equals("zh_cn") ? "zh_cn" : "en_us";
            }
        } catch (Exception ignored) { }
        load("en_us");
        if (!language.equals("en_us")) load(language);
    }
    private void load(String language) {
        try (var input = BootstrapText.class.getResourceAsStream("/assets/goosetools/lang/" + language + ".json")) {
            if (input == null) return;
            var json = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            json.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
        } catch (Exception ignored) { }
    }
    String text(String key, String fallback, Object... arguments) {
        return String.format(Locale.ROOT, values.getOrDefault("screen.goosetools.update." + key, fallback), arguments);
    }
}
