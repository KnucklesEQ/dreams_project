package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectExternallyEditedDreamNoteEndToEndTest {

    @Test
    void givenProtectedNoteWhenRunningBuildNoteEndToEndThenTheExistingNoteRemainsUntouched(@TempDir Path tempDir)
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
}
