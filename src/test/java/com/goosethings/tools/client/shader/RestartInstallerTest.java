package com.goosethings.tools.client.shader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartInstallerTest {
    @Test
    void parsesQuotedAndEscapedLauncherArguments() {
        assertEquals(
                List.of("net.fabricmc.loader.KnotClient", "--gameDir", "D:\\Games\\My Pack", "", "say \"hi\""),
                RestartInstaller.splitCommandLine(
                        "net.fabricmc.loader.KnotClient --gameDir \"D:\\Games\\My Pack\" \"\" \"say \\\"hi\\\"\""));
    }

    @Test
    void reconstructsFabricLaunchWhenProcessArgumentsAreUnavailable(@TempDir Path javaHome) throws Exception {
        Path executable = javaHome.resolve("bin").resolve("javaw.exe");
        Files.createDirectories(executable.getParent());
        Files.createFile(executable);

        RestartInstaller.Launch launch = RestartInstaller.reconstruct(
                Optional.empty(),
                List.of("-Xmx2G", "-Dfabric.side=client"),
                "loader.jar;game.jar",
                "net.fabricmc.loader.impl.launch.knot.KnotClient --gameDir D:\\Game",
                javaHome).orElseThrow();

        assertEquals(executable.toAbsolutePath().normalize().toString(), launch.command());
        assertEquals(List.of(
                        "-Xmx2G", "-Dfabric.side=client", "-cp", "loader.jar;game.jar",
                        "net.fabricmc.loader.impl.launch.knot.KnotClient", "--gameDir", "D:\\Game"),
                launch.arguments());
        assertTrue(launch.automatic());
    }

    @Test
    void fallsBackToManualModeOnlyWhenLaunchCannotBeReconstructed(@TempDir Path javaHome) {
        assertTrue(RestartInstaller.reconstruct(
                Optional.empty(), List.of(), "", "", javaHome).isEmpty());
        assertFalse(RestartInstaller.Launch.manual().automatic());
    }

    @Test
    void detectsSupportedLauncherBrands() {
        assertEquals(RestartInstaller.LauncherKind.HMCL,
                RestartInstaller.LauncherKind.detect("HMCL"));
        assertEquals(RestartInstaller.LauncherKind.PCL,
                RestartInstaller.LauncherKind.detect("Plain Craft Launcher 2"));
        assertEquals(RestartInstaller.LauncherKind.PCL,
                RestartInstaller.LauncherKind.detect("PCL2"));
        assertEquals(RestartInstaller.LauncherKind.MINECRAFT_LAUNCHER,
                RestartInstaller.LauncherKind.detect("minecraft-launcher"));
        assertEquals(RestartInstaller.LauncherKind.OTHER,
                RestartInstaller.LauncherKind.detect("unknown"));
    }

    @Test
    void preservesLauncherKindDuringFallbackReconstruction(@TempDir Path javaHome) throws Exception {
        Path executable = javaHome.resolve("bin").resolve("javaw.exe");
        Files.createDirectories(executable.getParent());
        Files.createFile(executable);
        RestartInstaller.Launch launch = RestartInstaller.reconstruct(
                Optional.empty(), List.of(), "loader.jar", "example.Main", javaHome,
                RestartInstaller.LauncherKind.HMCL).orElseThrow();
        assertEquals(RestartInstaller.LauncherKind.HMCL, launch.launcher());
    }
}
