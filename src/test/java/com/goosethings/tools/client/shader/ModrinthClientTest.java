package com.goosethings.tools.client.shader;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthClientTest {
    @Test
    void acceptsOnlyLeafJarOrZipNames() {
        assertEquals("iris.jar", ModrinthClient.safeName("iris.jar"));
        assertEquals("shader.zip", ModrinthClient.safeName("shader.zip"));
        assertThrows(SecurityException.class, () -> ModrinthClient.safeName("../iris.jar"));
        assertThrows(SecurityException.class, () -> ModrinthClient.safeName("payload.exe"));
    }

    @Test
    void selectsOnlyMissingComponents() {
        assertEquals(new ModrinthClient.Selection(false, false, false, false),
                ModrinthClient.select(true, true, true, true));
        assertEquals(new ModrinthClient.Selection(false, false, false, true),
                ModrinthClient.select(true, true, true, false));
        assertEquals(new ModrinthClient.Selection(true, false, false, false),
                ModrinthClient.select(false, true, true, true));
        assertEquals(new ModrinthClient.Selection(true, true, true, true),
                ModrinthClient.select(false, false, false, false));
    }

    @Test
    void restartIsRequiredOnlyForModArtifacts() {
        ModrinthClient.Artifact shader = artifact("complementary", "shaderpacks", "shader.zip");
        ModrinthClient.Artifact iris = artifact("iris", "mods", "iris.jar");

        assertFalse(new ModrinthClient.Plan(List.of()).requiresRestart());
        assertFalse(new ModrinthClient.Plan(List.of(shader)).requiresRestart());
        assertTrue(new ModrinthClient.Plan(List.of(iris)).requiresRestart());
        assertTrue(new ModrinthClient.Plan(List.of(shader, iris)).requiresRestart());
    }

    @Test
    void acceptsOnlyTrustedHttpsModrinthDownloadRedirects() {
        ModrinthClient.validateDownloadUri(
                URI.create("https://cdn.modrinth.com/data/project/version/file.zip"));
        ModrinthClient.validateDownloadUri(
                URI.create("https://cdn-raw.modrinth.com/data/project/version/file.zip"));
        ModrinthClient.validateDownloadUri(
                URI.create("https://cdn-alt.modrinth.com/data/project/version/file.zip"));
        assertThrows(SecurityException.class, () -> ModrinthClient.validateDownloadUri(
                URI.create("http://cdn.modrinth.com/data/project/version/file.zip")));
        assertThrows(SecurityException.class, () -> ModrinthClient.validateDownloadUri(
                URI.create("https://cdn.modrinth.com.evil.example/data/project/version/file.zip")));
        assertThrows(SecurityException.class, () -> ModrinthClient.validateDownloadUri(
                URI.create("https://cdn.modrinth.com/images/project/icon.png")));
        assertEquals("cdn-raw.modrinth.com", ModrinthClient.preferRawCdn(
                URI.create("https://cdn-alt.modrinth.com/data/project/version/file.zip")).getHost());
    }

    @Test
    void selectsComplementaryVersionRequiredByEuphoriaArtifact() {
        assertEquals("r5.9", ModrinthClient.compatibleShaderVersion(
                "EuphoriaPatcher-1.10.0-r5.9-fabric.jar"));
        assertEquals("r5.9.3", ModrinthClient.compatibleShaderVersion(
                "EuphoriaPatcher-1.10.5-r5.9.3-fabric.jar"));
        assertTrue(ModrinthClient.versionMatches("ComplementaryReimagined_r5.9.zip", "r5.9"));
        assertFalse(ModrinthClient.versionMatches("ComplementaryReimagined_r5.9.3.zip", "r5.9"));
        assertTrue(ModrinthClient.versionMatches("ComplementaryReimagined_r5.9.3.zip", "r5.9.3"));
    }

    private static ModrinthClient.Artifact artifact(String id, String folder, String filename) {
        return new ModrinthClient.Artifact(id, "1", id, folder, filename,
                URI.create("https://cdn.modrinth.com/data/example/versions/example/" + filename),
                1L, "00");
    }
}
