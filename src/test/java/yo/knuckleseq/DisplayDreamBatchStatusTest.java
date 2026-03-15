package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayDreamBatchStatusTest {

    @Test
    void givenKnownDreamItemsWhenDisplayingBatchStatusThenItDisplaysAllKnownItems(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_002");

        var result = MainCommandLineTestSupport.runMain("status", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("2026-03-14_001")),
            () -> assertTrue(result.stdout().contains("2026-03-14_002"))
        );
    }

    @Test
    void givenFailedFilterWhenDisplayingBatchStatusThenOnlyFailedItemsAreShown(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var failedItem = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_002");
        var failedManifest = Files.readString(failedItem.manifestPath())
            .replace("\"status\": \"note_built\"", "\"status\": \"failed\"")
            .replace("\"lastError\": null", "\"lastError\": {\"code\": \"openai_request_failed\", \"message\": \"boom\", \"retryable\": true, \"occurredAt\": \"2026-03-14T09:16:10Z\"}");
        Files.writeString(failedItem.manifestPath(), failedManifest);

        var result = MainCommandLineTestSupport.runMain("status", "--failed", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("2026-03-14_001")),
            () -> assertFalse(result.stdout().contains("2026-03-14_002"))
        );
    }

    @Test
    void givenProtectedFilterWhenDisplayingBatchStatusThenOnlyProtectedItemsAreShown(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var protectedItem = SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");
        SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_002");
        Files.writeString(protectedItem.notePath(), Files.readString(protectedItem.notePath()) + "\nmanual edit\n");

        var result = MainCommandLineTestSupport.runMain("status", "--protected", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("2026-03-14_001")),
            () -> assertFalse(result.stdout().contains("2026-03-14_002"))
        );
    }

    @Test
    void givenBatchStatusRequestWhenDisplayingBatchStatusThenItRendersConciseHumanReadableOutput(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        SonologTestFixtures.createCompletedDreamItem(appHome, "2026-03-14_001");

        var result = MainCommandLineTestSupport.runMain("status", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertFalse(result.stdout().isBlank()),
            () -> assertEquals("", result.stderr())
        );
    }
}
