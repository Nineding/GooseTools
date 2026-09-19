package com.goosethings.tools.client.shader;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.ClientClickActions;
import com.goosethings.tools.client.vision.BlackoutAssistClient;
import com.goosethings.tools.xaero.GgdClientPreferences;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Coordinates immediate POPULAR-preset activation and the consented post-match installer. */
public final class RecommendedShaderManager {
    private static Path pendingManifest;
    private static boolean restartArmed;
    private static int restartTicks = -1;
    private static int activationTicks;
    private static boolean bootMarked;
    private static long lastSetupRequest;
    private static boolean manualRelaunch;
    private static boolean manualInstallNoticeShown;
    private static String launcherName = RestartInstaller.LauncherKind.OTHER.displayName();

    private RecommendedShaderManager() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            markBootHealthy();
            if (GgdClientPreferences.recommendedShaderPending()
                    && pendingManifest == null && !restartArmed
                    && (++activationTicks % 40) == 0) {
                if (tryApplyRecommended()) {
                    GgdClientPreferences.setRecommendedShaderPending(false);
                    tell("message.goosetools.shader.applied",
                            "Complementary Reimagined + Euphoria POPULAR is active.");
                }
            }
            if (restartArmed && restartTicks >= 0) {
                if (restartTicks % 20 == 0 && client.player != null) {
                    int seconds = Math.max(1, restartTicks / 20);
                    client.player.sendOverlayMessage(Component.translatableWithFallback(
                                    manualRelaunch
                                            ? "message.goosetools.shader.manual_countdown"
                                            : "message.goosetools.shader.restart_countdown",
                                    manualRelaunch
                                            ? "Shader setup will install in %s seconds; reopen through %s afterward."
                                            : "Verified shader setup will install in %s seconds and restart through %s. Do not click Launch again.",
                                    seconds,
                                    launcherName));
                }
                if (--restartTicks <= 0) {
                    performRestart(client);
                }
            }
        });
    }

    public static void requestSetup(Screen returnScreen) {
        Minecraft client = Minecraft.getInstance();
        long now = System.nanoTime();
        if (client.gui.screen() instanceof ShaderInstallConfirmScreen
                || now - lastSetupRequest < java.time.Duration.ofMillis(750).toNanos()) {
            return;
        }
        lastSetupRequest = now;
        if (hasRuntimeMods() && tryApplyRecommended()) {
            GgdClientPreferences.setRecommendedShaderPending(false);
            tell("message.goosetools.shader.applied",
                    "Complementary Reimagined + Euphoria POPULAR is active.");
            if (returnScreen != null) client.setScreenAndShow(returnScreen);
            return;
        }
        if (pendingManifest != null && Files.isRegularFile(pendingManifest)) {
            restartArmed = true;
            restartTicks = BlackoutAssistClient.isInLobby() ? 200 : -1;
            GgdClientPreferences.setRecommendedShaderPending(true);
            announceReady();
            if (restartTicks >= 0) showCancelAction();
            return;
        }
        RestartInstaller.Launch launch = RestartInstaller.capture()
                .orElseGet(RestartInstaller.Launch::manual);
        Screen parent = returnScreen != null ? returnScreen : client.gui.screen();
        client.setScreenAndShow(new ShaderInstallConfirmScreen(parent, launch));
    }

    public static void armRestart(Path manifest, RestartInstaller.Launch launch) {
        pendingManifest = manifest.toAbsolutePath().normalize();
        manualRelaunch = !launch.automatic();
        launcherName = launch.launcher().displayName();
        restartArmed = true;
        restartTicks = BlackoutAssistClient.isInLobby() ? 200 : -1;
        GgdClientPreferences.setRecommendedShaderPending(true);
        announceReady();
        if (restartTicks >= 0) showCancelAction();
    }

    private static void announceReady() {
        tell(restartTicks >= 0
                        ? (manualRelaunch
                                ? "message.goosetools.shader.ready_lobby_manual"
                                : "message.goosetools.shader.ready_lobby")
                        : (manualRelaunch
                                ? "message.goosetools.shader.ready_match_manual"
                                : "message.goosetools.shader.ready_match"),
                restartTicks >= 0
                        ? (manualRelaunch
                                ? "Files verified. The game will close and install in 10 seconds; reopen it through %s."
                                : "Files verified. Installation and one automatic restart through %s will begin in 10 seconds. Do not click Launch again.")
                        : (manualRelaunch
                                ? "Files verified. After returning to the lobby, the game will close and install; reopen it through %s."
                                : "Files verified. Installation and one automatic restart through %s will begin after returning to the lobby. Do not click Launch again."),
                launcherName);
    }

    public static void onLobbyState(boolean inLobby) {
        if (inLobby && restartArmed && restartTicks < 0) {
            restartTicks = 200;
            showCancelAction();
        }
    }

    public static void cancelRestart() {
        restartArmed = false;
        restartTicks = -1;
        GgdClientPreferences.setRecommendedShaderPending(false);
        tell("message.goosetools.shader.restart_cancelled",
                "Shader installation cancelled. Open the shader setup again when you are ready.");
    }

    public static Optional<Path> findBasePack() {
        Path shaderpacks = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        if (!Files.isDirectory(shaderpacks)) return Optional.empty();
        try (Stream<Path> files = Files.list(shaderpacks)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.startsWith("complementaryreimagined") && name.endsWith(".zip");
                    }).max(Comparator.comparing(path -> path.getFileName().toString()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static Optional<Path> findPatchedPack() {
        Path shaderpacks = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        if (!Files.isDirectory(shaderpacks)) return Optional.empty();
        try (Stream<Path> files = Files.list(shaderpacks)) {
            return files.filter(Files::isDirectory)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.contains("complementaryreimagined")
                                && name.contains("euphoriapatches")
                                && Files.isRegularFile(path.resolve("shaders").resolve("shaders.properties"));
                    }).max(Comparator.comparing(path -> path.getFileName().toString()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    static Path prepareImmediate(ModrinthClient.Plan plan) throws Exception {
        if (plan.requiresRestart()) {
            throw new IllegalArgumentException("Installed Fabric mods must be updated after Minecraft exits");
        }
        if (!hasRuntimeMods()) {
            throw new IllegalStateException("Sodium, Iris and Euphoria Patcher must be loaded first");
        }
        Path basePack = ModrinthClient.installShaderPackNow(plan);
        Optional<Path> existing = findPatchedPackFor(basePack);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean newlyDownloaded = plan.artifacts().stream()
                .anyMatch(artifact -> "complementary".equals(artifact.modId()));
        if (newlyDownloaded) {
            // Euphoria's folder watcher normally receives the atomic move. Give it the first
            // chance to patch so GooseTools does not launch a duplicate patch/reload cycle.
            Optional<Path> watched = waitForPatchedPack(basePack, 5L);
            if (watched.isPresent()) {
                return watched.get();
            }
        }
        GooseTools.LOGGER.info("Requesting an in-process Euphoria patch for {}", basePack.getFileName());
        EuphoriaCompat.processNewShaderpack(basePack);
        return waitForPatchedPack(basePack, 60L).orElseThrow(() ->
                new IOException("Euphoria did not finish generating the patched shader within 60 seconds"));
    }

    private static Optional<Path> waitForPatchedPack(Path basePack, long timeoutSeconds)
            throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            Optional<Path> patched = findPatchedPackFor(basePack);
            if (patched.isPresent()) {
                return patched;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while Euphoria generated the patched shader", exception);
            }
        }
        return Optional.empty();
    }

    static boolean applyPrepared(Path patched) {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }
        Path irisConfig = FabricLoader.getInstance().getConfigDir().resolve("iris.properties");
        try {
            PopularProfile.backup(irisConfig);
            PopularProfile.apply(patched);
            IrisCompat.enable(patched.getFileName().toString());
            GooseTools.LOGGER.info("Applied Euphoria POPULAR profile to {}", patched.getFileName());
            return true;
        } catch (IOException | ReflectiveOperationException | LinkageError exception) {
            GooseTools.LOGGER.error("Could not apply recommended shader profile", exception);
            tell("message.goosetools.shader.apply_failed",
                    "Could not activate the recommended shader: %s", exception.getMessage());
            return false;
        }
    }

    static void announceImmediateApplied() {
        GgdClientPreferences.setRecommendedShaderPending(false);
        tell("message.goosetools.shader.applied",
                "Complementary Reimagined + Euphoria POPULAR is active.");
    }

    static boolean tryApplyRecommended() {
        Optional<Path> patched = findPatchedPack();
        if (patched.isEmpty()) {
            return false;
        }
        return applyPrepared(patched.get());
    }

    private static Optional<Path> findPatchedPackFor(Path basePack) {
        String name = basePack.getFileName().toString();
        String stem = name.substring(0, name.length() - ".zip".length()).toLowerCase(Locale.ROOT);
        return findPatchedPack().filter(path ->
                path.getFileName().toString().toLowerCase(Locale.ROOT).contains(stem));
    }

    private static boolean hasRuntimeMods() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("sodium") && loader.isModLoaded("iris")
                && loader.isModLoaded("euphoria_patcher");
    }

    private static void performRestart(Minecraft client) {
        restartArmed = false;
        restartTicks = -1;
        if (pendingManifest == null || !Files.isRegularFile(pendingManifest)) {
            pendingManifest = null;
            GgdClientPreferences.setRecommendedShaderPending(false);
            tell("message.goosetools.shader.install_missing",
                    "The verified update manifest is missing; restart was cancelled.");
            return;
        }
        try {
            RestartInstaller.spawn(pendingManifest);
            client.stop();
        } catch (IOException exception) {
            tell("message.goosetools.shader.restart_failed",
                    "Could not start the installer: %s", exception.getMessage());
        }
    }

    private static void markBootHealthy() {
        Path marker = FabricLoader.getInstance().getConfigDir().resolve("goosetools").resolve("boot-ok");
        if (!bootMarked) {
            bootMarked = true;
            try {
                Files.createDirectories(marker.getParent());
                Files.writeString(marker, GooseTools.VERSION);
            } catch (IOException exception) {
                GooseTools.LOGGER.warn("Could not write GooseTools boot marker", exception);
            }
        }
        Minecraft client = Minecraft.getInstance();
        Path manualResult = marker.getParent().resolve("manual-install-complete");
        if (!manualInstallNoticeShown && client.player != null && Files.isRegularFile(manualResult)) {
            manualInstallNoticeShown = true;
            tell("message.goosetools.shader.manual_complete",
                    "Recommended shader files were installed. This launcher start is now using the updated environment.");
            try {
                Files.deleteIfExists(manualResult);
            } catch (IOException exception) {
                GooseTools.LOGGER.warn("Could not clear GooseTools manual install marker", exception);
            }
        }
    }

    private static void tell(String key, String fallback, Object... args) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.translatableWithFallback(key, fallback, args));
        }
    }

    private static void showCancelAction() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.translatableWithFallback(
                        "message.goosetools.shader.cancel_restart", "[Cancel restart]")
                .withStyle(style -> style.withColor(ChatFormatting.RED).withUnderlined(true)
                        .withClickEvent(new ClickEvent.Custom(
                                ClientClickActions.CANCEL_SHADER_RESTART,
                                java.util.Optional.empty()))));
    }
}
