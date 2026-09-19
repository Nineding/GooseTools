package com.goosethings.tools.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Side-neutral, bounded display metadata. Never grants visibility or runs commands. */
public record TaskMarkerConfig(Map<String, Integer> colors, Map<String, Task> tasks) {
    public static final int MAX_BYTES = 65536;
    private static final Set<String> CATEGORIES = Set.of("normal", "duck", "emergency", "gold");

    public TaskMarkerConfig {
        colors = Map.copyOf(colors);
        tasks = Map.copyOf(tasks);
    }

    public record Task(String translationKey, String fallback, String category) { }

    public static TaskMarkerConfig defaults() {
        try (var input = TaskMarkerConfig.class.getResourceAsStream("/task_markers.json")) {
            if (input == null) throw new IllegalStateException("Missing bundled task_markers.json");
            return parse(new String(input.readNBytes(MAX_BYTES + 1), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load default task markers", exception);
        }
    }

    public static TaskMarkerConfig parse(String json) {
        if (json.length() > MAX_BYTES || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Task marker configuration exceeds 64 KiB");
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("Trailing JSON data");
            }
            JsonObject root = parsed.getAsJsonObject();
            keys(root, Set.of("schema_version", "colors", "tasks"));
            if (!root.has("schema_version") || !root.get("schema_version").toString().equals("1")) {
                throw new IllegalArgumentException("schema_version must be 1");
            }
            JsonObject palette = root.getAsJsonObject("colors");
            keys(palette, CATEGORIES);
            Map<String, Integer> colors = new LinkedHashMap<>();
            for (String category : CATEGORIES) {
                String color = string(palette, category, 9);
                if (!color.matches("#[0-9a-fA-F]{8}")) {
                    throw new IllegalArgumentException("Color must use #AARRGGBB: " + category);
                }
                colors.put(category, (int) Long.parseLong(color.substring(1), 16));
            }
            JsonObject entries = root.getAsJsonObject("tasks");
            if (entries == null || entries.size() > 512) {
                throw new IllegalArgumentException("tasks must contain at most 512 entries");
            }
            Map<String, Task> tasks = new LinkedHashMap<>();
            for (var entry : entries.entrySet()) {
                if (!entry.getKey().matches("[a-zA-Z0-9_.-]{1,96}")) {
                    throw new IllegalArgumentException("Invalid task ID: " + entry.getKey());
                }
                JsonObject task = entry.getValue().getAsJsonObject();
                keys(task, Set.of("translation_key", "fallback", "category"));
                String key = string(task, "translation_key", 160);
                if (!key.matches("[a-zA-Z0-9_.-]+")) {
                    throw new IllegalArgumentException("Invalid translation key: " + key);
                }
                String fallback = string(task, "fallback", 160);
                String category = string(task, "category", 16);
                if (!CATEGORIES.contains(category)) {
                    throw new IllegalArgumentException("Unknown task category: " + category);
                }
                tasks.put(entry.getKey(), new Task(key, fallback, category));
            }
            return new TaskMarkerConfig(colors, tasks);
        } catch (IOException | IllegalStateException | NullPointerException exception) {
            throw new IllegalArgumentException("Invalid task marker JSON: " + exception.getMessage(), exception);
        }
    }

    private static void keys(JsonObject object, Set<String> allowed) {
        if (object == null || !allowed.containsAll(object.keySet())) {
            throw new IllegalArgumentException("Missing object or unknown configuration field");
        }
    }

    private static String string(JsonObject object, String key, int limit) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected string: " + key);
        }
        String result = value.getAsString();
        if (result.isBlank() || result.length() > limit || result.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid string: " + key);
        }
        return result;
    }

    public Task task(String id) {
        return tasks.getOrDefault(id, new Task("item.task." + id + ".available", "Task", "normal"));
    }

    public int background(String id, String kind) {
        // Explicit gold markers remain special; configured IDs override the old emergency transport
        // category used for duck prerequisites. Unknown IDs still honor runtime emergency/gold.
        String category = "gold".equals(kind) ? "gold"
                : tasks.containsKey(id) ? tasks.get(id).category()
                : CATEGORIES.contains(kind) ? kind : "normal";
        return colors.get(category);
    }

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        JsonObject palette = new JsonObject();
        colors.forEach((key, color) -> palette.addProperty(key, String.format("#%08X", color)));
        root.add("colors", palette);
        JsonObject entries = new JsonObject();
        tasks.forEach((id, task) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("translation_key", task.translationKey());
            entry.addProperty("fallback", task.fallback());
            entry.addProperty("category", task.category());
            entries.add(id, entry);
        });
        root.add("tasks", entries);
        return root.toString();
    }
}
