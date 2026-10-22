package com.goosethings.tools.client.update;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GitHubUpdateClientTest {
    private static final String RELEASE = """
            {"tag_name":"v1.14.0+Alpha0.22", "draft":false, "prerelease":true, "assets":[
              {"name":"goosetools-1.14.0+Alpha0.22-sources.jar", "size":100,
               "browser_download_url":"https://github.com/Nineding/GooseTools/releases/download/v1.14.0/goosetools.jar"},
              {"name":"goosetools-1.14.0+Alpha0.22.jar", "size":100,
               "digest":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
               "browser_download_url":"https://github.com/Nineding/GooseTools/releases/download/v1.14.0+Alpha0.22/goosetools-1.14.0+Alpha0.22.jar"}]}
            """;

    @Test void alphaUsesNumericProjectSequenceAfterStable() {
        assertTrue(UpdateVersion.parse("1.14.0+Alpha0.22").compareTo(UpdateVersion.parse("1.14.0+Alpha0.9")) > 0);
        assertTrue(UpdateVersion.parse("1.14.0+Alpha0.1").compareTo(UpdateVersion.parse("1.14.0")) > 0);
        assertTrue(UpdateVersion.parse("1.14.1").compareTo(UpdateVersion.parse("1.14.0+Alpha0.999")) > 0);
        assertThrows(IllegalArgumentException.class, () -> UpdateVersion.parse("../../evil"));
    }

    @Test void includesAlphaAndSelectsOnlyVerifiedRuntimeJar() {
        var release = JsonParser.parseString(RELEASE).getAsJsonObject();
        assertEquals("goosetools-1.14.0+Alpha0.22.jar",
                GitHubUpdateClient.release(release, "1.14.0+Alpha0.21", true, null).orElseThrow().fileName());
        assertTrue(GitHubUpdateClient.release(release, "1.14.0+Alpha0.21", false, null).isEmpty());
        assertTrue(GitHubUpdateClient.release(release, "1.14.0+Alpha0.22", true, null).isEmpty());
        release.getAsJsonArray("assets").get(1).getAsJsonObject().remove("digest");
        assertTrue(GitHubUpdateClient.release(release, "1.14.0", true, null).isEmpty());
    }

    @Test void serverCanRequestAnOlderExactOfficialVersion() {
        var release = JsonParser.parseString(RELEASE).getAsJsonObject();
        assertTrue(GitHubUpdateClient.release(release, "1.14.0+Alpha0.23", true, null).isEmpty());
        assertTrue(GitHubUpdateClient.release(release, "1.14.0+Alpha0.23", true, "1.14.0+Alpha0.22").isPresent());
        assertTrue(GitHubUpdateClient.release(release, "1.14.0", true, "1.14.0+Alpha0.20").isEmpty());
    }

    @Test void rejectsForeignRepositoriesAndNonHttpsAddresses() {
        var release = JsonParser.parseString(RELEASE.replace("Nineding/GooseTools", "attacker/GooseTools")).getAsJsonObject();
        assertTrue(GitHubUpdateClient.release(release, "1.14.0", true, null).isEmpty());
        assertFalse(GitHubUpdateClient.allowed(URI.create("http://github.com/file")));
        assertFalse(GitHubUpdateClient.allowed(URI.create("https://github.com.attacker.test/file")));
        assertFalse(GitHubUpdateClient.allowed(URI.create("https://user:password@github.com/file")));
        assertFalse(GitHubUpdateClient.allowed(URI.create("https://github.com:8443/file")));
    }

    @Test void validatesIdentityDependenciesAndProtocolBeforeInstall(@TempDir Path temp) throws Exception {
        Path jar = temp.resolve("mod.jar");
        String metadata = """
                {"id":"goosetools", "version":"1.14.0+Alpha0.22", "custom":{"goosetools:protocol":28},
                 "depends":{"minecraft":"26.3", "fabricloader":">=0.19.5", "java":">=25",
                            "xaerominimap":"26.5.3"}}
                """;
        try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Map<String, String> installed = Map.of("minecraft", "26.3", "fabricloader", "0.19.5",
                "java", "25", "xaerominimap", "26.5.3");
        assertTrue(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.22", 28, installed));
        // The standalone updater may install a new protocol before any game classes load.
        assertTrue(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.22", -1, installed));
        assertFalse(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.22", 29, installed));
        assertFalse(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.21", 28, installed));
        assertFalse(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.22", 28, Map.of("minecraft", "26.3")));
        assertFalse(GitHubUpdateClient.compatible(jar, "1.14.0+Alpha0.22", 28,
                Map.of("minecraft", "26.2", "fabricloader", "0.19.5", "java", "25", "xaerominimap", "26.5.3")));
    }

    @Test void firstInstallEnablesBothUpdatesAndAlphaAndPersistsOptOut(@TempDir Path temp) throws Exception {
        Path settings = temp.resolve("auto-update.properties");
        assertEquals(new UpdatePreferences(true, true), UpdatePreferences.load(settings));
        new UpdatePreferences(false, false).save(settings);
        assertEquals(new UpdatePreferences(false, false), UpdatePreferences.load(settings));
    }

    @Test void releaseDependenciesAcceptSupportedAndNewerVersionsWithoutUpperBounds(@TempDir Path temp) throws Exception {
        var metadata = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        metadata.addProperty("version", "1.15.1");
        Path jar = temp.resolve("runtime.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write(metadata.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var supported = Map.of("minecraft", "26.3", "fabricloader", "0.19.5", "java", "25",
                "fabric-api", "0.161.0+26.3", "xaerominimap", "26.5.3", "xaeroworldmap", "1.46.4");
        assertTrue(GitHubUpdateClient.compatible(jar, "1.15.1", 41, supported));
        var newer = new java.util.HashMap<>(supported);
        newer.putAll(Map.of("fabricloader", "0.19.6", "java", "26", "fabric-api", "0.164.0+26.3",
                "xaerominimap", "26.6.1", "xaeroworldmap", "1.47.1"));
        assertTrue(GitHubUpdateClient.compatible(jar, "1.15.1", 41, newer));
        newer.putAll(Map.of("fabricloader", "1.0.0", "fabric-api", "1.0.0+26.3",
                "xaerominimap", "99.0.0", "xaeroworldmap", "99.0.0"));
        assertTrue(GitHubUpdateClient.compatible(jar, "1.15.1", 41, newer));
        for (var belowMinimum : Map.of("fabricloader", "0.19.4", "java", "24",
                "fabric-api", "0.160.0+26.3", "xaerominimap", "26.5.2", "xaeroworldmap", "1.46.3").entrySet()) {
            var invalid = new java.util.HashMap<>(supported);
            invalid.put(belowMinimum.getKey(), belowMinimum.getValue());
            assertFalse(GitHubUpdateClient.compatible(jar, "1.15.1", 41, invalid), belowMinimum.getKey());
        }
        assertFalse(GitHubUpdateClient.compatible(jar, "1.15.1", 40, supported));
    }
}
