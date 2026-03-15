package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;
import yo.knuckleseq.support.stubs.LlmJsonStubServer;
import yo.knuckleseq.support.stubs.OpenAiPipelineStubServer;
import yo.knuckleseq.support.stubs.TranscriptionStubServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessKnownDreamItemEndToEndTest {

    @Test
    void givenImportedDreamItemWhenRunningProcessEndToEndThenItProducesTheFinalNoteAndRunSummary(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

        try (var server = OpenAiPipelineStubServer.start(
            new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId())),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulAnalysisJson(fixture.dreamId()))
        )) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "process",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(fixture.notePath())),
                () -> assertTrue(Files.exists(appHome.resolve("runs"))),
                () -> assertEquals("", result.stderr())
            );
        }
    }
}
