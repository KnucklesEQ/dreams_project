package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;
import yo.knuckleseq.support.stubs.LlmJsonStubServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanDreamTranscriptTest {

    @Test
    void givenTranscribedDreamWhenCleaningThenTheCanonicalCleanedArtifactIsWritten(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

        try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId()))) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "clean",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(fixture.cleanedPath())),
                () -> assertTrue(Files.exists(fixture.cleanedPath().getParent().resolve("raw-response.json")))
            );
        }
    }

    @Test
    void givenTranscribedDreamWhenCleaningThenTheManifestStageStateIsUpdated(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

        try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId()))) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "clean",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            var manifest = Files.readString(fixture.manifestPath());

            assertAll(
                () -> assertTrue(manifest.contains("\"clean\": {")),
                () -> assertTrue(manifest.contains("\"status\": \"completed\"")),
                () -> assertTrue(manifest.contains("\"outputPath\""))
            );
        }
    }

    @Nested
    class HandleStageFreshness {

        @Test
        void givenFreshCleanupStageAndForceOptionWhenHandlingStageFreshnessThenTheStageIsRerun(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId()))) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "clean",
                    fixture.dreamId(),
                    "--force",
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenFreshCleanupStageWithoutForceWhenHandlingStageFreshnessThenTheStageIsSkipped(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createCleanedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "clean",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }
    }

    @Nested
    class ValidateCleanupOutput {

        @Test
        void givenValidConservativeJsonWhenValidatingCleanupOutputThenItBecomesCanonical(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.successfulCleanupJson(fixture.dreamId()))) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "clean",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenMalformedJsonWhenValidatingCleanupOutputThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, "{not-valid-json")) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "clean",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }

        @Test
        void givenJsonWithUnknownKeysWhenValidatingCleanupOutputThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.cleanupJsonWithUnknownKey(fixture.dreamId()))) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "clean",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }

        @Test
        void givenHallucinatedContentWhenValidatingCleanupOutputThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            try (var server = LlmJsonStubServer.start(200, LlmJsonStubServer.cleanupJsonWithHallucination(fixture.dreamId()))) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "clean",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }
    }
}
