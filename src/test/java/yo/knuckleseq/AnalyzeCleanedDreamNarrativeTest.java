package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeCleanedDreamNarrativeTest {

    @Test
    void givenCleanedDreamWhenAnalyzingThenTheCanonicalAnalysisArtifactIsWritten(@TempDir Path tempDir) throws Exception {
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
                () -> assertTrue(Files.exists(fixture.analysisPath()))
            );
        }
    }

    @Test
    void givenCleanedDreamWhenAnalyzingThenTheManifestStageStateIsUpdated(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

        try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulAnalysisJson(fixture.dreamId()))) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "analyze",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            var manifest = Files.readString(fixture.manifestPath());

            assertAll(
                () -> assertTrue(manifest.contains("\"analyze\": {")),
                () -> assertTrue(manifest.contains("\"status\": \"completed\"")),
                () -> assertTrue(manifest.contains("\"analysisPath\"") || manifest.contains("\"outputPath\""))
            );
        }
    }

    @Nested
    class HandleStageFreshness {

        @Test
        void givenFreshAnalysisStageAndForceOptionWhenHandlingStageFreshnessThenTheStageIsRerun(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

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
                    "--force",
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenFreshAnalysisStageWithoutForceWhenHandlingStageFreshnessThenTheStageIsSkipped(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "analyze",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }
    }

    @Nested
    class ValidateEvidenceBearingOutput {

        @Test
        void givenEvidenceBearingAnalysisJsonWhenValidatingAnalysisOutputThenItBecomesCanonical(@TempDir Path tempDir)
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

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenAnalysisItemsWithoutEvidenceWhenValidatingAnalysisOutputThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.analysisJsonWithoutEvidence(fixture.dreamId()))) {
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

                assertEquals(3, result.exitCode());
            }
        }

        @Test
        void givenUnsupportedValuesWhenValidatingAnalysisOutputThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.analysisJsonWithUnsupportedValues(fixture.dreamId()))) {
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

                assertEquals(3, result.exitCode());
            }
        }

        @Test
        void givenHallucinatedAnalysisOutputWhenValidatingAnalysisOutputThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.analysisJsonWithHallucination(fixture.dreamId()))) {
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

                assertEquals(3, result.exitCode());
            }
        }
    }

    @Nested
    class HandleTitleCandidate {

        @Test
        void givenSafeTitleCandidateWhenHandlingTitleCandidateThenTheAnalysisIsAccepted(@TempDir Path tempDir) throws Exception {
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

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenTitleCandidateWithoutEvidenceWhenHandlingTitleCandidateThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.analysisJsonWithoutTitleEvidence(fixture.dreamId()))) {
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

                assertEquals(3, result.exitCode());
            }
        }
    }
}
