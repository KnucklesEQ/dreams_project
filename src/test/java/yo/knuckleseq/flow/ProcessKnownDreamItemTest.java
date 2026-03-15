package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;
import yo.knuckleseq.support.stubs.LlmJsonStubServer;
import yo.knuckleseq.support.stubs.OpenAiPipelineStubServer;
import yo.knuckleseq.support.stubs.TranscriptionStubServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessKnownDreamItemTest {

    @Test
    void givenImportedDreamItemWhenProcessingThenTheRemainingStagesRunAndTheRunSummaryIsWritten(@TempDir Path tempDir)
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
                () -> assertTrue(latestRunJsonPath(appHome).toString().endsWith("run.json"))
            );
        }
    }

    @Nested
    class DetermineStageSelection {

        @Test
        void givenFreshStagesWhenDeterminingStageSelectionThenOnlyTheRemainingStagesAreRun(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "process",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenMissingUpstreamArtifactsWhenDeterminingStageSelectionThenTheCommandFailsClearly(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");
            Files.deleteIfExists(fixture.archivedAudioPath());

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "process",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(2, result.exitCode());
        }
    }

    @Nested
    class HandleStageFailures {

        @Test
        void givenADownstreamFailureWhenHandlingStageFailuresThenSuccessfulUpstreamArtifactsRemainAvailable(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

            try (var server = OpenAiPipelineStubServer.start(
                new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId())),
                new OpenAiPipelineStubServer.StubResponse(500, "{\"error\":\"analysis failed\"}")
            )) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "process",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertAll(
                    () -> assertTrue(Files.exists(fixture.transcriptPath())),
                    () -> assertTrue(Files.exists(fixture.cleanedPath())),
                    () -> assertFalse(Files.exists(fixture.notePath()))
                );
            }
        }

        @Test
        void givenADownstreamFailureWhenHandlingStageFailuresThenTheItemIsMarkedAsPartialOrFailed(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

            try (var server = OpenAiPipelineStubServer.start(
                new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId())),
                new OpenAiPipelineStubServer.StubResponse(500, "{\"error\":\"analysis failed\"}")
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

                var manifest = Files.readString(fixture.manifestPath());
                var runJson = Files.readString(latestRunJsonPath(appHome));

                assertAll(
                    () -> assertEquals(1, result.exitCode()),
                    () -> assertTrue(manifest.contains("\"status\": \"failed\"") || manifest.contains("\"status\": \"stale\"")),
                    () -> assertTrue(runJson.contains("partial") || runJson.contains("failed"))
                );
            }
        }
    }

    private Path latestRunJsonPath(Path appHome) throws IOException {
        try (Stream<Path> paths = Files.walk(appHome.resolve("runs"))) {
            return paths
                .filter(path -> path.getFileName().toString().equals("run.json"))
                .findFirst()
                .orElseThrow();
        }
    }
}
