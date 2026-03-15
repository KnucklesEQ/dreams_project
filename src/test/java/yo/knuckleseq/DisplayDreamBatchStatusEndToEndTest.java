package yo.knuckleseq;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayDreamBatchStatusEndToEndTest {

    @Test
    void givenKnownDreamItemsWhenRunningStatusEndToEndThenItPrintsTheBatchStatusView(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_002");

        var result = MainCommandLineTestSupport.runMain("status", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("2026-03-14_001")),
            () -> assertTrue(result.stdout().contains("2026-03-14_002")),
            () -> assertEquals("", result.stderr())
        );
    }
}
