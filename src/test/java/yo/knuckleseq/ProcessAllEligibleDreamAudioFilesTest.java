package yo.knuckleseq;

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

class ProcessAllEligibleDreamAudioFilesTest {

    @Test
    void givenEligibleDreamAudioFilesWhenProcessingAllThenAllEligibleItemsAreProcessedAndTheBatchRunSummaryIsWritten(@TempDir Path tempDir)
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
                () -> assertTrue(countMarkdownNotes(appHome) >= 2),
                () -> assertTrue(latestRunJsonPath(appHome).toString().endsWith("run.json"))
            );
        }
    }

    @Nested
    class HandleBatchContinuationPolicy {

        @Test
        void givenContinueOnErrorEnabledWhenHandlingBatchContinuationThenLaterItemsContinueAfterAFailure(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "audio-one");
            Files.writeString(appHome.resolve("input/A-20250108-081440.wav"), "audio-two");

            try (var server = OpenAiPipelineStubServer.start(
                new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_001")),
                new OpenAiPipelineStubServer.StubResponse(500, "{\"error\":\"analysis failed\"}"),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_002")),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulAnalysisJson("2026-03-14_002"))
            )) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "process",
                    "--config",
                    configPath.toString()
                );

                assertTrue(countMarkdownNotes(appHome) >= 1);
            }
        }

        @Test
        void givenAFailedItemWhenHandlingBatchContinuationThenTheFailureStillAppearsInTotalsAndErrors(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "audio-one");
            Files.writeString(appHome.resolve("input/A-20250108-081440.wav"), "audio-two");

            try (var server = OpenAiPipelineStubServer.start(
                new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_001")),
                new OpenAiPipelineStubServer.StubResponse(500, "{\"error\":\"analysis failed\"}"),
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

                var runJson = Files.readString(latestRunJsonPath(appHome));

                assertAll(
                    () -> assertEquals(1, result.exitCode()),
                    () -> assertTrue(runJson.contains("failed") || runJson.contains("partial")),
                    () -> assertTrue(runJson.contains("errors") || runJson.contains("warnings"))
                );
            }
        }
    }

    @Nested
    class SelectEligibleItems {

        @Test
        void givenEligibleAndIneligibleInputsWhenSelectingEligibleItemsThenOnlyEligibleItemsAreProcessed(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "audio-one");
            Files.writeString(appHome.resolve("input/not-audio.txt"), "ignore-me");

            try (var server = OpenAiPipelineStubServer.start(
                new OpenAiPipelineStubServer.StubResponse(200, TranscriptionStubServer.successfulVerboseJsonResponse()),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulCleanupJson("2026-03-14_001")),
                new OpenAiPipelineStubServer.StubResponse(200, LlmJsonStubServer.successfulAnalysisJson("2026-03-14_001"))
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
                    () -> assertEquals(1, countMarkdownNotes(appHome)),
                    () -> assertTrue(Files.exists(appHome.resolve("input/not-audio.txt")))
                );
            }
        }

        @Test
        void givenNoEligibleItemsWhenSelectingEligibleItemsThenTheCommandBehavesLikeASafeNoOp(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/not-audio.txt"), "ignore-me");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "process",
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(0, countMarkdownNotes(appHome))
            );
        }
    }

    private long countMarkdownNotes(Path appHome) throws IOException {
        try (Stream<Path> paths = Files.list(appHome.resolve("notes"))) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".md")).count();
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
