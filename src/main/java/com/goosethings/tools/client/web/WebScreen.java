package com.goosethings.tools.client.web;

import com.goosethings.tools.client.ClientWebManager;
import com.goosethings.tools.client.web.dom.DomElement;
import com.goosethings.tools.client.web.dom.WebDocument;
import com.goosethings.tools.client.web.input.WebTextInputController;
import com.goosethings.tools.client.web.render.ClientWebAssets;
import com.goosethings.tools.client.web.render.LayoutEngine;
import com.goosethings.tools.client.web.render.WebRenderGeometry;
import com.goosethings.tools.client.web.script.JsSandbox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

/** Minecraft-native renderer for the supported server page subset. */
public final class WebScreen extends Screen {
    private static final int SCROLLBAR_GUTTER = 5;
    private static final int CONTROL_BACKGROUND = 0xFF1C2E3D;
    private static final int CONTROL_HOVER_BACKGROUND = 0xFF28465D;
    private static final int CLOSE_HOVER_BACKGROUND = 0xFF71333D;
    private static final int CONTROL_BORDER = 0xFF49677D;
    private static final int CONTROL_HOVER_BORDER = 0xFF7ECFFF;
    private static final int CLOSE_HOVER_BORDER = 0xFFEF7784;
    private static final int CONTROL_ICON = 0xFFEAF6FC;
    private static final int CONTROL_DISABLED_ICON = 0xFF617382;
    private static final int INPUT_FOCUS_BORDER = 0xFF7ECFFF;
    private static final int INPUT_CARET = 0xFFF1FAFF;
    private static final int INPUT_SELECTION = 0x88508BB2;
    private static final long CARET_BLINK_MILLIS = 500L;
    public static final float DEFAULT_CONTENT_SCALE = WebRenderGeometry.DEFAULT_CONTENT_SCALE;

    private final ClientWebManager manager;
    private final WebDocument document;
    private final LayoutEngine layoutEngine;
    private final JsSandbox sandbox;
    private final WebTextInputController inputController = new WebTextInputController();
    private LayoutEngine.LayoutResult layout = new LayoutEngine.LayoutResult(0, List.of());
    private double scroll;
    private int contentHeight;
    private float contentScale;
    private long caretResetMillis = System.currentTimeMillis();
    private boolean sandboxClosed;

    public WebScreen(ClientWebManager manager, WebDocument document) {
        this(manager, document, DEFAULT_CONTENT_SCALE);
    }

    public WebScreen(ClientWebManager manager, WebDocument document, float contentScale) {
        super(Component.translatable("screen.goosetools.web.title"));
        this.manager = manager;
        this.document = document;
        this.contentScale = WebRenderGeometry.clampScale(contentScale);
        this.layoutEngine = new LayoutEngine(document, minecraft.font);
        document.setMutationListener(() -> { });
        this.sandbox = new JsSandbox(document, manager::openLocalPage);
        this.sandbox.executeScripts();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF012171D);
        WebRenderGeometry.Rect frame = currentViewport();
        WebRenderGeometry.Rect viewport = currentContentViewport();
        WebRenderGeometry.Rect virtualViewport = currentVirtualViewport();
        WebRenderGeometry.Point pointer = WebRenderGeometry.toVirtual(
                viewport, contentScale, mouseX, mouseY);

        graphics.fill(frame.left(), frame.top(), frame.right(), frame.bottom(), 0xF80A1017);
        graphics.fill(
                frame.left() + 1,
                frame.top() + 1,
                frame.right() - 1,
                viewport.top(),
                0xFF172633);
        updateHover(document.root(), pointer.x(), pointer.y(), viewport.contains(mouseX, mouseY));
        clampScroll(virtualViewport);
        layout = layoutEngine.layout(
                virtualViewport.left(),
                virtualViewport.top() - (int) scroll,
                Math.max(40, virtualViewport.width() - scrollbarGutterVirtual()));
        contentHeight = layout.contentHeight();
        clampScroll(virtualViewport);

        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        graphics.pose().pushMatrix();
        graphics.pose().translate(viewport.left(), viewport.top());
        graphics.pose().scale(contentScale, contentScale);
        for (LayoutEngine.DrawItem item : layout.items()) {
            if (item instanceof LayoutEngine.ImageItem image
                    && !WebRenderGeometry.image(
                    image.x(), image.y(), image.width(), image.height()).intersects(virtualViewport)) {
                continue;
            }
            draw(graphics, item);
        }
        drawFocusedInput(graphics);
        graphics.pose().popMatrix();
        graphics.disableScissor();
        drawScrollbar(graphics, viewport, virtualViewport);
        drawFrameControls(graphics, frame, mouseX, mouseY);
        graphics.outline(frame.left(), frame.top(), frame.width(), frame.height(), 0xFF36536A);
    }

    private void draw(GuiGraphicsExtractor graphics, LayoutEngine.DrawItem item) {
        if (item instanceof LayoutEngine.RectItem rect) {
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), rect.color());
        } else if (item instanceof LayoutEngine.OutlineItem outline) {
            graphics.outline(outline.x(), outline.y(), outline.width(), outline.height(), outline.color());
        } else if (item instanceof LayoutEngine.TextItem text) {
            if (Math.abs(text.scale() - 1.0F) < 0.001F) {
                graphics.text(font, text.text(), text.x(), text.y(), text.color(), text.shadow());
            } else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(text.x(), text.y());
                graphics.pose().scale(text.scale(), text.scale());
                graphics.text(font, text.text(), 0, 0, text.color(), text.shadow());
                graphics.pose().popMatrix();
            }
        } else if (item instanceof LayoutEngine.ImageItem image) {
            ClientWebAssets.TextureRef texture = manager.assets().resolve(document, image.source());
            if (texture != null) {
                WebRenderGeometry.Rect bounds = WebRenderGeometry.image(
                        image.x(), image.y(), image.width(), image.height());
                graphics.blit(
                        texture.identifier(),
                        bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                        0.0F, 1.0F, 0.0F, 1.0F);
            }
        }
    }

    private void drawFocusedInput(GuiGraphicsExtractor graphics) {
        DomElement focused = inputController.focused();
        LayoutEngine.InputItem input = findInputItem(focused);
        if (input == null) {
            return;
        }
        int boxX = focused.x();
        int boxY = focused.y();
        int boxWidth = focused.width();
        int boxHeight = focused.height();
        graphics.outline(boxX, boxY, boxWidth, boxHeight, INPUT_FOCUS_BORDER);
        if (boxWidth > 2 && boxHeight > 2) {
            graphics.outline(boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2, INPUT_FOCUS_BORDER);
        }

        String value = focused.value();
        if (inputController.hasSelection()) {
            int selectionLeft = input.textX() + font.width(value.substring(0, inputController.selectionStart()));
            int selectionRight = input.textX() + font.width(value.substring(0, inputController.selectionEnd()));
            graphics.fill(
                    Math.min(selectionLeft, selectionRight),
                    input.textY() - 1,
                    Math.max(selectionLeft + 1, selectionRight),
                    input.textY() + font.lineHeight + 1,
                    INPUT_SELECTION);
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - caretResetMillis);
        if (elapsed < CARET_BLINK_MILLIS || (elapsed / CARET_BLINK_MILLIS) % 2L == 0L) {
            int caretX = input.textX() + font.width(value.substring(0, inputController.cursor()));
            int maxCaretX = input.textX() + Math.max(0, input.contentWidth() - 2);
            caretX = Math.clamp(caretX, input.textX(), maxCaretX);
            graphics.fill(
                    caretX,
                    input.textY() - 1,
                    caretX + 2,
                    input.textY() + font.lineHeight + 1,
                    INPUT_CARET);
        }
    }

    private void drawScrollbar(
            GuiGraphicsExtractor graphics,
            WebRenderGeometry.Rect viewport,
            WebRenderGeometry.Rect virtualViewport) {
        int viewportHeight = Math.max(1, viewport.height());
        int virtualHeight = Math.max(1, virtualViewport.height());
        if (contentHeight <= virtualHeight) {
            return;
        }
        int trackX = viewport.right() - 4;
        int thumbHeight = Math.max(18, viewportHeight * virtualHeight / contentHeight);
        int maxScroll = Math.max(1, contentHeight - virtualHeight);
        int thumbY = viewport.top() + (int) ((viewportHeight - thumbHeight) * (scroll / maxScroll));
        graphics.fill(trackX, viewport.top(), viewport.right() - 1, viewport.bottom(), 0x55273542);
        graphics.fill(trackX, thumbY, viewport.right() - 1, thumbY + thumbHeight, 0xCC6DA8CE);
    }

    private void drawFrameControls(
            GuiGraphicsExtractor graphics,
            WebRenderGeometry.Rect frame,
            int mouseX,
            int mouseY) {
        WebRenderGeometry.Controls controls = WebRenderGeometry.controls(frame);
        boolean canZoomOut = contentScale > WebRenderGeometry.MIN_CONTENT_SCALE;
        boolean canZoomIn = contentScale < WebRenderGeometry.MAX_CONTENT_SCALE;
        boolean zoomOutHovered = controls.zoomOut().contains(mouseX, mouseY);
        boolean zoomInHovered = controls.zoomIn().contains(mouseX, mouseY);
        boolean closeHovered = controls.close().contains(mouseX, mouseY);

        drawControlBackground(graphics, controls.zoomOut(), zoomOutHovered, false);
        drawMagnifierIcon(graphics, controls.zoomOut(), false, canZoomOut);
        drawControlBackground(graphics, controls.zoomIn(), zoomInHovered, false);
        drawMagnifierIcon(graphics, controls.zoomIn(), true, canZoomIn);
        drawControlBackground(graphics, controls.close(), closeHovered, true);
        drawCloseIcon(graphics, controls.close());

        FormattedCharSequence zoomText = Component.literal(
                Math.round(contentScale * 100.0F) + "%").getVisualOrderText();
        int labelY = frame.top() + Math.max(1,
                (WebRenderGeometry.TITLE_BAR_HEIGHT - font.lineHeight) / 2);
        int zoomTextX = controls.zoomOut().left() - 4 - font.width(zoomText);
        graphics.text(font, zoomText, zoomTextX, labelY, 0xFFAFC5D4, false);

        Component tooltip = null;
        if (zoomOutHovered) {
            tooltip = Component.translatable("screen.goosetools.web.zoom_out");
        } else if (zoomInHovered) {
            tooltip = Component.translatable("screen.goosetools.web.zoom_in");
        } else if (closeHovered) {
            tooltip = Component.translatable("screen.goosetools.web.close");
        }
        if (tooltip != null) {
            FormattedCharSequence text = tooltip.getVisualOrderText();
            int textRight = zoomTextX - 5;
            int textX = Math.max(frame.left() + 5, textRight - font.width(text));
            graphics.text(font, text, textX, labelY, 0xFFEAF6FC, false);
        }
    }

    private static void drawControlBackground(
            GuiGraphicsExtractor graphics,
            WebRenderGeometry.Rect bounds,
            boolean hovered,
            boolean close) {
        int background = hovered
                ? close ? CLOSE_HOVER_BACKGROUND : CONTROL_HOVER_BACKGROUND
                : CONTROL_BACKGROUND;
        int border = hovered
                ? close ? CLOSE_HOVER_BORDER : CONTROL_HOVER_BORDER
                : CONTROL_BORDER;
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), background);
        graphics.outline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), border);
    }

    private static void drawMagnifierIcon(
            GuiGraphicsExtractor graphics,
            WebRenderGeometry.Rect bounds,
            boolean plus,
            boolean enabled) {
        int color = enabled ? CONTROL_ICON : CONTROL_DISABLED_ICON;
        int left = bounds.left() + 3;
        int top = bounds.top() + 3;
        graphics.fill(left + 1, top, left + 6, top + 1, color);
        graphics.fill(left, top + 1, left + 1, top + 6, color);
        graphics.fill(left + 6, top + 1, left + 7, top + 6, color);
        graphics.fill(left + 1, top + 6, left + 6, top + 7, color);
        graphics.fill(left + 6, top + 6, left + 8, top + 8, color);
        graphics.fill(left + 8, top + 8, left + 10, top + 9, color);
        graphics.fill(left + 2, top + 3, left + 5, top + 4, color);
        if (plus) {
            graphics.fill(left + 3, top + 2, left + 4, top + 5, color);
        }
    }

    private static void drawCloseIcon(
            GuiGraphicsExtractor graphics,
            WebRenderGeometry.Rect bounds) {
        int left = bounds.left() + 5;
        int top = bounds.top() + 5;
        for (int index = 0; index < 6; index++) {
            graphics.fill(left + index, top + index, left + index + 1, top + index + 1, CONTROL_ICON);
            graphics.fill(left + 5 - index, top + index, left + 6 - index, top + index + 1, CONTROL_ICON);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        WebRenderGeometry.Rect frame = currentViewport();
        WebRenderGeometry.Controls controls = WebRenderGeometry.controls(frame);
        if (controls.close().contains(event.x(), event.y())) {
            onClose();
            return true;
        }
        if (controls.zoomOut().contains(event.x(), event.y())) {
            adjustContentScale(-WebRenderGeometry.CONTENT_SCALE_STEP);
            return true;
        }
        if (controls.zoomIn().contains(event.x(), event.y())) {
            adjustContentScale(WebRenderGeometry.CONTENT_SCALE_STEP);
            return true;
        }
        WebRenderGeometry.Rect viewport = currentContentViewport();
        if (!viewport.contains(event.x(), event.y())) {
            blurInput();
            return frame.contains(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
        }
        WebRenderGeometry.Point pointer = WebRenderGeometry.toVirtual(
                viewport, contentScale, event.x(), event.y());
        DomElement target = findInteractive(document.root(), pointer.x(), pointer.y());
        if (target != null && "input".equals(target.tag())) {
            focusInput(target, caretAt(target, pointer.x()));
        } else {
            blurInput();
        }
        if (target == null) {
            return true;
        }

        if ("summary".equals(target.tag()) && target.parent() != null
                && "details".equals(target.parent().tag())) {
            DomElement details = target.parent();
            if (details.hasAttribute("open")) {
                details.removeAttribute("open");
            } else {
                details.setAttribute("open", "open");
            }
        }
        String href = target.attribute("href");
        if (href.startsWith("goose:")) {
            manager.openLocalPage(href.substring("goose:".length()));
        } else if (href.startsWith("#")) {
            DomElement anchor = document.getElementById(href.substring(1));
            if (anchor != null) {
                scroll = Math.max(0, anchor.y() + scroll);
            }
        } else {
            OfficialWebLinks.resolve(href)
                    .ifPresent(uri -> ConfirmLinkScreen.confirmLinkNow(this, uri));
        }
        if (target.hasAttribute("data-page")) {
            manager.openLocalPage(target.attribute("data-page"));
        }
        if (target.hasAttribute("data-language")) {
            document.setLanguage(target.attribute("data-language"));
            manager.setPreferredLanguage(target.attribute("data-language"));
        }
        if ("toggle".equals(target.attribute("data-theme"))) {
            if (manager.toggleDarkTheme()) {
                document.root().addClass("theme-dark");
            } else {
                document.root().removeClass("theme-dark");
            }
            document.mutated();
        }
        if ("close".equals(target.attribute("data-action"))) {
            onClose();
        }
        sandbox.fire(target, "click");
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        WebRenderGeometry.Rect viewport = currentContentViewport();
        if (!viewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        scroll -= vertical * 28.0D / contentScale;
        clampScroll(currentVirtualViewport());
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (inputController.focused() == null || !event.isAllowedChatCharacter()) {
            return super.charTyped(event);
        }
        if (inputController.insert(event.codepointAsString(), inputLimit(inputController.focused()))) {
            fireInputEvent();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        DomElement focused = inputController.focused();
        if (focused == null) {
            return super.keyPressed(event);
        }
        if (event.isSelectAll()) {
            inputController.selectAll();
            resetCaretBlink();
            return true;
        }
        if (event.isCopy()) {
            minecraft.keyboardHandler.setClipboard(inputController.selectedText());
            return true;
        }
        if (event.isCut()) {
            minecraft.keyboardHandler.setClipboard(inputController.selectedText());
            if (inputController.deleteSelection()) {
                fireInputEvent();
            }
            return true;
        }
        if (event.isPaste()) {
            if (inputController.insert(minecraft.keyboardHandler.getClipboard(), inputLimit(focused))) {
                fireInputEvent();
            }
            return true;
        }
        switch (event.key()) {
            case InputConstants.KEY_BACKSPACE -> {
                if (inputController.backspace()) {
                    fireInputEvent();
                }
                return true;
            }
            case InputConstants.KEY_DELETE -> {
                if (inputController.delete()) {
                    fireInputEvent();
                }
                return true;
            }
            case InputConstants.KEY_LEFT -> {
                inputController.moveLeft(event.hasShiftDown());
                resetCaretBlink();
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                inputController.moveRight(event.hasShiftDown());
                resetCaretBlink();
                return true;
            }
            case InputConstants.KEY_HOME -> {
                inputController.moveHome(event.hasShiftDown());
                resetCaretBlink();
                return true;
            }
            case InputConstants.KEY_END -> {
                inputController.moveEnd(event.hasShiftDown());
                resetCaretBlink();
                return true;
            }
            case InputConstants.KEY_RETURN -> {
                sandbox.fire(focused, "change");
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    public void onClose() {
        closeSandbox();
        super.onClose();
    }

    @Override
    public void removed() {
        closeSandbox();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void closeSandbox() {
        if (!sandboxClosed) {
            sandboxClosed = true;
            sandbox.close();
        }
    }

    private WebRenderGeometry.Rect currentViewport() {
        return WebRenderGeometry.viewport(width, height);
    }

    private WebRenderGeometry.Rect currentContentViewport() {
        return WebRenderGeometry.contentViewport(currentViewport());
    }

    private WebRenderGeometry.Rect currentVirtualViewport() {
        return WebRenderGeometry.virtualViewport(currentContentViewport(), contentScale);
    }

    private int scrollbarGutterVirtual() {
        return Math.max(1, (int) Math.ceil(SCROLLBAR_GUTTER / contentScale));
    }

    private void adjustContentScale(float difference) {
        contentScale = WebRenderGeometry.clampScale(contentScale + difference);
        clampScroll(currentVirtualViewport());
    }

    public float contentScale() {
        return contentScale;
    }

    private void clampScroll(WebRenderGeometry.Rect virtualViewport) {
        scroll = Math.clamp(scroll, 0.0D, Math.max(0.0D, contentHeight - virtualViewport.height()));
    }

    private LayoutEngine.InputItem findInputItem(DomElement element) {
        if (element == null) {
            return null;
        }
        for (LayoutEngine.InputItem input : layout.inputs()) {
            if (input.element() == element) {
                return input;
            }
        }
        return null;
    }

    private int caretAt(DomElement input, double mouseX) {
        LayoutEngine.InputItem metrics = findInputItem(input);
        String value = input.value();
        if (metrics == null || value.isEmpty() || mouseX <= metrics.textX()) {
            return 0;
        }
        double relativeX = mouseX - metrics.textX();
        int previous = 0;
        int previousWidth = 0;
        while (previous < value.length()) {
            int next = value.offsetByCodePoints(previous, 1);
            int nextWidth = font.width(value.substring(0, next));
            if (relativeX < (previousWidth + nextWidth) / 2.0D) {
                return previous;
            }
            previous = next;
            previousWidth = nextWidth;
        }
        return value.length();
    }

    private void focusInput(DomElement input, int caret) {
        DomElement previous = inputController.focused();
        if (previous != null && previous != input) {
            sandbox.fire(previous, "change");
            sandbox.fire(previous, "blur");
        }
        boolean changed = inputController.focus(input, caret);
        if (changed) {
            sandbox.fire(input, "focus");
        }
        resetCaretBlink();
    }

    private void blurInput() {
        DomElement previous = inputController.blur();
        if (previous != null) {
            sandbox.fire(previous, "change");
            sandbox.fire(previous, "blur");
        }
    }

    private void fireInputEvent() {
        DomElement focused = inputController.focused();
        if (focused != null) {
            resetCaretBlink();
            sandbox.fire(focused, "input");
        }
    }

    private void resetCaretBlink() {
        caretResetMillis = System.currentTimeMillis();
    }

    private static int inputLimit(DomElement input) {
        int limit = 256;
        try {
            String maxLength = input.attribute("maxlength");
            if (!maxLength.isBlank()) {
                limit = Math.clamp(Integer.parseInt(maxLength), 1, 2048);
            }
        } catch (NumberFormatException ignored) {
        }
        return limit;
    }

    private static void updateHover(
            DomElement element,
            double mouseX,
            double mouseY,
            boolean pointerInsideViewport) {
        element.setHovered(pointerInsideViewport && element.contains(mouseX, mouseY));
        element.children().forEach(child -> updateHover(
                child, mouseX, mouseY, pointerInsideViewport));
    }

    private static DomElement findInteractive(DomElement element, double mouseX, double mouseY) {
        List<DomElement> children = new ArrayList<>(element.children());
        for (int index = children.size() - 1; index >= 0; index--) {
            DomElement found = findInteractive(children.get(index), mouseX, mouseY);
            if (found != null) {
                return found;
            }
        }
        if (!element.contains(mouseX, mouseY)) {
            return null;
        }
        return switch (element.tag()) {
            case "button", "a", "input", "summary" -> element;
            default -> element.hasEventHandler("click")
                    || element.hasAttribute("data-page")
                    || element.hasAttribute("data-language")
                    || element.hasAttribute("data-action") ? element : null;
        };
    }
}
