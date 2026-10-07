package com.goosethings.tools.client.vision;

import com.goosethings.tools.client.shader.IrisCompat;
import com.goosethings.tools.client.shader.RecommendedShaderManager;
import com.goosethings.tools.xaero.GgdClientPreferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native client settings opened from the server's function panel. */
public final class BlackoutSettingsScreen extends Screen {
    private final Screen parent;

    public BlackoutSettingsScreen(Screen parent) {
        super(Component.translatableWithFallback(
                "screen.goosetools.blackout.title", "Blackout visibility assistance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 130;
        int top = height / 2 - 48;
        addRenderableWidget(Button.builder(wallLabel(), button -> {
            GgdClientPreferences.setBlackoutWallGuide(!GgdClientPreferences.blackoutWallGuide());
            button.setMessage(wallLabel());
        }).bounds(left, top, 260, 20).build());
        addRenderableWidget(Button.builder(promptLabel(), button -> {
            GgdClientPreferences.setBlackoutAssistPrompts(!GgdClientPreferences.blackoutAssistPrompts());
            button.setMessage(promptLabel());
        }).bounds(left, top + 24, 260, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.goosetools.blackout.recommended_shader", "Set up recommended shader"),
                button -> RecommendedShaderManager.requestSetup(this))
                .bounds(left, top + 48, 260, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "screen.goosetools.update.settings", "Update settings"),
                button -> Minecraft.getInstance().setScreenAndShow(
                        new com.goosethings.tools.client.update.UpdateSettingsScreen(this)))
                .bounds(left, top + 72, 260, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "gui.done", "Done"), button -> onClose())
                .bounds(left, top + 106, 260, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE610151B);
        graphics.text(font, title, width / 2 - font.width(title) / 2,
                height / 2 - 90, 0xFFF0F8FF, false);
        Component recommendation = Component.translatableWithFallback(
                "screen.goosetools.blackout.description",
                "Shaders are recommended for better brightness and visuals. Wall highlighting is extra collision help but can look abrupt.");
        graphics.text(font, recommendation, width / 2 - font.width(recommendation) / 2,
                height / 2 - 70, 0xFFB9CDD8, false);
        Component status = IrisCompat.isShaderPackActive()
                ? Component.translatableWithFallback("screen.goosetools.blackout.shader_active", "Shader: active")
                : Component.translatableWithFallback("screen.goosetools.blackout.shader_inactive", "Shader: inactive");
        graphics.text(font, status, width / 2 - font.width(status) / 2,
                height / 2 + 92, IrisCompat.isShaderPackActive() ? 0xFF7FE69A : 0xFFFFC477, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component wallLabel() {
        return Component.translatableWithFallback(
                GgdClientPreferences.blackoutWallGuide()
                        ? "screen.goosetools.blackout.wall_guide_on"
                        : "screen.goosetools.blackout.wall_guide_off",
                GgdClientPreferences.blackoutWallGuide()
                        ? "Wall highlighting: On" : "Wall highlighting: Off");
    }

    private static Component promptLabel() {
        return Component.translatableWithFallback(
                GgdClientPreferences.blackoutAssistPrompts()
                        ? "screen.goosetools.blackout.prompts_on"
                        : "screen.goosetools.blackout.prompts_off",
                GgdClientPreferences.blackoutAssistPrompts()
                        ? "Blackout recommendation: On" : "Blackout recommendation: Permanently off");
    }
}
