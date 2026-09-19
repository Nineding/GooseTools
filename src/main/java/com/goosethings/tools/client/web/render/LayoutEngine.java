package com.goosethings.tools.client.web.render;

import com.goosethings.tools.client.web.dom.DomElement;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.goosethings.tools.client.web.style.ComputedStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Block layout plus a compact row/wrapping flex subset. */
public final class LayoutEngine {
    private final WebDocument document;
    private final Font font;
    private final List<DrawItem> drawItems = new ArrayList<>();
    private final List<InputItem> inputItems = new ArrayList<>();

    public LayoutEngine(WebDocument document, Font font) {
        this.document = document;
        this.font = font;
    }

    public LayoutResult layout(int x, int y, int width) {
        drawItems.clear();
        inputItems.clear();
        int height = layoutElement(document.root(), x, y, Math.max(40, width), true);
        return new LayoutResult(height, List.copyOf(drawItems), List.copyOf(inputItems));
    }

    private int layoutElement(DomElement element, int availableX, int availableY, int availableWidth, boolean honorWidth) {
        if (element.isTextNode()) {
            element.setBounds(0, 0, 0, 0);
            return 0;
        }
        ComputedStyle style = document.stylesheet().compute(element);
        if (element.hidden() || style.is("display", "none")) {
            zeroBounds(element);
            return 0;
        }

        Insets margin = Insets.parse(style.get("margin", "0"), availableWidth)
                .override(style, "margin", availableWidth);
        Insets padding = Insets.parse(style.get("padding", "0"), availableWidth)
                .override(style, "padding", availableWidth);
        int border = Math.max(0, style.pixels("border-width", 0, availableWidth));
        int naturalWidth = Math.max(1, availableWidth - margin.left() - margin.right());
        int boxWidth = honorWidth && !style.get("width", "").isBlank()
                ? Math.clamp(style.pixels("width", naturalWidth, availableWidth), 1, naturalWidth)
                : naturalWidth;
        int boxX = availableX + margin.left();
        int boxY = availableY + margin.top();
        int contentX = boxX + border + padding.left();
        int contentWidth = Math.max(1, boxWidth - border * 2 - padding.left() - padding.right());
        int contentY = boxY + border + padding.top();
        int cursorY = contentY;
        int ownContentHeight = 0;

        int drawInsert = drawItems.size();
        if ("img".equals(element.tag())) {
            int imageWidth = Math.clamp(style.pixels("width", 32, contentWidth), 1, contentWidth);
            int imageHeight = Math.max(1, style.pixels("height", 32, contentWidth));
            drawItems.add(new ImageItem(element.attribute("src"), contentX, contentY, imageWidth, imageHeight));
            ownContentHeight = imageHeight;
        } else if ("hr".equals(element.tag())) {
            ownContentHeight = Math.max(1, style.pixels("height", 1, contentWidth));
        } else if ("input".equals(element.tag())) {
            int fixedHeight = Math.max(16, style.pixels("height", 22, contentWidth));
            String displayed = element.value();
            int textColor = style.color("color", 0xFFE8EDF2);
            if (displayed.isEmpty()) {
                String placeholderKey = element.attribute("data-placeholder-i18n");
                displayed = placeholderKey.isBlank()
                        ? element.attribute("placeholder")
                        : document.translations().translate(placeholderKey);
                textColor = 0xFF8292A0;
            }
            drawItems.add(new TextItem(
                    FormattedCharSequence.forward(displayed, net.minecraft.network.chat.Style.EMPTY),
                    contentX,
                    contentY + Math.max(0, (fixedHeight - padding.top() - padding.bottom() - font.lineHeight) / 2),
                    textColor,
                    1.0F,
                    false));
            inputItems.add(new InputItem(
                    element,
                    contentX,
                    contentY + Math.max(0, (fixedHeight - padding.top() - padding.bottom() - font.lineHeight) / 2),
                    contentWidth));
            ownContentHeight = Math.max(1, fixedHeight - padding.top() - padding.bottom() - border * 2);
        } else if (element.isInlineTextContainer()) {
            ownContentHeight = layoutText(element, style, contentX, contentY, contentWidth);
        } else if (style.is("display", "flex") && style.is("flex-direction", "row")) {
            ownContentHeight = layoutFlexRow(element, style, contentX, contentY, contentWidth);
        } else {
            boolean closedDetails = "details".equals(element.tag()) && !element.hasAttribute("open");
            for (DomElement child : element.children()) {
                if (closedDetails && !"summary".equals(child.tag())) {
                    zeroBounds(child);
                    continue;
                }
                int childHeight = layoutElement(child, contentX, cursorY, contentWidth, true);
                cursorY += childHeight;
                ownContentHeight += childHeight;
            }
        }

        int requestedHeight = style.pixels("height", 0, contentWidth);
        int boxHeight = Math.max(
                requestedHeight,
                border * 2 + padding.top() + Math.max(ownContentHeight, cursorY - contentY) + padding.bottom());
        if (boxHeight == 0) {
            boxHeight = 1;
        }
        element.setBounds(boxX, boxY, boxWidth, boxHeight);

        int background = style.color("background-color", 0x00000000);
        if ((background >>> 24) != 0) {
            drawItems.add(drawInsert, new RectItem(boxX, boxY, boxWidth, boxHeight, background));
            drawInsert++;
        }
        int borderColor = style.color("border-color", 0x00000000);
        if (border > 0 && (borderColor >>> 24) != 0) {
            for (int i = 0; i < border; i++) {
                drawItems.add(drawInsert++, new OutlineItem(
                        boxX + i,
                        boxY + i,
                        Math.max(1, boxWidth - i * 2),
                        Math.max(1, boxHeight - i * 2),
                        borderColor));
            }
        }
        return margin.top() + boxHeight + margin.bottom();
    }

    private int layoutText(
            DomElement element,
            ComputedStyle style,
            int x,
            int y,
            int width) {
        MutableComponent component = document.componentFor(element);
        if (style.is("font-weight", "bold")) {
            component.withStyle(ChatFormatting.BOLD);
        }
        if (style.is("font-style", "italic")) {
            component.withStyle(ChatFormatting.ITALIC);
        }
        if ("li".equals(element.tag())) {
            component = net.minecraft.network.chat.Component.literal("• ").append(component);
        }
        float scale = style.fontScale();
        int wrapWidth = Math.max(1, (int) (width / scale));
        List<FormattedCharSequence> lines = style.is("white-space", "nowrap")
                ? List.of(component.getVisualOrderText())
                : font.split(component, wrapWidth);
        if (lines.isEmpty()) {
            return 0;
        }
        int lineGap = Math.max(0, style.pixels("line-height", 2, width));
        int lineHeight = Math.max(1, Math.round((font.lineHeight + lineGap) * scale));
        int color = style.color("color", 0xFFE8EDF2);
        boolean shadow = !style.is("text-shadow", "none");
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int drawX = x;
            int renderedWidth = Math.round(font.width(line) * scale);
            if (style.is("text-align", "center")) {
                drawX += Math.max(0, (width - renderedWidth) / 2);
            } else if (style.is("text-align", "right")) {
                drawX += Math.max(0, width - renderedWidth);
            }
            drawItems.add(new TextItem(line, drawX, y + index * lineHeight, color, scale, shadow));
        }
        return lines.size() * lineHeight;
    }

    private int layoutFlexRow(
            DomElement parent,
            ComputedStyle style,
            int x,
            int y,
            int width) {
        List<DomElement> visible = parent.children().stream()
                .filter(child -> !child.hidden())
                .toList();
        if (visible.isEmpty()) {
            return 0;
        }
        int gap = Math.max(0, style.pixels("gap", 4, width));
        boolean wrap = style.is("flex-wrap", "wrap");
        int cursorX = x;
        int cursorY = y;
        int rowHeight = 0;
        int usedHeight = 0;
        int defaultWidth = Math.max(1, (width - gap * (visible.size() - 1)) / visible.size());

        for (DomElement child : visible) {
            ComputedStyle childStyle = document.stylesheet().compute(child);
            int childWidth = childStyle.get("width", "").isBlank()
                    ? defaultWidth
                    : Math.clamp(childStyle.pixels("width", defaultWidth, width), 1, width);
            if (wrap && cursorX > x && cursorX + childWidth > x + width) {
                cursorY += rowHeight + gap;
                usedHeight += rowHeight + gap;
                cursorX = x;
                rowHeight = 0;
            }
            int childHeight = layoutElement(child, cursorX, cursorY, childWidth, false);
            cursorX += childWidth + gap;
            rowHeight = Math.max(rowHeight, childHeight);
        }
        return usedHeight + rowHeight;
    }

    private static void zeroBounds(DomElement element) {
        element.setBounds(0, 0, 0, 0);
        element.children().forEach(LayoutEngine::zeroBounds);
    }

    public record LayoutResult(int contentHeight, List<DrawItem> items, List<InputItem> inputs) {
        public LayoutResult(int contentHeight, List<DrawItem> items) {
            this(contentHeight, items, List.of());
        }
    }

    public record InputItem(DomElement element, int textX, int textY, int contentWidth) {
    }

    public sealed interface DrawItem permits RectItem, OutlineItem, TextItem, ImageItem {
    }

    public record RectItem(int x, int y, int width, int height, int color) implements DrawItem {
    }

    public record OutlineItem(int x, int y, int width, int height, int color) implements DrawItem {
    }

    public record TextItem(
            FormattedCharSequence text,
            int x,
            int y,
            int color,
            float scale,
            boolean shadow) implements DrawItem {
    }

    public record ImageItem(String source, int x, int y, int width, int height) implements DrawItem {
    }

    private record Insets(int top, int right, int bottom, int left) {
        private static Insets parse(String value, int relativeTo) {
            String[] parts = value == null || value.isBlank() ? new String[]{"0"} : value.trim().split("\\s+");
            int top = ComputedStyle.parseLength(parts[0], 0, relativeTo);
            int right = parts.length > 1 ? ComputedStyle.parseLength(parts[1], 0, relativeTo) : top;
            int bottom = parts.length > 2 ? ComputedStyle.parseLength(parts[2], 0, relativeTo) : top;
            int left = parts.length > 3 ? ComputedStyle.parseLength(parts[3], 0, relativeTo) : right;
            return new Insets(top, right, bottom, left);
        }

        private Insets override(ComputedStyle style, String prefix, int relativeTo) {
            return new Insets(
                    style.pixels(prefix + "-top", top, relativeTo),
                    style.pixels(prefix + "-right", right, relativeTo),
                    style.pixels(prefix + "-bottom", bottom, relativeTo),
                    style.pixels(prefix + "-left", left, relativeTo));
        }
    }
}
