package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeCleanedDreamNarrativeEndToEndTest {

    @Test
    void givenCleanedDreamWhenRunningAnalyzeEndToEndThenItWritesTheCanonicalAnalysisArtifact(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

        try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulAnalysisJson(fixture.dreamId()))) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "analyze",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(fixture.analysisPath())),
                () -> assertEquals("", result.stderr())
            );
        }
    }
}
