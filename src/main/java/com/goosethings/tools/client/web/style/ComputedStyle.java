package com.goosethings.tools.client.web.style;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ComputedStyle {
    private final Map<String, String> values = new LinkedHashMap<>();

    public void set(String property, String value) {
        values.put(property.toLowerCase(Locale.ROOT), value.trim());
    }

    public String get(String property, String fallback) {
        return values.getOrDefault(property.toLowerCase(Locale.ROOT), fallback);
    }

    public boolean is(String property, String value) {
        return get(property, "").equalsIgnoreCase(value);
    }

    public int pixels(String property, int fallback, int relativeTo) {
        return parseLength(get(property, ""), fallback, relativeTo);
    }

    public float fontScale() {
        int pixels = pixels("font-size", 9, 9);
        return Math.max(0.75F, Math.min(3.0F, pixels / 9.0F));
    }

    public int color(String property, int fallback) {
        return parseColor(get(property, ""), fallback);
    }

    public static int parseLength(String value, int fallback, int relativeTo) {
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value)) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("px")) {
                return Math.round(Float.parseFloat(normalized.substring(0, normalized.length() - 2)));
            }
            if (normalized.endsWith("%")) {
                return Math.round(relativeTo * Float.parseFloat(
                        normalized.substring(0, normalized.length() - 1)) / 100.0F);
            }
            return Math.round(Float.parseFloat(normalized));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int parseColor(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "transparent" -> 0x00000000;
            case "white" -> 0xFFFFFFFF;
            case "black" -> 0xFF000000;
            case "red" -> 0xFFFF5555;
            case "green" -> 0xFF55FF55;
            case "blue" -> 0xFF5555FF;
            case "yellow" -> 0xFFFFFF55;
            case "gray", "grey" -> 0xFFAAAAAA;
            default -> parseHexColor(normalized, fallback);
        };
    }

    private static int parseHexColor(String value, int fallback) {
        if (!value.startsWith("#")) {
            if (value.startsWith("rgb(")) {
                return parseRgb(value, fallback);
            }
            return fallback;
        }
        try {
            String hex = value.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    private static int parseRgb(String value, int fallback) {
        try {
            String inside = value.substring(value.indexOf('(') + 1, value.lastIndexOf(')'));
            String[] parts = inside.split(",");
            if (parts.length != 3) {
                return fallback;
            }
            int red = Math.clamp(Integer.parseInt(parts[0].trim()), 0, 255);
            int green = Math.clamp(Integer.parseInt(parts[1].trim()), 0, 255);
            int blue = Math.clamp(Integer.parseInt(parts[2].trim()), 0, 255);
            return 0xFF000000 | red << 16 | green << 8 | blue;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
