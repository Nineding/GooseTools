package com.goosethings.tools.nametag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NameTagAttachmentConfigTest {
    @TempDir
    Path directory;

    @Test
    void detectiveIconsArePrivateFullBloodSnapshots() {
        var config = NameTagAttachmentConfig.defaults();
        assertEquals(4, config.attachments().size());
        var angel = config.attachments().getFirst();

        assertTrue(angel.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "detectiveCheckedAngel"),
                true));
        assertFalse(angel.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "detectiveCheckedAngel"),
                false));
        assertFalse(angel.visible(
                false,
                Set.of("players"),
                Set.of("players"),
                Set.of("players", "detectiveCheckedAngel"),
                true));
        assertFalse(angel.visible(
                true,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "detectiveCheckedAngel"),
                true));
        assertFalse(angel.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "detectiveCheckedAngel", "inTalk"),
                true));
    }

    @Test
    void seagullDetectiveSnapshotRemainsVisibleThroughMeeting() {
        var angel = NameTagAttachmentConfig.defaults().attachments().getFirst();

        assertTrue(angel.visible(
                false,
                Set.of("Seagull", "players", "seagullNametagDetective"),
                Set.of("players"),
                Set.of("players", "inTalk", "seagullNametagDetectiveAngel"),
                true));
        assertFalse(angel.visible(
                false,
                Set.of("Seagull", "players", "seagullNametagDetective"),
                Set.of("players"),
                Set.of("players", "inTalk", "seagullNametagDetectiveDemon"),
                true));
        assertFalse(angel.visible(
                false,
                Set.of("players", "seagullNametagDetective"),
                Set.of("players"),
                Set.of("players", "inTalk", "seagullNametagDetectiveAngel"),
                true));
    }

    @Test
    void magpieIconIsPrivateToMagpieAndRoleVisibleSpectators() {
        var magpie = NameTagAttachmentConfig.defaults().attachments().stream()
                .filter(attachment -> attachment.id().equals("magpie_guessed"))
                .findFirst()
                .orElseThrow();

        assertTrue(magpie.visible(
                false,
                Set.of("Magpie", "players"),
                Set.of("players"),
                Set.of("players", "inTalk", "magpieGuessedThisMeeting"),
                true));
        assertFalse(magpie.visible(
                false,
                Set.of("players"),
                Set.of("players"),
                Set.of("players", "inTalk", "magpieGuessedThisMeeting", "spectator"),
                true));
        assertFalse(magpie.visible(
                false,
                Set.of("players"),
                Set.of("players"),
                Set.of("players", "inTalk", "magpieGuessedThisMeeting", "deadInMap"),
                true));
        assertFalse(magpie.visible(
                false,
                Set.of("players"),
                Set.of("players"),
                Set.of("players", "magpieGuessedThisMeeting"),
                true));
        assertFalse(magpie.visible(
                false,
                Set.of("players"),
                Set.of("players"),
                Set.of("players", "inTalk", "magpieGuessedThisMeeting"),
                false));
        assertFalse(magpie.visible(
                true,
                Set.of("players", "inTalk", "magpieGuessedThisMeeting"),
                Set.of("players"),
                Set.of("players", "inTalk", "magpieGuessedThisMeeting"),
                true));
        assertTrue(magpie.visible(false, Set.of("spectator", "dlcDeadViewer"),
                Set.of("players"), Set.of("players", "inTalk", "magpieGuessedThisMeeting"), true, true));
        for (String role : Set.of("Goose", "Assassin", "Raven", "Detective")) {
            assertFalse(magpie.visible(false, Set.of("players", "inTalk", role),
                    Set.of("players"), Set.of("players", "inTalk", "magpieGuessedThisMeeting"), true));
        }
    }

    @Test
    void renderedTagSourceDoesNotCopyResultThroughDisguiseIdentity() {
        var angel = NameTagAttachmentConfig.defaults().attachments().getFirst();
        assertFalse(angel.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players", "detectiveCheckedAngel"),
                Set.of("players", "stealId"),
                true));
        assertTrue(angel.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "stealId", "detectiveCheckedAngel"),
                true));
    }

    @Test
    void roleVisibleSpectatorSeesPrivateDataDrivenStatusThroughMeeting() {
        var angel = NameTagAttachmentConfig.defaults().attachments().getFirst();

        assertTrue(angel.visible(
                false,
                Set.of("players", "spectator", "dlcDeadViewer"),
                Set.of("players"),
                Set.of("players", "inTalk", "detectiveCheckedAngel"),
                true,
                true));
        assertFalse(angel.visible(
                false,
                Set.of("players", "spectator", "dlcDeadViewer"),
                Set.of("players"),
                Set.of("players", "spectator", "detectiveCheckedAngel"),
                true,
                true));
    }

    @Test
    void createsDefaultFileAndRetainsLastGoodSnapshot() throws Exception {
        Path file = directory.resolve("goosetools/nametag_attachments.json");
        var repository = new NameTagAttachmentRepository(file);
        var original = repository.reload();
        assertTrue(Files.isRegularFile(file));
        assertEquals(4, original.attachments().size());

        Files.writeString(file, Files.readString(file).replace("\"width\": 10.0", "\"width\": 12.0"));
        var updated = repository.reload();
        assertEquals(12.0F, updated.attachments().getFirst().width());

        Files.writeString(file, "{broken");
        assertThrows(RuntimeException.class, repository::reload);
        assertSame(updated, repository.current());
    }

    @Test
    void brokerShackledIconIsPrivateToBrokerOnlyDuringMeetings() {
        var broker = NameTagAttachmentConfig.defaults().attachments().stream()
                .filter(attachment -> attachment.id().equals("broker_shackled"))
                .findFirst()
                .orElseThrow();

        assertTrue(broker.visible(
                false,
                Set.of("Broker", "players"),
                Set.of("players"),
                Set.of("players", "inTalk", "brokerShackled"),
                true));
        assertTrue(broker.visible(
                false,
                Set.of("Seagull", "players", "seagullNametagBroker"),
                Set.of("players"),
                Set.of("players", "inTalk", "brokerShackled"),
                true));
        assertFalse(broker.visible(
                false,
                Set.of("Detective", "players"),
                Set.of("players"),
                Set.of("players", "inTalk", "brokerShackled"),
                true));
        assertFalse(broker.visible(
                true,
                Set.of("Broker", "players"),
                Set.of("players"),
                Set.of("players", "inTalk", "brokerShackled"),
                true));
    }

    @Test
    void rejectsUnsafeTexturesUnknownFieldsAndInvalidSizes() {
        String json = FilesUnchecked.defaultJson();
        assertThrows(RuntimeException.class, () -> NameTagAttachmentConfig.parse(
                json.replace("textures/item/angelring.png", "textures/../secret.png")));
        assertThrows(RuntimeException.class, () -> NameTagAttachmentConfig.parse(
                json.replace("\"width\": 10.0", "\"width\": 0.0")));
        assertThrows(RuntimeException.class, () -> NameTagAttachmentConfig.parse(
                json.replace("\"color\": \"#FFFFFF\"", "\"colour\": \"#FFFFFF\"")));
        assertThrows(RuntimeException.class, () -> NameTagAttachmentConfig.parse(json + "{}"));
    }

    private static final class FilesUnchecked {
        private FilesUnchecked() {
        }

        private static String defaultJson() {
            try (var input = NameTagAttachmentConfig.class.getResourceAsStream("/nametag_attachments.json")) {
                if (input == null) {
                    throw new IllegalStateException("Missing test resource");
                }
                return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
