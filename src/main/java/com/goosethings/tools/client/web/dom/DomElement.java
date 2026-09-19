package com.goosethings.tools.client.web.dom;

import com.goosethings.tools.client.web.WebTranslations;
import net.minecraft.network.chat.Component;
import org.mozilla.javascript.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Small mutable DOM node exposed only through the sandbox bridge. */
public final class DomElement {
    private static final Set<String> INLINE_TAGS = Set.of(
            "#text", "span", "strong", "b", "em", "i", "u", "code", "br", "small");

    private final String tag;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final Set<String> classes = new LinkedHashSet<>();
    private final List<DomElement> children = new ArrayList<>();
    private final Map<String, List<Function>> eventHandlers = new LinkedHashMap<>();
    private DomElement parent;
    private String rawText;
    private String value = "";
    private boolean hidden;
    private boolean hovered;

    private int x;
    private int y;
    private int width;
    private int height;

    public DomElement(String tag) {
        this(tag, "");
    }

    public DomElement(String tag, String rawText) {
        this.tag = tag.toLowerCase(Locale.ROOT);
        this.rawText = rawText;
    }

    public String tag() {
        return tag;
    }

    public DomElement parent() {
        return parent;
    }

    public List<DomElement> children() {
        return Collections.unmodifiableList(children);
    }

    public void appendChild(DomElement child) {
        if (child == this || isAncestorOf(child)) {
            throw new IllegalArgumentException("DOM cycle");
        }
        if (child.parent != null) {
            child.parent.children.remove(child);
        }
        child.parent = this;
        children.add(child);
    }

    private boolean isAncestorOf(DomElement candidate) {
        for (DomElement current = this; current != null; current = current.parent) {
            if (current == candidate) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String attribute(String key) {
        return attributes.getOrDefault(key.toLowerCase(Locale.ROOT), "");
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public void removeAttribute(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        attributes.remove(normalized);
        if ("hidden".equals(normalized)) {
            hidden = false;
        }
    }

    public void setAttribute(String key, String value) {
        String normalized = key.toLowerCase(Locale.ROOT);
        attributes.put(normalized, value == null ? "" : value);
        if ("class".equals(normalized)) {
            classes.clear();
            for (String className : attributes.get(normalized).trim().split("\\s+")) {
                if (!className.isBlank()) {
                    classes.add(className);
                }
            }
        } else if ("value".equals(normalized)) {
            this.value = attributes.get(normalized);
        } else if ("hidden".equals(normalized)) {
            hidden = true;
        }
    }

    public Set<String> classes() {
        return Collections.unmodifiableSet(classes);
    }

    public void addClass(String className) {
        if (className != null && className.matches("[a-zA-Z0-9_-]{1,64}")) {
            classes.add(className);
            attributes.put("class", String.join(" ", classes));
        }
    }

    public void removeClass(String className) {
        classes.remove(className);
        attributes.put("class", String.join(" ", classes));
    }

    public boolean toggleClass(String className) {
        if (classes.contains(className)) {
            removeClass(className);
            return false;
        }
        addClass(className);
        return true;
    }

    public String id() {
        return attribute("id");
    }

    public String rawText() {
        return rawText;
    }

    public void setRawText(String text) {
        rawText = text == null ? "" : text;
        children.clear();
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        attributes.put("value", this.value);
    }

    public boolean hidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean hovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return !hidden
                && mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    public boolean isTextNode() {
        return "#text".equals(tag);
    }

    public boolean isInlineTextContainer() {
        if (hasAttribute("data-mc-translate") || hasAttribute("data-i18n")) {
            return true;
        }
        if (children.isEmpty()) {
            return !Set.of("img", "input", "hr").contains(tag);
        }
        for (DomElement child : children) {
            if (!INLINE_TAGS.contains(child.tag()) || !child.isInlineSubtree()) {
                return false;
            }
        }
        return true;
    }

    private boolean isInlineSubtree() {
        if (!INLINE_TAGS.contains(tag)) {
            return false;
        }
        for (DomElement child : children) {
            if (!child.isInlineSubtree()) {
                return false;
            }
        }
        return true;
    }

    public String textContent(WebTranslations translations) {
        if (hasAttribute("data-i18n")) {
            return translations.translate(attribute("data-i18n"));
        }
        if (hasAttribute("data-mc-translate")) {
            return Component.translatable(attribute("data-mc-translate")).getString();
        }
        if (isTextNode()) {
            return rawText;
        }
        if (children.isEmpty()) {
            return rawText == null ? "" : rawText;
        }
        StringBuilder text = new StringBuilder();
        for (DomElement child : children) {
            if ("br".equals(child.tag)) {
                text.append('\n');
            } else {
                text.append(child.textContent(translations));
            }
        }
        return text.toString().strip();
    }

    public void addEventHandler(String eventName, Function function) {
        eventHandlers.computeIfAbsent(eventName.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                .add(function);
    }

    public List<Function> eventHandlers(String eventName) {
        return List.copyOf(eventHandlers.getOrDefault(eventName.toLowerCase(Locale.ROOT), List.of()));
    }

    public boolean hasEventHandler(String eventName) {
        return !eventHandlers.getOrDefault(eventName.toLowerCase(Locale.ROOT), List.of()).isEmpty();
    }
}
