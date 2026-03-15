package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectExternallyEditedDreamNoteTest {

    @Test
    void givenProtectedNoteWhenBuildingTheNoteThenTheOverwriteStopsBeforeWriting(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        Files.writeString(fixture.notePath(), Files.readString(fixture.notePath()) + "\nmanual edit\n");
        var noteBefore = Files.readString(fixture.notePath());

        var result = MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertAll(
            () -> assertEquals(4, result.exitCode()),
            () -> assertEquals(noteBefore, Files.readString(fixture.notePath()))
        );
    }

    @Test
    void givenProtectedNoteWhenBuildingTheNoteThenAProtectionErrorIsReported(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        Files.writeString(fixture.notePath(), Files.readString(fixture.notePath()) + "\nmanual edit\n");

        var result = MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertTrue(
            result.stdout().contains("note_overwrite_protected")
                || result.stderr().contains("note_overwrite_protected")
                || result.stdout().contains("proteg")
                || result.stderr().contains("proteg")
        );
    }

    @Nested
    class HandleProtection {

        @Test
        void givenProtectedNoteAndForceOptionWhenHandlingProtectionThenTheNoteIsOverwrittenAndTheStoredHashIsRefreshed(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
            Files.writeString(fixture.notePath(), Files.readString(fixture.notePath()) + "\nmanual edit\n");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--force",
                "--config",
                configPath.toString()
            );

            var manifest = Files.readString(fixture.manifestPath());

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(manifest.contains("\"noteHash\""))
            );
        }

        @Test
        void givenProtectedNoteWithoutForceWhenHandlingProtectionThenTheNoteIsNotOverwritten(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
            Files.writeString(fixture.notePath(), Files.readString(fixture.notePath()) + "\nmanual edit\n");
            var noteBefore = Files.readString(fixture.notePath());

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(4, result.exitCode()),
                () -> assertEquals(noteBefore, Files.readString(fixture.notePath()))
            );
        }

        @Test
        void givenExistingNoteWithoutStoredGeneratedHashWhenHandlingProtectionThenTheNoteIsTreatedAsProtected(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
            var manifestWithoutNoteHash = Files.readString(fixture.manifestPath())
                .replace("\"noteHash\": \"sha256:note\"", "\"noteHash\": null");
            Files.writeString(fixture.manifestPath(), manifestWithoutNoteHash);

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(4, result.exitCode());
        }
    }
}
