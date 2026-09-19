package com.goosethings.tools.client.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.goosethings.tools.web.WebBundle;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Page-owned language catalog, independent from Minecraft translate components. */
public final class WebTranslations {
    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();
    private String selected = "auto";

    public WebTranslations(WebBundle bundle, String preferred) {
        bundle.files().forEach((path, bytes) -> {
            if (!path.startsWith("lang/") || !path.endsWith(".json")) {
                return;
            }
            String code = path.substring("lang/".length(), path.length() - ".json".length())
                    .toLowerCase(Locale.ROOT);
            try {
                JsonObject object = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        values.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                languages.put(code, Map.copyOf(values));
            } catch (RuntimeException ignored) {
                // A malformed optional language file does not invalidate the already validated page archive.
            }
        });
        setSelected(preferred);
    }

    public String translate(String key) {
        Map<String, String> active = languages.get(activeCode());
        if (active != null && active.containsKey(key)) {
            return active.get(key);
        }
        Map<String, String> english = languages.get("en_us");
        if (english != null && english.containsKey(key)) {
            return english.get(key);
        }
        Map<String, String> chinese = languages.get("zh_cn");
        if (chinese != null && chinese.containsKey(key)) {
            return chinese.get(key);
        }
        return key;
    }

    public boolean setSelected(String code) {
        String normalized = code == null ? "auto" : code.toLowerCase(Locale.ROOT);
        if (!"auto".equals(normalized) && !languages.containsKey(normalized)) {
            return false;
        }
        selected = normalized;
        return true;
    }

    public String selected() {
        return selected;
    }

    public String activeCode() {
        if (!"auto".equals(selected)) {
            return selected;
        }
        String minecraft = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        if (languages.containsKey(minecraft)) {
            return minecraft;
        }
        return languages.containsKey("en_us") ? "en_us" : languages.keySet().stream().findFirst().orElse("en_us");
    }
}
