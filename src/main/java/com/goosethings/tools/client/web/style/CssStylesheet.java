package com.goosethings.tools.client.web.style;

import com.goosethings.tools.client.web.dom.DomElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deliberately small CSS parser: simple/descendant selectors and safe visual declarations. */
public final class CssStylesheet {
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern BLOCK = Pattern.compile("([^{}]+)\\{([^{}]*)}", Pattern.DOTALL);
    private static final Pattern ATTRIBUTE = Pattern.compile("\\[([a-zA-Z0-9_-]+)(?:=([\"']?)([^\"'\\]]+)\\2)?]");
    private final List<Rule> rules = new ArrayList<>();

    public void add(String css) {
        String clean = COMMENT.matcher(css == null ? "" : css).replaceAll("");
        Matcher matcher = BLOCK.matcher(clean);
        while (matcher.find()) {
            Map<String, String> declarations = parseDeclarations(matcher.group(2));
            for (String selector : matcher.group(1).split(",")) {
                String trimmed = selector.trim();
                if (!trimmed.isEmpty() && trimmed.length() <= 160) {
                    rules.add(new Rule(trimmed, declarations));
                }
            }
        }
    }

    public ComputedStyle compute(DomElement element) {
        ComputedStyle style = defaults(element);
        for (Rule rule : rules) {
            if (matches(element, rule.selector())) {
                rule.declarations().forEach(style::set);
            }
        }
        parseDeclarations(element.attribute("style")).forEach(style::set);
        return style;
    }

    public static boolean matches(DomElement element, String selector) {
        if (selector == null || selector.isBlank()) {
            return false;
        }
        String normalized = selector.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ");
        int index = parts.length - 1;
        DomElement current = element;
        if (!matchesSimple(current, parts[index--])) {
            return false;
        }
        while (index >= 0) {
            current = current.parent();
            while (current != null && !matchesSimple(current, parts[index])) {
                current = current.parent();
            }
            if (current == null) {
                return false;
            }
            index--;
        }
        return true;
    }

    private static boolean matchesSimple(DomElement element, String raw) {
        if (element == null) {
            return false;
        }
        String token = raw.trim();
        if (token.contains(":hover")) {
            if (!element.hovered()) {
                return false;
            }
            token = token.replace(":hover", "");
        }
        if (token.contains(":first-child")) {
            DomElement parent = element.parent();
            if (parent == null || parent.children().isEmpty() || parent.children().getFirst() != element) {
                return false;
            }
            token = token.replace(":first-child", "");
        }

        Matcher attributeMatcher = ATTRIBUTE.matcher(token);
        while (attributeMatcher.find()) {
            String key = attributeMatcher.group(1);
            String expected = attributeMatcher.group(3);
            if (!element.hasAttribute(key)
                    || expected != null && !expected.equals(element.attribute(key))) {
                return false;
            }
        }
        token = attributeMatcher.replaceAll("");

        int cursor = 0;
        StringBuilder tag = new StringBuilder();
        while (cursor < token.length() && token.charAt(cursor) != '.' && token.charAt(cursor) != '#') {
            tag.append(token.charAt(cursor++));
        }
        if (!tag.isEmpty() && !"*".contentEquals(tag)
                && !tag.toString().equalsIgnoreCase(element.tag())) {
            return false;
        }
        while (cursor < token.length()) {
            char prefix = token.charAt(cursor++);
            int start = cursor;
            while (cursor < token.length() && token.charAt(cursor) != '.' && token.charAt(cursor) != '#') {
                cursor++;
            }
            String value = token.substring(start, cursor);
            if (prefix == '.' && !element.classes().contains(value)) {
                return false;
            }
            if (prefix == '#' && !element.id().equals(value)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> parseDeclarations(String source) {
        Map<String, String> values = new LinkedHashMap<>();
        if (source == null) {
            return values;
        }
        for (String declaration : source.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            if (property.matches("[a-z-]{1,40}") && value.length() <= 160) {
                values.put(property, value);
            }
        }
        return values;
    }

    private static ComputedStyle defaults(DomElement element) {
        ComputedStyle style = new ComputedStyle();
        style.set("display", "block");
        style.set("color", "#e8edf2");
        style.set("background-color", "transparent");
        style.set("font-size", "9px");
        style.set("margin", "0px");
        style.set("padding", "0px");
        style.set("border-width", "0px");
        style.set("border-color", "transparent");
        style.set("line-height", "2px");
        switch (element.tag()) {
            case "body" -> style.set("padding", "12px");
            case "h1" -> {
                style.set("font-size", "18px");
                style.set("font-weight", "bold");
                style.set("margin", "4px 0 8px 0");
            }
            case "h2" -> {
                style.set("font-size", "14px");
                style.set("font-weight", "bold");
                style.set("margin", "8px 0 5px 0");
            }
            case "h3", "summary" -> {
                style.set("font-size", "11px");
                style.set("font-weight", "bold");
                style.set("margin", "4px 0");
            }
            case "p", "li" -> style.set("margin", "2px 0 5px 0");
            case "button", "input" -> {
                style.set("background-color", "#263747");
                style.set("border-width", "1px");
                style.set("border-color", "#52718c");
                style.set("padding", "5px 7px");
                style.set("margin", "2px");
                style.set("height", "22px");
            }
            case "a" -> {
                style.set("color", "#79c9ff");
                style.set("margin", "2px 0");
            }
            case "img" -> {
                style.set("width", "32px");
                style.set("height", "32px");
                style.set("margin", "2px 4px 4px 0");
            }
            case "hr" -> {
                style.set("height", "1px");
                style.set("background-color", "#405363");
                style.set("margin", "6px 0");
            }
            default -> {
            }
        }
        return style;
    }

    private record Rule(String selector, Map<String, String> declarations) {
        private Rule {
            declarations = Map.copyOf(declarations);
        }
    }
}
