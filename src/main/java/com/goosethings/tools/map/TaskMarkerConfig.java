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
public record TaskMarkerConfig(Map<String, Integer> colors, Map<String, Task> tasks,
                               Map<String, Gradient> gradients) {
    public static final int MAX_BYTES = 65536;
    private static final Set<String> CATEGORIES = Set.of("normal", "duck", "emergency", "gold", "series");
    private static final Gradient SERIES_DEFAULT = new Gradient(0xD02879C8, 0xD026A66A);

    public TaskMarkerConfig {
        colors = Map.copyOf(colors);
        tasks = Map.copyOf(tasks);
        gradients = Map.copyOf(gradients);
    }

    public record Task(String translationKey, String fallback, String category) { }
    public record Gradient(int start, int end) {
        public int at(double fraction) {
            double t = Math.max(0, Math.min(1, Double.isFinite(fraction) ? fraction : 0));
            int result = 0;
            for (int shift = 0; shift <= 24; shift += 8) {
                int a = (start >>> shift) & 255, b = (end >>> shift) & 255;
                result |= ((int) Math.round(a + (b - a) * t)) << shift;
            }
            return result;
        }
    }

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
            keys(root, Set.of("schema_version", "colors", "tasks", "gradients"));
            if (!root.has("schema_version") || !root.get("schema_version").toString().equals("1")) {
                throw new IllegalArgumentException("schema_version must be 1");
            }
            JsonObject palette = root.getAsJsonObject("colors");
            keys(palette, CATEGORIES);
            Map<String, Integer> colors = new LinkedHashMap<>();
            for (String category : CATEGORIES) {
                colors.put(category, "series".equals(category) && !palette.has(category)
                        ? SERIES_DEFAULT.start() : color(palette, category));
            }
            Map<String, Gradient> gradients = new LinkedHashMap<>();
            gradients.put("series", SERIES_DEFAULT);
            if (root.has("gradients")) {
                JsonObject ramps = root.getAsJsonObject("gradients");
                keys(ramps, CATEGORIES);
                for (var entry : ramps.entrySet()) {
                    JsonObject ramp = entry.getValue().getAsJsonObject();
                    keys(ramp, Set.of("start", "end"));
                    gradients.put(entry.getKey(), new Gradient(color(ramp, "start"), color(ramp, "end")));
                }
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
            return new TaskMarkerConfig(colors, tasks, gradients);
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

    private static int color(JsonObject object, String key) {
        String value = string(object, key, 9);
        if (!value.matches("#[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException("Color must use #AARRGGBB: " + key);
        }
        return (int) Long.parseLong(value.substring(1), 16);
    }

    public Task task(String id) {
        return tasks.getOrDefault(id, new Task("item.task." + id + ".available", "Task", "normal"));
    }

    public int background(String id, String kind) {
        return backgroundAt(id, kind, 0);
    }

    public int backgroundAt(String id, String kind, double fraction) {
        String category = category(id, kind);
        Gradient gradient = gradients.get(category);
        return gradient == null ? colors.get(category) : gradient.at(fraction);
    }

    private String category(String id, String kind) {
        // Explicit gold markers remain special; configured IDs override the old emergency transport
        // category used for duck prerequisites. Unknown IDs still honor runtime emergency/gold.
        return "gold".equals(kind) ? "gold"
                : tasks.containsKey(id) ? tasks.get(id).category()
                : CATEGORIES.contains(kind) ? kind : "normal";
    }

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        JsonObject palette = new JsonObject();
        colors.forEach((key, color) -> palette.addProperty(key, String.format("#%08X", color)));
        root.add("colors", palette);
        JsonObject ramps = new JsonObject();
        gradients.forEach((category, gradient) -> {
            JsonObject ramp = new JsonObject();
            ramp.addProperty("start", String.format("#%08X", gradient.start()));
            ramp.addProperty("end", String.format("#%08X", gradient.end()));
            ramps.add(category, ramp);
        });
        root.add("gradients", ramps);
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
