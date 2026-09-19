package com.goosethings.tools.nametag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Parse completely before replacing the last known-good attachment snapshot. */
public final class NameTagAttachmentRepository {
    private final Path file;
    private NameTagAttachmentConfig current = NameTagAttachmentConfig.defaults();

    public NameTagAttachmentRepository(Path file) {
        this.file = file;
    }

    public NameTagAttachmentConfig current() {
        return current;
    }

    public NameTagAttachmentConfig reload() throws IOException {
        if (Files.isSymbolicLink(file.getParent()) || Files.isSymbolicLink(file)) {
            throw new IOException("Nametag attachment configuration must not be a symbolic link");
        }
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(file.getParent());
            try (var input = NameTagAttachmentConfig.class.getResourceAsStream("/nametag_attachments.json")) {
                if (input == null) {
                    throw new IOException("Missing default nametag attachment configuration");
                }
                Files.copy(input, file);
            }
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(NameTagAttachmentConfig.MAX_BYTES + 1);
            if (bytes.length > NameTagAttachmentConfig.MAX_BYTES) {
                throw new IOException("Configuration exceeds 64 KiB");
            }
            NameTagAttachmentConfig next = NameTagAttachmentConfig.parse(
                    new String(bytes, StandardCharsets.UTF_8));
            current = next;
            return next;
        }
    }
}
