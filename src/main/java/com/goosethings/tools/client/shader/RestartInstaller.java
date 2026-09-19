package com.goosethings.tools.client.shader;

import com.goosethings.tools.GooseTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/** Captures the launcher command and starts the tiny JDK-only installer after Minecraft closes. */
public final class RestartInstaller {
    private RestartInstaller() {
    }

    public static Optional<Launch> capture() {
        ProcessHandle.Info info = ProcessHandle.current().info();
        Optional<String> command = info.command();
        Optional<String[]> arguments = info.arguments();
        LauncherKind launcher = LauncherKind.detect(System.getProperty("minecraft.launcher.brand", ""));
        if (command.isPresent() && arguments.isPresent()) {
            return Optional.of(new Launch(command.get(), List.of(arguments.get()), launcher));
        }
        return reconstruct(
                command,
                ManagementFactory.getRuntimeMXBean().getInputArguments(),
                System.getProperty("java.class.path", ""),
                System.getProperty("sun.java.command", ""),
                Path.of(System.getProperty("java.home", "")),
                launcher);
    }

    public static void writeLaunch(Properties manifest, Launch launch) {
        manifest.setProperty("launch.automatic", Boolean.toString(launch.automatic()));
        manifest.setProperty("launch.launcher", launch.launcher().storedName());
        if (!launch.automatic()) {
            manifest.setProperty("launch.arg.count", "0");
            return;
        }
        manifest.setProperty("launch.command", encode(launch.command()));
        manifest.setProperty("launch.arg.count", Integer.toString(launch.arguments().size()));
        for (int index = 0; index < launch.arguments().size(); index++) {
            manifest.setProperty("launch.arg." + index, encode(launch.arguments().get(index)));
        }
    }

    public static void spawn(Path manifest) throws IOException {
        Path origin = FabricLoader.getInstance().getModContainer(GooseTools.MOD_ID)
                .flatMap(container -> container.getOrigin().getPaths().stream().findFirst())
                .orElseThrow(() -> new IOException("GooseTools origin is unavailable"));
        String java = helperJavaExecutable()
                .orElseThrow(() -> new IOException("Java executable is unavailable"));
        new ProcessBuilder(java, "-cp", origin.toString(),
                GooseToolsUpdateHelper.class.getName(), manifest.toString(),
                Long.toString(ProcessHandle.current().pid()))
                .directory(FabricLoader.getInstance().getGameDir().toFile())
                .start();
    }

    static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static Optional<Launch> reconstruct(
            Optional<String> processCommand,
            List<String> jvmArguments,
            String classPath,
            String sunJavaCommand,
            Path javaHome) {
        return reconstruct(
                processCommand, jvmArguments, classPath, sunJavaCommand, javaHome, LauncherKind.OTHER);
    }

    static Optional<Launch> reconstruct(
            Optional<String> processCommand,
            List<String> jvmArguments,
            String classPath,
            String sunJavaCommand,
            Path javaHome,
            LauncherKind launcher) {
        Optional<String> executable = processCommand.filter(value -> !value.isBlank());
        if (executable.isEmpty()) {
            executable = javaExecutable(javaHome);
        }
        List<String> application = splitCommandLine(sunJavaCommand);
        if (executable.isEmpty() || application.isEmpty()) {
            return Optional.empty();
        }

        List<String> arguments = new ArrayList<>(jvmArguments.size() + application.size() + 2);
        arguments.addAll(jvmArguments);
        String entrypoint = application.getFirst();
        if (entrypoint.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
            arguments.add("-jar");
        } else if (!classPath.isBlank()) {
            arguments.add("-cp");
            arguments.add(classPath);
        }
        arguments.addAll(application);
        return Optional.of(new Launch(executable.get(), arguments, launcher));
    }

    /** Parses the quoting emitted by the Java launcher, including empty and escaped arguments. */
    static List<String> splitCommandLine(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean started = false;
        int index = 0;
        while (index < commandLine.length()) {
            char character = commandLine.charAt(index);
            if (Character.isWhitespace(character) && !quoted) {
                if (started) {
                    result.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
                index++;
                continue;
            }
            if (character == '\\') {
                int start = index;
                while (index < commandLine.length() && commandLine.charAt(index) == '\\') index++;
                int slashes = index - start;
                if (index < commandLine.length() && commandLine.charAt(index) == '"') {
                    current.append("\\".repeat(slashes / 2));
                    if ((slashes & 1) == 0) quoted = !quoted;
                    else current.append('"');
                    started = true;
                    index++;
                } else {
                    current.append("\\".repeat(slashes));
                    started = true;
                }
                continue;
            }
            if (character == '"') {
                quoted = !quoted;
                started = true;
                index++;
                continue;
            }
            current.append(character);
            started = true;
            index++;
        }
        if (started) result.add(current.toString());
        return List.copyOf(result);
    }

    private static Optional<String> helperJavaExecutable() {
        return ProcessHandle.current().info().command().filter(value -> !value.isBlank())
                .or(() -> javaExecutable(Path.of(System.getProperty("java.home", ""))));
    }

    private static Optional<String> javaExecutable(Path javaHome) {
        if (javaHome == null || javaHome.toString().isBlank()) return Optional.empty();
        Path bin = javaHome.resolve("bin");
        for (String name : List.of("javaw.exe", "java.exe", "java")) {
            Path candidate = bin.resolve(name).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate)) return Optional.of(candidate.toString());
        }
        return Optional.empty();
    }

    public enum LauncherKind {
        HMCL("hmcl", "HMCL"),
        PCL("pcl", "PCL"),
        MINECRAFT_LAUNCHER("minecraft_launcher", "Minecraft Launcher"),
        OTHER("other", "the current launcher");

        private final String storedName;
        private final String displayName;

        LauncherKind(String storedName, String displayName) {
            this.storedName = storedName;
            this.displayName = displayName;
        }

        public String storedName() {
            return storedName;
        }

        public String displayName() {
            return displayName;
        }

        static LauncherKind detect(String brand) {
            String normalized = brand == null ? "" : brand.toLowerCase(Locale.ROOT);
            if (normalized.contains("hmcl")) return HMCL;
            if (normalized.contains("pcl") || normalized.contains("plain craft")) return PCL;
            if (normalized.contains("minecraft-launcher")
                    || normalized.contains("minecraft launcher")
                    || normalized.contains("mojang")) return MINECRAFT_LAUNCHER;
            return OTHER;
        }

        static LauncherKind fromStored(String value) {
            for (LauncherKind kind : values()) {
                if (kind.storedName.equals(value)) return kind;
            }
            return OTHER;
        }
    }

    public record Launch(String command, List<String> arguments, LauncherKind launcher) {
        public Launch {
            command = command == null ? "" : command;
            arguments = List.copyOf(arguments);
            launcher = launcher == null ? LauncherKind.OTHER : launcher;
        }

        public Launch(String command, List<String> arguments) {
            this(command, arguments, LauncherKind.OTHER);
        }

        public static Launch manual() {
            return new Launch(
                    "", List.of(), LauncherKind.detect(System.getProperty("minecraft.launcher.brand", "")));
        }

        public boolean automatic() {
            return !command.isBlank();
        }
    }
}
