package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayDreamItemStatusTest {

    @Test
    void givenKnownDreamIdWhenDisplayingStatusThenItDisplaysTheCurrentTopLevelStatus(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");

        var result = MainCommandLineTestSupport.runMain(
            "status",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains(fixture.dreamId())),
            () -> assertTrue(result.stdout().contains("note_built"))
        );
    }

    @Test
    void givenKnownDreamIdWhenDisplayingStatusThenItHighlightsStaleSignalsWhenTheyExist(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        var staleManifest = Files.readString(fixture.manifestPath())
            .replace("\"status\": \"note_built\"", "\"status\": \"stale\"")
            .replace("\"buildNote\": {\n                  \"status\": \"completed\"", "\"buildNote\": {\n                  \"status\": \"stale\"");
        Files.writeString(fixture.manifestPath(), staleManifest);

        var result = MainCommandLineTestSupport.runMain(
            "status",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertTrue(result.stdout().contains("stale"));
    }

    @Test
    void givenProtectedNoteWhenDisplayingStatusThenItHighlightsTheProtectionSignal(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        Files.writeString(fixture.notePath(), Files.readString(fixture.notePath()) + "\nmanual edit\n");

        var result = MainCommandLineTestSupport.runMain(
            "status",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertTrue(result.stdout().contains("protected") || result.stdout().contains("protected_note") || result.stdout().contains("proteg"));
    }

    @Test
    void givenKnownDreamIdWhenDisplayingStatusThenItRendersConciseHumanReadableOutput(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");

        var result = MainCommandLineTestSupport.runMain(
            "status",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertFalse(result.stdout().isBlank()),
            () -> assertEquals("", result.stderr())
        );
    }

    @Nested
    class LookupRequestedDreamItem {

        @Test
        void givenKnownDreamIdWhenLookingUpTheRequestedDreamItemThenTheStatusIsDisplayed(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                "status",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenUnknownDreamIdWhenLookingUpTheRequestedDreamItemThenTheCommandFailsClearly(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                "status",
                "2026-03-14_999",
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(2, result.exitCode()),
                () -> assertFalse(result.stderr().isBlank() && result.stdout().isBlank())
            );
        }
    }
}
