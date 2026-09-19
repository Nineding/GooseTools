package com.goosethings.tools.client.web.input;

import com.goosethings.tools.client.web.dom.DomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WebTextInputControllerTest {
    @Test
    void selectionTypingAndClipboardTextEditTheFocusedInput() {
        DomElement input = new DomElement("input");
        input.setValue("Goose");
        WebTextInputController controller = new WebTextInputController();

        assertTrue(controller.focus(input, input.value().length()));
        controller.moveLeft(true);
        controller.moveLeft(true);
        assertEquals("se", controller.selectedText());
        assertTrue(controller.insert("鸭", 5));
        assertEquals("Goo鸭", input.value());
        assertEquals(input.value().length(), controller.cursor());

        controller.selectAll();
        assertTrue(controller.insert("渡鸦\n§x", 2));
        assertEquals("渡鸦", input.value());
        assertFalse(controller.hasSelection());
    }

    @Test
    void backspaceAndDeleteRespectUnicodeCodePointBoundaries() {
        DomElement input = new DomElement("input");
        input.setValue("A\uD83E\uDEBFB");
        WebTextInputController controller = new WebTextInputController();
        controller.focus(input, 3);

        assertTrue(controller.backspace());
        assertEquals("AB", input.value());
        controller.moveHome(false);
        assertTrue(controller.delete());
        assertEquals("B", input.value());
        assertTrue(controller.delete());
        assertEquals("", input.value());
        assertFalse(controller.delete());
    }
}
