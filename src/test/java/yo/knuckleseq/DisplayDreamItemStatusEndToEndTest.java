package yo.knuckleseq;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayDreamItemStatusEndToEndTest {

    @Test
    void givenKnownDreamItemWhenRunningStatusEndToEndThenItPrintsTheCurrentItemStatus(@TempDir Path tempDir)
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
            () -> assertTrue(result.stdout().contains("note_built")),
            () -> assertEquals("", result.stderr())
        );
    }
}
