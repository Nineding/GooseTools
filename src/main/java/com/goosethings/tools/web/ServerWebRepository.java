package com.goosethings.tools.web;

import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Owns the server-local config/goosetools/web page tree. */
public final class ServerWebRepository {
    private static final List<String> DEFAULT_FILES = List.of(
            "manifest.json",
            "index.html",
            "roles.html",
            "gameplay.html",
            "features.html",
            "styles.css",
            "app.js",
            "lang/zh_cn.json",
            "lang/en_us.json",
            "images/task_progress.png",
            "images/theme_sun.png",
            "images/theme_moon.png");

    private static final Path ROOT = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("goosetools")
            .resolve("web")
            .toAbsolutePath()
            .normalize();

    private static volatile WebBundle current;

    private ServerWebRepository() {
    }

    public static synchronized void initialize() {
        try {
            installDefaultsIfMissing();
            reload();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize GooseTools server web root " + ROOT, exception);
        }
    }

    public static synchronized WebBundle reload() throws IOException {
        WebBundle loaded = WebBundle.load(ROOT);
        current = loaded;
        GooseTools.LOGGER.info(
                "Loaded GooseTools web bundle {} ({} pages, {} compressed bytes) from {}",
                loaded.hash(),
                loaded.pageIds().size(),
                loaded.compressed().length,
                ROOT);
        return loaded;
    }

    public static WebBundle current() {
        WebBundle bundle = current;
        if (bundle == null) {
            throw new IllegalStateException("Server web repository has not been initialized");
        }
        return bundle;
    }

    public static Path root() {
        return ROOT;
    }

    private static void installDefaultsIfMissing() throws IOException {
        Files.createDirectories(ROOT);
        if (Files.isRegularFile(ROOT.resolve("manifest.json"))) {
            return;
        }
        for (String relative : DEFAULT_FILES) {
            Path destination = ROOT.resolve(relative).normalize();
            if (!destination.startsWith(ROOT)) {
                throw new IOException("Unsafe bundled web path: " + relative);
            }
            Files.createDirectories(destination.getParent());
            try (InputStream input = ServerWebRepository.class.getResourceAsStream("/default_web/" + relative)) {
                if (input == null) {
                    throw new IOException("Missing bundled default web file: " + relative);
                }
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        GooseTools.LOGGER.info("Installed default GooseTools pages into {}", ROOT);
    }
}
