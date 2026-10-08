package com.goosethings.tools.client.update.bootstrap;

import com.goosethings.tools.client.update.*;
import java.awt.GraphicsEnvironment;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import javax.swing.SwingUtilities;

/** HMCL waits for this JVM before starting Minecraft; it never captures login or game arguments. */
public final class GooseToolsBootstrap {
    private GooseToolsBootstrap() { }

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        boolean headless = GraphicsEnvironment.isHeadless();
        for (int index = 0; index < args.length; index++) {
            if (args[index].equals("--headless")) headless = true;
            else if (List.of("--game-dir", "--instance-dir").contains(args[index]) && index + 1 < args.length) {
                arguments.put(args[index], args[++index]);
            } else throw new IllegalArgumentException("Unsupported launcher updater argument");
        }
        Path game = Path.of(Objects.requireNonNull(arguments.get("--game-dir"))).toAbsolutePath().normalize();
        Path instance = Path.of(Objects.requireNonNull(arguments.get("--instance-dir"))).toAbsolutePath().normalize();
        Path directory = game.resolve("config/goosetools/bootstrap");
        BootstrapFiles.safe(directory);
        Files.createDirectories(directory);
        UpdateMonitor monitor = new UpdateMonitor();
        UpdateMonitor initialMonitor = monitor;
        BootstrapWindow[] window = new BootstrapWindow[1];
        int exitCode = 0;
        if (!headless) SwingUtilities.invokeAndWait(() -> window[0] = new BootstrapWindow(game, initialMonitor));
        try {
            boolean repeat;
            do {
                repeat = false;
                try { run(game, instance, monitor); exitCode = 0; }
                catch (CancellationException cancelled) { System.out.println("GooseTools: update cancelled; continuing with installed version"); }
                catch (Exception failure) {
                    System.err.println("GooseTools launcher update unavailable: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
                    monitor.update(UpdateMonitor.Phase.FAILED, "", 0, 0);
                    exitCode = Files.exists(directory.resolve("pending-startup.properties"))
                            || failure instanceof BootstrapTransaction.BusyException ? 2 : 0;
                    if (window[0] != null) window[0].blocked(exitCode != 0, failure instanceof BootstrapTransaction.BusyException);
                    receipt(game, "", "failed");
                    if (window[0] != null && window[0].retry()) {
                        monitor = new UpdateMonitor(); window[0].monitor(monitor); window[0].blocked(false, false); repeat = true;
                    }
                }
            } while (repeat);
            if (window[0] != null && monitor.snapshot().phase() != UpdateMonitor.Phase.CANCELLED) Thread.sleep(800);
        } catch (Exception failure) {
            // Keep HMCL usable when files are locked, GitHub is unavailable or a hook is duplicated.
            System.err.println("GooseTools updater: " + failure.getMessage());
            exitCode = 2;
        } finally { if (window[0] != null) window[0].close(); }
        if (exitCode != 0) System.exit(exitCode);
    }

    static void run(Path game, Path instance, UpdateMonitor monitor) throws Exception {
        Path lockFile = game.resolve("config/goosetools/bootstrap/updater.lock");
        try (var channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new BootstrapTransaction.BusyException("Another updater is running for this instance");
            runLocked(game, instance, monitor);
        }
    }

    private static void runLocked(Path game, Path instance, UpdateMonitor monitor) throws Exception {
        monitor.checkCancelled();
        if (BootstrapTransaction.active(game)) throw new BootstrapTransaction.BusyException("This Minecraft instance is still running");
        if (Files.exists(game.resolve("config/goosetools/bootstrap/pending-startup.properties"))) {
            monitor.update(UpdateMonitor.Phase.INSTALLING, "", 0, 0);
        }
        boolean rollback = BootstrapTransaction.recover(game);
        var inventory = BootstrapInventory.read(game, instance);
        var preferences = UpdatePreferences.load(game.resolve("config/goosetools/auto-update.properties"));
        if (!preferences.enabled()) {
            monitor.update(UpdateMonitor.Phase.CURRENT, inventory.version(), 0, 0);
            receipt(game, inventory.version(), "disabled"); return;
        }
        String failed = Files.isRegularFile(game.resolve("config/goosetools/failed-update-version"))
                ? Files.readString(game.resolve("config/goosetools/failed-update-version")).trim() : "";
        for (var release : GitHubUpdateClient.releases(inventory.version(), preferences.includeAlpha(), null, monitor)) {
            if (release.version().equals(failed)) continue;
            // Before Fabric loads, protocol changes can also be installed. The server handshake remains exact.
            var staged = GitHubUpdateClient.stage(release, game, inventory.origin(), -1, inventory.installed(), monitor);
            if (staged.isEmpty()) continue;
            monitor.update(UpdateMonitor.Phase.INSTALLING, release.version(), release.size(), release.size());
            BootstrapTransaction.install(game, staged.get());
            monitor.update(UpdateMonitor.Phase.READY, release.version(), release.size(), release.size());
            receipt(game, release.version(), "installed");
            System.out.println("GooseTools: installed " + release.version() + " before Minecraft startup");
            return;
        }
        monitor.update(UpdateMonitor.Phase.CURRENT, inventory.version(), 0, 0);
        receipt(game, inventory.version(), rollback ? "rolled-back" : "current");
        System.out.println("GooseTools: " + inventory.version() + " checked before Minecraft startup");
    }

    static void receipt(Path game, String version, String outcome) throws Exception {
        Properties values = new Properties();
        values.setProperty("version", version);
        values.setProperty("outcome", outcome);
        values.setProperty("time", Long.toString(System.currentTimeMillis()));
        BootstrapFiles.write(game.resolve("config/goosetools/bootstrap/last-check.properties"), values);
    }
}
