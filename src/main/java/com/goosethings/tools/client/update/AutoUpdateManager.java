package com.goosethings.tools.client.update;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.shader.RestartInstaller;
import com.goosethings.tools.client.vision.BlackoutAssistClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.goosethings.tools.client.update.bootstrap.BootstrapFiles;
import com.goosethings.tools.client.update.bootstrap.BootstrapTransaction;
import com.goosethings.tools.client.update.bootstrap.HmclIntegration;

/** All presentation and state changes run on the client thread; network work is asynchronous. */
public final class AutoUpdateManager {
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("goosetools");
    private static final Path SETTINGS = DIRECTORY.resolve("auto-update.properties");
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "GooseTools update check");
        thread.setDaemon(true);
        return thread;
    });
    private static UpdatePreferences preferences = new UpdatePreferences(true, true);
    private static Pending pending;
    private static boolean checking;
    private static boolean postponed;
    private static long nextCheck;
    private static String serverVersion;
    private static int serverProtocol = GooseTools.PROTOCOL_VERSION;
    private static long generation;
    private static Component status = text("checking", "Checking for updates...");
    private static UpdateMonitor monitor = new UpdateMonitor();
    private static boolean firstScreen;
    private static boolean hmclConnected;

    private AutoUpdateManager() { }

    public static void register() {
        try { preferences = UpdatePreferences.load(SETTINGS); }
        catch (IOException failure) { GooseTools.LOGGER.warn("Could not load automatic update settings", failure); }
        // Development launches must never replace class directories or fetch production updates.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        Path game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        try {
            BootstrapTransaction.recordActive(game);
            hmclConnected = HmclIntegration.connect(game, instance(game)) == HmclIntegration.Result.CONNECTED;
        } catch (Exception failure) { GooseTools.LOGGER.warn("Could not prepare HMCL updater", failure); }
        nextCheck = Long.MAX_VALUE;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!firstScreen && client.isGameLoadFinished() && client.gui.overlay() == null
                    && (client.gui.screen() instanceof TitleScreen || client.level != null)) {
                firstScreen = true;
                try { BootstrapTransaction.confirm(game, GooseTools.VERSION); }
                catch (Exception failure) { GooseTools.LOGGER.warn("Could not confirm launcher update", failure); }
                if (preferences.enabled() && client.level == null) {
                    if (!launcherResult(game)) check();
                    showProgress(client.gui.screen());
                } else if (preferences.enabled()) {
                    nextCheck = System.nanoTime() + Duration.ofSeconds(15).toNanos();
                }
            }
            if (!preferences.enabled()) return;
            if (firstScreen && !checking && System.nanoTime() >= nextCheck && !postponed && pending == null) check();
            if (pending != null && !postponed && safeToInstall(client)
                    && !(client.gui.screen() instanceof UpdateRestartScreen)
                    && !(client.gui.screen() instanceof UpdateProgressScreen)
                    && (client.level != null || client.gui.screen() instanceof TitleScreen
                    || client.gui.screen() instanceof JoinMultiplayerScreen
                    || client.gui.screen() instanceof DisconnectedScreen)) {
                client.setScreenAndShow(new UpdateRestartScreen(client.gui.screen(), pending.version()));
            }
        });
    }

    public static void onServerVersion(String version, int protocol) {
        UpdateVersion.parse(version); // Only official version formats may become a lookup target.
        serverVersion = version;
        serverProtocol = protocol;
        if (!version.equals(GooseTools.VERSION) && preferences.enabled()) {
            generation++;
            monitor.cancel();
            pending = null;
            postponed = false;
            nextCheck = 0;
            if (!checking) check();
        }
    }

    public static void check() {
        if (checking || !preferences.enabled() || FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        checking = true;
        monitor = new UpdateMonitor();
        UpdateMonitor transfer = monitor;
        status = text("checking", "Checking for updates...");
        long request = generation;
        String required = serverVersion != null && !serverVersion.equals(GooseTools.VERSION) ? serverVersion : null;
        int protocol = required == null ? GooseTools.PROTOCOL_VERSION : serverProtocol;
        boolean alpha = preferences.includeAlpha();
        Map<String, String> installed = new HashMap<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> installed.put(mod.getMetadata().getId(),
                mod.getMetadata().getVersion().getFriendlyString()));
        installed.put("java", Integer.toString(Runtime.version().feature()));
        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        Path origin = FabricLoader.getInstance().getModContainer(GooseTools.MOD_ID).orElseThrow()
                .getOrigin().getPaths().getFirst().toAbsolutePath().normalize();
        CompletableFuture.supplyAsync(() -> {
            try {
                String failed = Files.isRegularFile(DIRECTORY.resolve("failed-update-version"))
                        ? Files.readString(DIRECTORY.resolve("failed-update-version")).trim() : "";
                for (var release : GitHubUpdateClient.releases(GooseTools.VERSION, alpha, required, transfer)) {
                    if (release.version().equals(failed)) continue;
                    var manifest = GitHubUpdateClient.stage(release, gameDir, origin, protocol, installed, transfer);
                    if (manifest.isPresent()) return new Pending(manifest.get(), release.version());
                }
                return null;
            } catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, WORKER).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
            checking = false;
            if (request != generation || !preferences.enabled()) {
                return;
            }
            nextCheck = System.nanoTime() + Duration.ofMinutes(failure == null ? 60 : 5).toNanos();
            if (failure != null) {
                if (transfer.snapshot().phase() != UpdateMonitor.Phase.CANCELLED)
                    transfer.update(UpdateMonitor.Phase.FAILED, "", 0, 0);
                status = text("unavailable", "Update check unavailable; the game can continue normally.");
                GooseTools.LOGGER.warn("GooseTools update check failed; retrying later", failure.getCause());
            } else if (result == null) {
                transfer.update(UpdateMonitor.Phase.CURRENT, GooseTools.VERSION, 0, 0);
                status = text("current", "No compatible update is available.");
            } else {
                pending = result;
                var snapshot = transfer.snapshot();
                transfer.update(UpdateMonitor.Phase.READY, result.version(), snapshot.downloaded(), snapshot.total());
                status = text("ready", "Downloaded and verified GooseTools %s.", result.version());
                GooseTools.LOGGER.info("Staged official GooseTools update {}", result.version());
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(text("deferred",
                            "GooseTools %s is ready. Installation will wait until this game ends or you disconnect.",
                            result.version()));
                }
            }
        }));
    }

    public static boolean safeToInstall(Minecraft client) {
        return client.level == null || (BlackoutAssistClient.isInLobby() && pending != null
                && pending.version().equals(serverVersion));
    }

    public static Component install() {
        if (pending == null || !preferences.enabled() || !safeToInstall(Minecraft.getInstance())) {
            return text("cancelled", "Installation postponed.");
        }
        try {
            Properties manifest = new Properties();
            try (var input = Files.newInputStream(pending.manifest())) { manifest.load(input); }
            // Capture fresh launch parameters immediately before exiting; no account data in download state.
            RestartInstaller.writeLaunch(manifest, RestartInstaller.capture().orElseGet(RestartInstaller.Launch::manual));
            try (var output = Files.newOutputStream(pending.manifest())) {
                manifest.store(output, "Verified GooseTools update");
            }
            RestartInstaller.spawn(pending.manifest());
            pending = null;
            Minecraft.getInstance().stop();
            return null;
        } catch (IOException failure) {
            postponed = true;
            GooseTools.LOGGER.error("Could not start GooseTools update installer", failure);
            return text("install_failed", "Could not start the installer. Your current game is unchanged.");
        }
    }

    public static void postpone() { postponed = true; }
    public static void resume() { postponed = false; nextCheck = 0; check(); }
    public static UpdatePreferences preferences() { return preferences; }
    public static Component status() { return status; }

    public static void settings(boolean enabled, boolean alpha) {
        preferences = new UpdatePreferences(enabled, alpha);
        generation++;
        monitor.cancel();
        pending = null;
        try { preferences.save(SETTINGS); }
        catch (IOException failure) { GooseTools.LOGGER.warn("Could not save automatic update settings", failure); }
        postponed = false;
        nextCheck = 0;
    }

    static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("screen.goosetools.update." + key, fallback, args);
    }

    public static UpdateMonitor.Snapshot progress() { return monitor.snapshot(); }
    static String pendingVersion() { return pending == null ? null : pending.version(); }
    public static void showProgress(net.minecraft.client.gui.screens.Screen parent) {
        Minecraft.getInstance().setScreenAndShow(new UpdateProgressScreen(parent));
    }
    static void continueGame() {
        if (checking) {
            generation++;
            monitor.cancel();
            nextCheck = System.nanoTime() + Duration.ofMinutes(5).toNanos();
        }
        postponed = true;
    }
    public static boolean hmclConnected() { return hmclConnected; }
    public static Component connectHmcl() {
        Path game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        try {
            var result = HmclIntegration.connect(game, instance(game));
            hmclConnected = result == HmclIntegration.Result.CONNECTED;
            return switch (result) {
                case CONNECTED -> text("hmcl_connected", "HMCL connected: updates install before the next launch.");
                case EXISTING_COMMAND -> text("hmcl_existing", "This instance already has a launch command. It was preserved.");
                case NOT_HMCL -> text("hmcl_missing", "Open this instance with HMCL to connect its pre-launch updater.");
            };
        } catch (Exception failure) {
            GooseTools.LOGGER.warn("Could not connect HMCL updater", failure);
            return text("hmcl_failed", "Could not connect HMCL. In-game updates remain available.");
        }
    }

    private static boolean launcherResult(Path game) {
        try {
            var value = BootstrapFiles.read(game.resolve("config/goosetools/bootstrap/last-check.properties"));
            long age = System.currentTimeMillis() - Long.parseLong(value.getProperty("time", "0"));
            String outcome = value.getProperty("outcome", "");
            if (age < 0 || age > Duration.ofMinutes(15).toMillis() || !GooseTools.VERSION.equals(value.getProperty("version"))
                    || !java.util.Set.of("current", "installed", "rolled-back").contains(outcome)) return false;
            monitor.update(UpdateMonitor.Phase.CURRENT, GooseTools.VERSION, 0, 0);
            status = outcome.equals("installed")
                    ? text("bootstrap_installed", "HMCL installed GooseTools %s before this launch.", GooseTools.VERSION)
                    : outcome.equals("rolled-back") ? text("bootstrap_rollback", "The previous update did not complete startup. The old version was restored.")
                    : text("bootstrap_checked", "HMCL checked GooseTools %s before this launch.", GooseTools.VERSION);
            nextCheck = System.nanoTime() + Duration.ofMinutes(60).toNanos();
            return true;
        } catch (Exception ignored) { return false; }
    }

    private static Path instance(Path game) {
        String directory = System.getenv("INST_DIR");
        return directory == null || directory.isBlank() ? game : Path.of(directory).toAbsolutePath().normalize();
    }

    private record Pending(Path manifest, String version) { }
}
