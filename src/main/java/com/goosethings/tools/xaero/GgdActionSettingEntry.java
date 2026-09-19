package com.goosethings.tools.xaero;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.ISettingEntry;

import java.util.function.Supplier;

/** A small dynamic button entry for the restricted Xaero settings screen. */
public final class GgdActionSettingEntry implements ISettingEntry {
    private final Supplier<Component> messageSupplier;
    private final Runnable action;

    public GgdActionSettingEntry(Supplier<Component> messageSupplier, Runnable action) {
        this.messageSupplier = messageSupplier;
        this.action = action;
    }

    @Override
    public String getStringForSearch() {
        return messageSupplier.get().getString();
    }

    @Override
    public AbstractWidget createWidget(int x, int y, int width) {
        return Button.builder(messageSupplier.get(), button -> {
                    action.run();
                    button.setMessage(messageSupplier.get());
                })
                .bounds(x, y, width, 20)
                .build();
    }
}
