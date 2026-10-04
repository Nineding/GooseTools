package com.goosethings.tools.nametag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bounded, server-authoritative rules for simple data-driven nametag icons.
 * The server evaluates every private viewer/target condition and sends clients
 * only the texture metadata they are allowed to render.
 */
public record NameTagAttachmentConfig(List<Attachment> attachments) {
    public static final int MAX_BYTES = 65_536;
    public static final int MAX_DEFINITIONS = 64;
    private static final int MAX_TAGS_PER_LIST = 16;
    private static final Set<String> ROOT_KEYS = Set.of("schema_version", "attachments");
    private static final Set<String> ATTACHMENT_KEYS = Set.of(
            "id", "texture", "viewer_tags_all", "viewer_tags_none",
            "target_tags_all", "target_tags_none", "hide_self", "tag_source",
            "requires_full_blood", "width", "height", "color");

    public NameTagAttachmentConfig {
        attachments = List.copyOf(attachments);
        if (attachments.size() > MAX_DEFINITIONS) {
            throw new IllegalArgumentException("Too many nametag attachment definitions");
        }
    }

    public record Attachment(
            String id,
            String texture,
            Set<String> viewerTagsAll,
            Set<String> viewerTagsNone,
            Set<String> targetTagsAll,
            Set<String> targetTagsNone,
            boolean hideSelf,
            boolean identityTags,
            boolean requiresFullBlood,
            float width,
            float height,
            int rgb) {
        public Attachment {
            viewerTagsAll = Set.copyOf(viewerTagsAll);
            viewerTagsNone = Set.copyOf(viewerTagsNone);
            targetTagsAll = Set.copyOf(targetTagsAll);
            targetTagsNone = Set.copyOf(targetTagsNone);
            rgb &= 0x00ff_ffff;
        }

        boolean visible(boolean samePlayer,
                        Set<String> viewerTags,
                        Set<String> identityTagsSet,
                        Set<String> renderedTags,
                        boolean fullBlood) {
            return visible(samePlayer, viewerTags, identityTagsSet,
                    renderedTags, fullBlood, false);
        }

        boolean visible(boolean samePlayer,
                        Set<String> viewerTags,
                        Set<String> identityTagsSet,
                        Set<String> renderedTags,
                        boolean fullBlood,
                        boolean spectatorStatusView) {
            if (hideSelf && samePlayer || requiresFullBlood && !fullBlood) {
                return false;
            }
            Set<String> targetTags = identityTags ? identityTagsSet : renderedTags;
            boolean borrowedDetective = NameTagRolePolicy.hasBorrowedRole(viewerTags, "Detective");
            return (spectatorStatusView || containsAllViewerTags(viewerTags, viewerTagsAll))
                    && containsAllTargetTags(targetTags, targetTagsAll, borrowedDetective)
                    && (spectatorStatusView || disjoint(viewerTags, viewerTagsNone))
                    && disjointTargetTags(
                    targetTags, targetTagsNone, borrowedDetective, spectatorStatusView);
        }
    }

    public static NameTagAttachmentConfig defaults() {
        try (var input = NameTagAttachmentConfig.class.getResourceAsStream("/nametag_attachments.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing default nametag attachment configuration");
            }
            return parse(new String(input.readNBytes(MAX_BYTES + 1), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load default nametag attachments", exception);
        }
    }

    public static NameTagAttachmentConfig parse(String json) {
        if (json.length() > MAX_BYTES || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Nametag attachment configuration exceeds 64 KiB");
        }
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("Trailing JSON data");
            }
            JsonObject root = parsed.getAsJsonObject();
            keys(root, ROOT_KEYS);
            if (!root.has("schema_version") || root.get("schema_version").getAsInt() != 1) {
                throw new IllegalArgumentException("schema_version must be 1");
            }
            JsonArray entries = root.getAsJsonArray("attachments");
            if (entries == null || entries.size() > MAX_DEFINITIONS) {
                throw new IllegalArgumentException("attachments must contain at most " + MAX_DEFINITIONS + " entries");
            }
            List<Attachment> attachments = new ArrayList<>(entries.size());
            Set<String> ids = new HashSet<>();
            for (JsonElement element : entries) {
                JsonObject entry = element.getAsJsonObject();
                keys(entry, ATTACHMENT_KEYS);
                String id = string(entry, "id", 64);
                if (!id.matches("[a-z0-9_.-]+") || !ids.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate attachment ID: " + id);
                }
                String texture = string(entry, "texture", 256);
                if (!validTexture(texture)) {
                    throw new IllegalArgumentException("Invalid attachment texture: " + texture);
                }
                String source = string(entry, "tag_source", 16);
                if (!source.equals("rendered") && !source.equals("identity")) {
                    throw new IllegalArgumentException("tag_source must be rendered or identity");
                }
                float width = boundedFloat(entry, "width");
                float height = boundedFloat(entry, "height");
                String color = string(entry, "color", 7);
                if (!color.matches("#[0-9a-fA-F]{6}")) {
                    throw new IllegalArgumentException("color must use #RRGGBB: " + id);
                }
                attachments.add(new Attachment(
                        id,
                        texture,
                        tags(entry, "viewer_tags_all"),
                        tags(entry, "viewer_tags_none"),
                        tags(entry, "target_tags_all"),
                        tags(entry, "target_tags_none"),
                        bool(entry, "hide_self"),
                        source.equals("identity"),
                        bool(entry, "requires_full_blood"),
                        width,
                        height,
                        Integer.parseInt(color.substring(1), 16)));
            }
            return new NameTagAttachmentConfig(attachments);
        } catch (IOException | IllegalStateException | NullPointerException | ClassCastException exception) {
            throw new IllegalArgumentException(
                    "Invalid nametag attachment JSON: " + exception.getMessage(), exception);
        }
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        for (String value : right) {
            if (left.contains(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAllViewerTags(Set<String> viewerTags, Set<String> requiredTags) {
        for (String tag : requiredTags) {
            if (!NameTagRolePolicy.hasRole(viewerTags, tag)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAllTargetTags(Set<String> targetTags,
                                                 Set<String> requiredTags,
                                                 boolean borrowedDetective) {
        for (String tag : requiredTags) {
            if (targetTags.contains(tag)) {
                continue;
            }
            if (borrowedDetective
                    && tag.equals("detectiveCheckedAngel")
                    && targetTags.contains("seagullNametagDetectiveAngel")) {
                continue;
            }
            if (borrowedDetective
                    && tag.equals("detectiveCheckedDemon")
                    && targetTags.contains("seagullNametagDetectiveDemon")) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean disjointTargetTags(Set<String> targetTags,
                                              Set<String> excludedTags,
                                              boolean borrowedDetective,
                                              boolean spectatorStatusView) {
        for (String tag : excludedTags) {
            if ((borrowedDetective || spectatorStatusView) && tag.equals("inTalk")) {
                continue;
            }
            if (targetTags.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validTexture(String texture) {
        if (!texture.matches("[a-z0-9_.-]{1,64}:[a-z0-9/._-]{1,191}")) {
            return false;
        }
        String path = texture.substring(texture.indexOf(':') + 1);
        return path.startsWith("textures/")
                && path.endsWith(".png")
                && !path.contains("..")
                && !path.contains("//");
    }

    private static Set<String> tags(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() > MAX_TAGS_PER_LIST) {
            throw new IllegalArgumentException(key + " must contain at most " + MAX_TAGS_PER_LIST + " tags");
        }
        Set<String> result = new HashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Expected string tag in " + key);
            }
            String tag = element.getAsString();
            if (!tag.matches("[A-Za-z0-9_.:+-]{1,64}") || !result.add(tag)) {
                throw new IllegalArgumentException("Invalid or duplicate tag in " + key + ": " + tag);
            }
        }
        return result;
    }

    private static float boundedFloat(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected number: " + key);
        }
        float result = value.getAsFloat();
        if (!Float.isFinite(result) || result < 1.0F || result > 64.0F) {
            throw new IllegalArgumentException(key + " must be between 1 and 64");
        }
        return result;
    }

    private static boolean bool(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Expected boolean: " + key);
        }
        return value.getAsBoolean();
    }

    private static void keys(JsonObject object, Set<String> allowed) {
        if (object == null || !object.keySet().equals(allowed)) {
            throw new IllegalArgumentException("Missing object field or unknown configuration field");
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
}
