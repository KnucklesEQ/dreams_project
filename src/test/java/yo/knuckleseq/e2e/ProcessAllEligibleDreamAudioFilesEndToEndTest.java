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

class ProcessAllEligibleDreamAudioFilesEndToEndTest {

    @Test
    void givenEligibleDreamAudioFilesWhenRunningProcessEndToEndThenTheBatchProducesNotesAndRunTraceability(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "audio-one");
        Files.writeString(appHome.resolve("input/A-20250108-081440.wav"), "audio-two");

        try (var server = OpenAiPipelineStubServer.start(
            new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_001")),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulAnalysisJson("2026-03-14_001")),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_002")),
            new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulAnalysisJson("2026-03-14_002"))
        )) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "process",
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(appHome.resolve("notes"))),
                () -> assertTrue(Files.exists(appHome.resolve("runs"))),
                () -> assertEquals("", result.stderr())
            );
        }
    }
}
