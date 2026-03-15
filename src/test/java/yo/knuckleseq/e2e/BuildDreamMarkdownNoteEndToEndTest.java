package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDreamMarkdownNoteEndToEndTest {

    @Test
    void givenAnalyzedDreamWhenRunningBuildNoteEndToEndThenItWritesTheMarkdownNoteContract(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

        var result = MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        var note = Files.readString(fixture.notePath());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(note.contains("type: dream")),
            () -> assertTrue(note.contains("## Version depurada")),
            () -> assertTrue(note.contains("## Trazabilidad")),
            () -> assertEquals("", result.stderr())
        );
    }
}
