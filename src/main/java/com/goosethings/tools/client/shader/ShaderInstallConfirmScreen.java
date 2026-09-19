package com.goosethings.tools.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

/** Explicit opt-in screen: resolving metadata is automatic; file download starts only after Yes. */
public final class ShaderInstallConfirmScreen extends Screen {
    private final Screen parent;
    private final RestartInstaller.Launch launch;
    private volatile ModrinthClient.Plan plan;
    private volatile String error;
    private volatile String status;
    private Button yes;

    public ShaderInstallConfirmScreen(Screen parent, RestartInstaller.Launch launch) {
        super(Component.translatableWithFallback(
                "screen.goosetools.shader_install.title", "Install recommended shader setup?"));
        this.parent = parent;
        this.launch = launch;
        this.status = Component.translatableWithFallback(
                "screen.goosetools.shader_install.resolving", "Checking stable Modrinth releases...").getString();
    }

    @Override
    protected void init() {
        int y = height / 2 + 62;
        yes = addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "gui.yes", "Yes"), button -> startDownload()).bounds(width / 2 - 104, y, 100, 20).build());
        yes.active = plan != null && error == null;
        addRenderableWidget(Button.builder(Component.translatableWithFallback(
                "gui.no", "No"), button -> onClose()).bounds(width / 2 + 4, y, 100, 20).build());
        if (plan == null && error == null) {
            CompletableFuture.supplyAsync(() -> {
                try { return ModrinthClient.resolve(); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            }).whenComplete((resolved, failure) -> Minecraft.getInstance().execute(() -> {
                if (failure == null) {
                    plan = resolved;
                    status = Component.translatableWithFallback(
                            "screen.goosetools.shader_install.ready",
                            "Ready: %s verified stable file(s).", resolved.artifacts().size()).getString();
                    yes.active = true;
                } else {
                    error = rootMessage(failure);
                    status = error;
                    yes.active = false;
                }
            }));
        }
    }

    private void startDownload() {
        if (plan == null) return;
        yes.active = false;
        if (!plan.requiresRestart()) {
            startImmediateSetup();
            return;
        }
        status = Component.translatableWithFallback(
                "screen.goosetools.shader_install.downloading", "Downloading and verifying files...").getString();
        CompletableFuture.supplyAsync(() -> {
            try { return ModrinthClient.download(plan, launch); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        }).whenComplete((manifest, failure) -> Minecraft.getInstance().execute(() -> {
            if (failure == null) {
                RecommendedShaderManager.armRestart(manifest, launch);
                Minecraft.getInstance().setScreenAndShow(parent);
            } else {
                error = rootMessage(failure);
                status = error;
                yes.active = true;
            }
        }));
    }

    private void startImmediateSetup() {
        status = Component.translatableWithFallback(
                "screen.goosetools.shader_install.preparing_now",
                "Installing the shader and generating its Euphoria patch...").getString();
        CompletableFuture.supplyAsync(() -> {
            try { return RecommendedShaderManager.prepareImmediate(plan); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        }).whenComplete((patched, failure) -> Minecraft.getInstance().execute(() -> {
            if (failure != null) {
                error = rootMessage(failure);
                status = error;
                yes.active = true;
                return;
            }
            status = Component.translatableWithFallback(
                    "screen.goosetools.shader_install.applying_now",
                    "Applying the POPULAR profile and enabling Iris...").getString();
            if (RecommendedShaderManager.applyPrepared(patched)) {
                RecommendedShaderManager.announceImmediateApplied();
                Minecraft.getInstance().setScreenAndShow(parent);
            } else {
                error = Component.translatableWithFallback(
                        "screen.goosetools.shader_install.apply_failed",
                        "The shader was installed, but it could not be enabled. See the log for details.").getString();
                status = error;
                yes.active = true;
            }
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xED10161D);
        graphics.text(font, title, width / 2 - font.width(title) / 2,
                height / 2 - 86, 0xFFFFFFFF, false);
        Component warning;
        if (plan == null) {
            warning = Component.translatableWithFallback(
                    "screen.goosetools.shader_install.explanation_checking",
                    "GooseTools checks Sodium, Iris, Euphoria Patcher and Complementary independently. Only missing files will be downloaded, with SHA-512 and archive identity verification.");
        } else if (!plan.requiresRestart()) {
            warning = Component.translatableWithFallback(
                    "screen.goosetools.shader_install.explanation_now",
                    "All required Fabric mods are already loaded. GooseTools will install only the missing Complementary shader if needed, generate its Euphoria patch, apply POPULAR and enable it immediately. No restart is needed.");
        } else if (launch.automatic()) {
            warning = Component.translatableWithFallback(
                        "screen.goosetools.shader_install.explanation",
                        "GooseTools will download only the missing files listed below. A Fabric mod must be installed after this match, with a backup and one automatic restart through %s. Do not click Launch again.",
                        launch.launcher().displayName());
        } else {
            warning = Component.translatableWithFallback(
                        "screen.goosetools.shader_install.explanation_manual",
                        "GooseTools will download only the missing files listed below. A Fabric mod must be installed after this match; reopen Minecraft through %s manually afterward.",
                        launch.launcher().displayName());
        }
        int lineY = height / 2 - 58;
        for (var line : font.split(warning, Math.min(420, width - 40))) {
            graphics.text(font, line, width / 2 - Math.min(420, width - 40) / 2,
                    lineY, 0xFFC8D7DF, false);
            lineY += font.lineHeight + 2;
        }
        Component state = Component.literal(status == null ? "" : status);
        graphics.text(font, state, width / 2 - font.width(state) / 2,
                height / 2 + 42, error == null ? 0xFF91DCFF : 0xFFFF7979, false);
        if (plan != null) {
            int artifactY = height / 2 - 5;
            for (ModrinthClient.Artifact artifact : plan.artifacts()) {
                Component item = Component.literal(artifact.name() + " · " + artifact.version());
                graphics.text(font, item, width / 2 - font.width(item) / 2,
                        artifactY, 0xFF91DFA6, false);
                artifactY += font.lineHeight + 1;
            }
            if (plan.artifacts().isEmpty()) {
                Component item = Component.translatableWithFallback(
                        "screen.goosetools.shader_install.immediate_only",
                        "Installed stack found; Euphoria can generate and enable the shader now without restarting.");
                graphics.text(font, item, width / 2 - font.width(item) / 2,
                        artifactY, 0xFF91DFA6, false);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
