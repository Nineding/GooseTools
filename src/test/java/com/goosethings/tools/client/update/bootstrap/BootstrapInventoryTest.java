package com.goosethings.tools.client.update.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapInventoryTest {
    @TempDir Path root;
    private Path instance;
    private void prepare() throws Exception {
        instance = root.resolve("CustomInstanceName");
        Files.createDirectories(instance); Files.createDirectories(root.resolve("mods"));
        Files.writeString(instance.resolve("CustomInstanceName.json"), """
            {"id":"CustomInstanceName","patches":[{"id":"game","version":"26.3"}],
             "libraries":[{"name":"net.fabricmc:fabric-loader:0.19.5"}]}
            """);
        jar("goosetools.jar", """
            {"id":"goosetools","version":"1.14.0+Alpha0.24","custom":{"goosetools:protocol":29}}
            """, Map.of());
    }
    private static byte[] archive(String metadata, Map<String, byte[]> nested) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json")); zip.write(metadata.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            for (var child : nested.entrySet()) { zip.putNextEntry(new ZipEntry(child.getKey())); zip.write(child.getValue()); zip.closeEntry(); }
        }
        return bytes.toByteArray();
    }
    private void jar(String name, String metadata, Map<String, byte[]> nested) throws Exception {
        Files.write(root.resolve("mods").resolve(name), archive(metadata, nested));
    }
    @Test void readsMinecraftFromGamePatchRatherThanInstanceLabel() throws Exception {
        prepare(); var inventory = BootstrapInventory.read(root, instance);
        assertEquals("26.3", inventory.installed().get("minecraft"));
        assertEquals("0.19.5", inventory.installed().get("fabricloader"));
        assertEquals("1.14.0+Alpha0.24", inventory.version()); assertEquals(29, inventory.protocol());
    }
    @Test void inventoriesDeclaredNestedFabricApiModulesAndAliases() throws Exception {
        prepare();
        byte[] child = archive("{\"id\":\"fabric-networking-api-v1\",\"version\":\"5.0.0\",\"provides\":[\"network-alias\"]}", Map.of());
        jar("api.jar", """
            {"id":"fabric-api","version":"0.161.0+26.3","jars":[{"file":"META-INF/jars/network.jar"}]}
            """, Map.of("META-INF/jars/network.jar", child));
        var inventory = BootstrapInventory.read(root, instance);
        assertEquals("5.0.0", inventory.installed().get("fabric-networking-api-v1"));
        assertEquals("5.0.0", inventory.installed().get("network-alias"));
    }
    @Test void duplicateGooseToolsJarsAreRejected() throws Exception {
        prepare(); Files.copy(root.resolve("mods/goosetools.jar"), root.resolve("mods/duplicate.jar"));
        assertThrows(IOException.class, () -> BootstrapInventory.read(root, instance));
    }
    @Test void serverOnlyDependenciesAreNotClaimedOnClient() throws Exception {
        prepare(); jar("server.jar", "{\"id\":\"server-only\",\"version\":\"1.0.0\",\"environment\":\"server\"}", Map.of());
        assertFalse(BootstrapInventory.read(root, instance).installed().containsKey("server-only"));
    }
    @Test void missingLauncherVersionIsRejected() throws Exception {
        prepare(); Files.writeString(instance.resolve("CustomInstanceName.json"), "{\"id\":\"CustomInstanceName\"}");
        assertThrows(IOException.class, () -> BootstrapInventory.read(root, instance));
    }
    @Test void existingUserHookIsPreservedByteForByte() throws Exception {
        prepare(); Path config = instance.resolve(".hmcl/config/instance-game-settings.json");
        Files.createDirectories(config.getParent());
        String original = "{\"preLaunchCommand\":\"custom command\",\"overrideProperties\":[\"runningDirectory\"]}";
        Files.writeString(config, original);
        assertEquals(HmclIntegration.Result.EXISTING_COMMAND, HmclIntegration.connect(root, instance));
        assertEquals(original, Files.readString(config));
    }
}
