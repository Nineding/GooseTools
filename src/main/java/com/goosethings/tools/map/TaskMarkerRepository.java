package com.goosethings.tools.map;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Parse completely before replacing the last known-good snapshot. */
public final class TaskMarkerRepository {
    private final Path file;
    private TaskMarkerConfig current = TaskMarkerConfig.defaults();

    public TaskMarkerRepository(Path file) {
        this.file = file;
    }

    public TaskMarkerConfig current() { return current; }

    public TaskMarkerConfig reload() throws IOException {
        if (Files.isSymbolicLink(file.getParent()) || Files.isSymbolicLink(file)) {
            throw new IOException("Task marker configuration must not be a symbolic link");
        }
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(file.getParent());
            try (var input = TaskMarkerConfig.class.getResourceAsStream("/task_markers.json")) {
                if (input == null) throw new IOException("Missing default task marker configuration");
                Files.copy(input, file);
            }
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(TaskMarkerConfig.MAX_BYTES + 1);
            if (bytes.length > TaskMarkerConfig.MAX_BYTES) throw new IOException("Configuration exceeds 64 KiB");
            TaskMarkerConfig next = TaskMarkerConfig.parse(new String(bytes, StandardCharsets.UTF_8));
            current = next;
            return next;
        }
    }
}
