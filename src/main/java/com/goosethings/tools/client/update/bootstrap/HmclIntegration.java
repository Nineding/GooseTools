package com.goosethings.tools.client.update.bootstrap;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Only modifies this instance's hook and preserves every unrelated HMCL property. */
public final class HmclIntegration {
    public enum Result { CONNECTED, EXISTING_COMMAND, NOT_HMCL }
    private HmclIntegration() { }

    public static Result connect(Path game, Path instance) throws Exception {
        Path settings = instance.resolve(".hmcl/config/instance-game-settings.json");
        if (!Files.isRegularFile(settings)) return Result.NOT_HMCL;
        BootstrapFiles.safe(settings);
        JsonObject values = JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
        String previous = values.has("preLaunchCommand") && !values.get("preLaunchCommand").isJsonNull()
                ? values.get("preLaunchCommand").getAsString().trim() : "";
        String command = command();
        if (!previous.isEmpty() && !previous.equals(command)) return Result.EXISTING_COMMAND;
        prepare(game);
        JsonArray overrides = values.has("overrideProperties") ? values.getAsJsonArray("overrideProperties") : new JsonArray();
        if (!overrides.contains(new JsonPrimitive("preLaunchCommand"))) overrides.add("preLaunchCommand");
        values.add("overrideProperties", overrides);
        values.addProperty("preLaunchCommand", command);
        Path backup = settings.resolveSibling("instance-game-settings.before-goosetools.json");
        if (!Files.exists(backup)) Files.copy(settings, backup);
        Path temporary = Files.createTempFile(settings.getParent(), ".goosetools-", ".json");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(values) + "\n", StandardCharsets.UTF_8);
            try { Files.move(temporary, settings, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, settings, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        return Result.CONNECTED;
    }

    public static String command() {
        return "\"$INST_JAVA\" -Djava.net.useSystemProxies=true -jar \"$INST_MC_DIR/config/goosetools/bootstrap/goosetools-updater.jar\""
                + " --game-dir \"$INST_MC_DIR\" --instance-dir \"$INST_DIR\"";
    }

    public static void prepare(Path game) throws IOException {
        Path helper = game.resolve("config/goosetools/bootstrap/goosetools-updater.jar");
        BootstrapFiles.safe(helper);
        Files.createDirectories(helper.getParent());
        Path temporary = Files.createTempFile(helper.getParent(), ".helper-", ".jar");
        try (var input = HmclIntegration.class.getResourceAsStream("/META-INF/goosetools/updater.jar")) {
            if (input == null) throw new IOException("Embedded launcher updater is missing");
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, helper, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, helper, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

}
