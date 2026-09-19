package com.goosethings.tools.client.web.input;

import com.goosethings.tools.client.web.dom.DomElement;

/** Selection-aware editor state for the native DOM input renderer. */
public final class WebTextInputController {
    private DomElement focused;
    private int cursor;
    private int anchor;

    public DomElement focused() {
        return focused;
    }

    public boolean focus(DomElement input, int position) {
        if (input == null || !"input".equals(input.tag())) {
            throw new IllegalArgumentException("Only input elements can receive text focus");
        }
        boolean changed = focused != input;
        focused = input;
        cursor = clampToBoundary(input.value(), position);
        anchor = cursor;
        return changed;
    }

    public DomElement blur() {
        DomElement previous = focused;
        focused = null;
        cursor = 0;
        anchor = 0;
        return previous;
    }

    public int cursor() {
        normalizeSelection();
        return cursor;
    }

    public int selectionStart() {
        normalizeSelection();
        return Math.min(cursor, anchor);
    }

    public int selectionEnd() {
        normalizeSelection();
        return Math.max(cursor, anchor);
    }

    public boolean hasSelection() {
        return focused != null && selectionStart() != selectionEnd();
    }

    public String selectedText() {
        if (!hasSelection()) {
            return "";
        }
        return focused.value().substring(selectionStart(), selectionEnd());
    }

    public void setCursor(int position, boolean selecting) {
        if (focused == null) {
            return;
        }
        cursor = clampToBoundary(focused.value(), position);
        if (!selecting) {
            anchor = cursor;
        }
    }

    public void selectAll() {
        if (focused == null) {
            return;
        }
        anchor = 0;
        cursor = focused.value().length();
    }

    public void moveLeft(boolean selecting) {
        if (focused == null) {
            return;
        }
        normalizeSelection();
        if (!selecting && hasSelection()) {
            setCursor(selectionStart(), false);
            return;
        }
        if (cursor > 0) {
            setCursor(focused.value().offsetByCodePoints(cursor, -1), selecting);
        } else if (!selecting) {
            anchor = cursor;
        }
    }

    public void moveRight(boolean selecting) {
        if (focused == null) {
            return;
        }
        normalizeSelection();
        if (!selecting && hasSelection()) {
            setCursor(selectionEnd(), false);
            return;
        }
        if (cursor < focused.value().length()) {
            setCursor(focused.value().offsetByCodePoints(cursor, 1), selecting);
        } else if (!selecting) {
            anchor = cursor;
        }
    }

    public void moveHome(boolean selecting) {
        setCursor(0, selecting);
    }

    public void moveEnd(boolean selecting) {
        if (focused != null) {
            setCursor(focused.value().length(), selecting);
        }
    }

    public boolean insert(String text, int maxCodePoints) {
        if (focused == null || text == null) {
            return false;
        }
        String filtered = filterInput(text);
        if (filtered.isEmpty()) {
            return false;
        }
        normalizeSelection();
        String value = focused.value();
        int start = selectionStart();
        int end = selectionEnd();
        String retained = value.substring(0, start) + value.substring(end);
        int room = Math.max(0, maxCodePoints - retained.codePointCount(0, retained.length()));
        String insertion = firstCodePoints(filtered, room);
        if (insertion.isEmpty()) {
            return false;
        }
        String updated = value.substring(0, start) + insertion + value.substring(end);
        focused.setValue(updated);
        cursor = start + insertion.length();
        anchor = cursor;
        return true;
    }

    public boolean backspace() {
        if (focused == null) {
            return false;
        }
        normalizeSelection();
        if (hasSelection()) {
            return deleteSelection();
        }
        if (cursor == 0) {
            return false;
        }
        int start = focused.value().offsetByCodePoints(cursor, -1);
        focused.setValue(focused.value().substring(0, start) + focused.value().substring(cursor));
        cursor = start;
        anchor = cursor;
        return true;
    }

    public boolean delete() {
        if (focused == null) {
            return false;
        }
        normalizeSelection();
        if (hasSelection()) {
            return deleteSelection();
        }
        String value = focused.value();
        if (cursor >= value.length()) {
            return false;
        }
        int end = value.offsetByCodePoints(cursor, 1);
        focused.setValue(value.substring(0, cursor) + value.substring(end));
        anchor = cursor;
        return true;
    }

    public boolean deleteSelection() {
        if (focused == null || !hasSelection()) {
            return false;
        }
        int start = selectionStart();
        int end = selectionEnd();
        focused.setValue(focused.value().substring(0, start) + focused.value().substring(end));
        cursor = start;
        anchor = cursor;
        return true;
    }

    private void normalizeSelection() {
        if (focused == null) {
            cursor = 0;
            anchor = 0;
            return;
        }
        cursor = clampToBoundary(focused.value(), cursor);
        anchor = clampToBoundary(focused.value(), anchor);
    }

    private static int clampToBoundary(String value, int position) {
        int clamped = Math.clamp(position, 0, value.length());
        if (clamped > 0 && clamped < value.length()
                && Character.isLowSurrogate(value.charAt(clamped))
                && Character.isHighSurrogate(value.charAt(clamped - 1))) {
            return clamped - 1;
        }
        return clamped;
    }

    private static String filterInput(String text) {
        StringBuilder filtered = new StringBuilder(text.length());
        text.codePoints()
                .filter(codePoint -> codePoint >= 32 && codePoint != 127 && codePoint != 167)
                .forEach(filtered::appendCodePoint);
        return filtered.toString();
    }

    private static String firstCodePoints(String value, int count) {
        if (count <= 0) {
            return "";
        }
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= count) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, count));
    }
}
