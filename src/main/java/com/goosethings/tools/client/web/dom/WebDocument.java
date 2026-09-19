package com.goosethings.tools.client.web.dom;

import com.goosethings.tools.client.web.WebTranslations;
import com.goosethings.tools.client.web.style.CssStylesheet;
import com.goosethings.tools.web.WebBundle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parsed page DOM, CSS and script sources from one validated bundle. */
public final class WebDocument {
    private static final int MAX_DOM_NODES = 4096;
    private static final int MAX_SCRIPT_CHARS = 512 * 1024;

    private final WebBundle bundle;
    private final String pageId;
    private final String pageFile;
    private final DomElement root;
    private final CssStylesheet stylesheet;
    private final WebTranslations translations;
    private final List<ScriptSource> scripts;
    private Runnable mutationListener = () -> { };

    private WebDocument(
            WebBundle bundle,
            String pageId,
            String pageFile,
            DomElement root,
            CssStylesheet stylesheet,
            WebTranslations translations,
            List<ScriptSource> scripts) {
        this.bundle = bundle;
        this.pageId = pageId;
        this.pageFile = pageFile;
        this.root = root;
        this.stylesheet = stylesheet;
        this.translations = translations;
        this.scripts = List.copyOf(scripts);
    }

    public static WebDocument parse(WebBundle bundle, String pageId, String preferredLanguage) throws IOException {
        String pageFile = bundle.pageFile(pageId);
        if (pageFile == null) {
            throw new IOException("Unknown page: " + pageId);
        }
        byte[] htmlBytes = bundle.files().get(pageFile);
        org.jsoup.nodes.Document html = Jsoup.parse(
                new String(htmlBytes, StandardCharsets.UTF_8),
                pageFile,
                Parser.htmlParser());

        CssStylesheet stylesheet = new CssStylesheet();
        for (Element style : html.select("style")) {
            stylesheet.add(style.data());
        }
        for (Element link : html.select("link[rel=stylesheet][href]")) {
            String path = resolveRelative(pageFile, link.attr("href"));
            byte[] css = bundle.files().get(path);
            if (css != null) {
                stylesheet.add(new String(css, StandardCharsets.UTF_8));
            }
        }

        List<ScriptSource> scripts = new ArrayList<>();
        int scriptChars = 0;
        for (Element script : html.select("script")) {
            String name;
            String source;
            if (script.hasAttr("src")) {
                name = resolveRelative(pageFile, script.attr("src"));
                byte[] bytes = bundle.files().get(name);
                if (bytes == null) {
                    continue;
                }
                source = new String(bytes, StandardCharsets.UTF_8);
            } else {
                name = pageFile + "#inline";
                source = script.data();
            }
            scriptChars += source.length();
            if (scriptChars > MAX_SCRIPT_CHARS) {
                throw new IOException("Page scripts are too large");
            }
            scripts.add(new ScriptSource(name, source));
        }

        Counter counter = new Counter();
        DomElement root = convert(html.body(), counter);
        return new WebDocument(
                bundle,
                pageId,
                pageFile,
                root,
                stylesheet,
                new WebTranslations(bundle, preferredLanguage),
                scripts);
    }

    private static DomElement convert(Node node, Counter counter) throws IOException {
        if (++counter.value > MAX_DOM_NODES) {
            throw new IOException("Page contains too many DOM nodes");
        }
        if (node instanceof TextNode textNode) {
            String normalized = textNode.getWholeText().replaceAll("[\\t\\r\\n ]+", " ");
            return new DomElement("#text", normalized);
        }
        if (!(node instanceof Element element)) {
            return new DomElement("span");
        }
        DomElement result = new DomElement(element.tagName());
        for (Attribute attribute : element.attributes()) {
            result.setAttribute(attribute.getKey(), attribute.getValue());
        }
        if ("input".equals(result.tag())) {
            result.setValue(result.attribute("value"));
        }
        for (Node child : element.childNodes()) {
            if (child instanceof Element childElement
                    && SetHolder.NON_VISUAL_TAGS.contains(childElement.tagName())) {
                continue;
            }
            DomElement converted = convert(child, counter);
            if (!converted.isTextNode() || !converted.rawText().isBlank()) {
                result.appendChild(converted);
            }
        }
        return result;
    }

    public MutableComponent componentFor(DomElement element) {
        MutableComponent component;
        if (element.hasAttribute("data-mc-translate")) {
            String[] rawArgs = element.attribute("data-mc-with").isBlank()
                    ? new String[0]
                    : element.attribute("data-mc-with").split("\\|");
            Object[] args = new Object[rawArgs.length];
            for (int i = 0; i < rawArgs.length; i++) {
                String raw = rawArgs[i].trim();
                args[i] = raw.startsWith("@")
                        ? Component.translatable(raw.substring(1))
                        : Component.literal(raw);
            }
            component = Component.translatable(element.attribute("data-mc-translate"), args);
        } else if (element.hasAttribute("data-i18n")) {
            component = Component.literal(translations.translate(element.attribute("data-i18n")));
        } else {
            component = Component.literal(element.textContent(translations));
        }
        if (SetHolder.BOLD_TAGS.contains(element.tag())) {
            component.withStyle(ChatFormatting.BOLD);
        }
        if (SetHolder.ITALIC_TAGS.contains(element.tag())) {
            component.withStyle(ChatFormatting.ITALIC);
        }
        if ("u".equals(element.tag()) || "a".equals(element.tag())) {
            component.withStyle(ChatFormatting.UNDERLINE);
        }
        return component;
    }

    public List<DomElement> querySelectorAll(String selector) {
        List<DomElement> results = new ArrayList<>();
        collect(root, selector, results);
        return results;
    }

    private static void collect(DomElement element, String selector, List<DomElement> results) {
        if (com.goosethings.tools.client.web.style.CssStylesheet.matches(element, selector)) {
            results.add(element);
        }
        element.children().forEach(child -> collect(child, selector, results));
    }

    public DomElement getElementById(String id) {
        return querySelectorAll("#" + id).stream().findFirst().orElse(null);
    }

    public DomElement createElement(String tag) {
        String normalized = tag == null ? "" : tag.toLowerCase(Locale.ROOT);
        if (!normalized.matches("(div|span|p|h[1-3]|button|a|ul|li|section|article|details|summary|img|input|br|hr)")) {
            throw new IllegalArgumentException("Unsupported element: " + tag);
        }
        return new DomElement(normalized);
    }

    public void setMutationListener(Runnable mutationListener) {
        this.mutationListener = mutationListener == null ? () -> { } : mutationListener;
    }

    public void mutated() {
        mutationListener.run();
    }

    public boolean setLanguage(String code) {
        boolean changed = translations.setSelected(code);
        if (changed) {
            mutated();
        }
        return changed;
    }

    public String resolveResource(String source) {
        return resolveRelative(pageFile, source);
    }

    private static String resolveRelative(String baseFile, String source) {
        String value = source == null ? "" : source.trim().replace('\\', '/');
        if (value.startsWith("mc:")) {
            return value;
        }
        int slash = baseFile.lastIndexOf('/');
        String base = slash < 0 ? "" : baseFile.substring(0, slash + 1);
        java.nio.file.Path normalized = java.nio.file.Path.of(base).resolve(value).normalize();
        String result = normalized.toString().replace('\\', '/');
        if (result.startsWith("../") || result.equals("..") || result.contains(":")) {
            return "";
        }
        return result;
    }

    public WebBundle bundle() {
        return bundle;
    }

    public String pageId() {
        return pageId;
    }

    public DomElement root() {
        return root;
    }

    public CssStylesheet stylesheet() {
        return stylesheet;
    }

    public WebTranslations translations() {
        return translations;
    }

    public List<ScriptSource> scripts() {
        return scripts;
    }

    public record ScriptSource(String name, String source) {
    }

    private static final class Counter {
        private int value;
    }

    private static final class SetHolder {
        private static final java.util.Set<String> NON_VISUAL_TAGS = java.util.Set.of(
                "script", "style", "link", "meta", "title", "head");
        private static final java.util.Set<String> BOLD_TAGS = java.util.Set.of("strong", "b", "h1", "h2", "h3", "summary");
        private static final java.util.Set<String> ITALIC_TAGS = java.util.Set.of("em", "i");
    }
}
