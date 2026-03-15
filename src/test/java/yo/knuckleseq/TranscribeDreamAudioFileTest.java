package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscribeDreamAudioFileTest {

    @Test
    void givenImportedDreamAudioWhenTranscribingThenTheCanonicalTranscriptArtifactsAreWritten(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

        try (var server = TranscriptionStubServer.start(200, TranscriptionStubServer.successfulVerboseJsonResponse())) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "transcribe",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(fixture.transcriptPath())),
                () -> assertTrue(Files.exists(fixture.transcriptPath().getParent().resolve("segments.json"))),
                () -> assertTrue(Files.exists(fixture.transcriptPath().getParent().resolve("raw-response.json")))
            );
        }
    }

    @Test
    void givenImportedDreamAudioWhenTranscribingThenTheManifestStageStateIsUpdated(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

        try (var server = TranscriptionStubServer.start(200, TranscriptionStubServer.successfulVerboseJsonResponse())) {
            Files.writeString(
                configPath,
                Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
            );

            MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "transcribe",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            var manifest = Files.readString(fixture.manifestPath());

            assertAll(
                () -> assertTrue(manifest.contains("\"transcribe\": {")),
                () -> assertTrue(manifest.contains("\"status\": \"completed\"")),
                () -> assertTrue(manifest.contains("\"transcriptPath\""))
            );
        }
    }

    @Nested
    class HandleStageFreshness {

        @Test
        void givenFreshTranscriptionAndForceOptionWhenHandlingStageFreshnessThenTheStageIsRerun(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            try (var server = TranscriptionStubServer.start(200, TranscriptionStubServer.successfulVerboseJsonResponse())) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "transcribe",
                    fixture.dreamId(),
                    "--force",
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenFreshTranscriptionWithoutForceWhenHandlingStageFreshnessThenTheStageIsSkipped(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createTranscribedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "transcribe",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }
    }

    @Nested
    class ExecuteTranscription {

        @Test
        void givenReachableTranscriptionServiceWhenExecutingTranscriptionThenTheTranscriptArtifactsAreProduced(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

            try (var server = TranscriptionStubServer.start(200, TranscriptionStubServer.successfulVerboseJsonResponse())) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "transcribe",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenMissingArchivedAudioWhenExecutingTranscriptionThenTheCommandFailsClearly(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");
            Files.deleteIfExists(fixture.archivedAudioPath());

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "transcribe",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(2, result.exitCode());
        }

        @Test
        void givenProviderFailureWhenExecutingTranscriptionThenTheCommandFailsWithExternalServiceError(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

            try (var server = TranscriptionStubServer.start(500, "{\"error\":\"boom\"}")) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "transcribe",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }

        @Test
        void givenMalformedOrEmptyTranscriptPayloadWhenExecutingTranscriptionThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createImportedDreamItem(appHome, "2026-03-14_001");

            try (var server = TranscriptionStubServer.start(200, TranscriptionStubServer.emptyVerboseJsonResponse())) {
                Files.writeString(
                    configPath,
                    Files.readString(configPath).replace("https://api.openai.com/v1", server.baseUrl())
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "transcribe",
                    fixture.dreamId(),
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }
    }
}
