package com.goosethings.tools.client.ai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

/** Client-side parser for the server-validated, fixed-palette report emphasis markup. */
final class AiReportMarkup {
    private static final Map<String, Integer> COLORS = Map.of(
            "gold", 0xFFD36E,
            "green", 0x9FE3A5,
            "red", 0xFF8E8E,
            "aqua", 0x91D7FF,
            "purple", 0xC7A0FF);

    private AiReportMarkup() {
    }

    static Component parse(String value, int baseColor) {
        String input = value == null ? "" : value;
        MutableComponent output = Component.empty().withStyle(style -> style.withColor(baseColor));
        int cursor = 0;
        Integer active = null;
        while (cursor < input.length()) {
            int open = input.indexOf("[[", cursor);
            if (open < 0) {
                append(output, input.substring(cursor), active, baseColor);
                break;
            }
            append(output, input.substring(cursor, open), active, baseColor);
            int close = input.indexOf("]]", open + 2);
            if (close < 0) {
                append(output, input.substring(open), active, baseColor);
                break;
            }
            String token = input.substring(open + 2, close);
            if ("/".equals(token)) active = null;
            else if (active == null && COLORS.containsKey(token)) active = COLORS.get(token);
            else append(output, input.substring(open, close + 2), active, baseColor);
            cursor = close + 2;
        }
        return output;
    }

    private static void append(MutableComponent output, String text, Integer active, int baseColor) {
        if (text.isEmpty()) return;
        int color = active == null ? baseColor : active;
        output.append(Component.literal(text).withStyle(style -> style.withColor(color)));
    }
}
