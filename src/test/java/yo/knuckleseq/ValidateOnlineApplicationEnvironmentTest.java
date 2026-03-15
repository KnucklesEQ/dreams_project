package yo.knuckleseq;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidateOnlineApplicationEnvironmentTest {

    @Test
    void givenOnlineValidationFlagWhenValidatingEnvironmentThenSuccessfulLocalChecksRemainRequired(@TempDir Path tempDir)
        throws Exception {
        var missingConfigPath = tempDir.resolve("missing/config.json");

        var result = MainCommandLineTestSupport.runMain("doctor", "--online", "--config", missingConfigPath.toString());

        assertEquals(2, result.exitCode());
    }

    @Test
    void givenResolvedApiKeyReferenceWhenValidatingEnvironmentThenTheOnlineChecksCanUseTheConfiguredApiKey(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);

        try (var server = OpenAiApiStubServer.startWithModels("whisper-1", "gpt-4.1-mini")) {
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome).replace(
                    "https://api.openai.com/v1",
                    server.baseUrl()
                )
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "doctor",
                "--online",
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }
    }

    @Test
    void givenSuccessfulOnlineValidationWhenValidatingEnvironmentThenItReportsOnlineReadiness(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);

        try (var server = OpenAiApiStubServer.startWithModels("whisper-1", "gpt-4.1-mini")) {
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome).replace(
                    "https://api.openai.com/v1",
                    server.baseUrl()
                )
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "doctor",
                "--online",
                "--config",
                configPath.toString()
            );

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertFalse(result.stdout().isBlank()),
                () -> assertEquals("", result.stderr())
            );
        }
    }

    @Nested
    class ValidateOnlineChecks {

        @Test
        void givenReachableRemoteServiceAndSupportedModelsWhenValidatingOnlineChecksThenTheCommandSucceeds(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);

            try (var server = OpenAiApiStubServer.startWithModels("whisper-1", "gpt-4.1-mini")) {
                var configPath = SonologTestFixtures.writeConfig(
                    appHome,
                    SonologTestFixtures.validConfigJson(appHome).replace(
                        "https://api.openai.com/v1",
                        server.baseUrl()
                    )
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "doctor",
                    "--online",
                    "--config",
                    configPath.toString()
                );

                assertEquals(0, result.exitCode());
            }
        }

        @Test
        void givenMissingApiKeyWhenValidatingOnlineChecksThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);

            try (var server = OpenAiApiStubServer.startWithModels("whisper-1", "gpt-4.1-mini")) {
                var configPath = SonologTestFixtures.writeConfig(
                    appHome,
                    SonologTestFixtures.validConfigJson(appHome).replace(
                        "https://api.openai.com/v1",
                        server.baseUrl()
                    )
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    "doctor",
                    "--online",
                    "--config",
                    configPath.toString()
                );

                assertEquals(2, result.exitCode());
            }
        }

        @Test
        void givenUnreachableRemoteServiceWhenValidatingOnlineChecksThenTheCommandFails(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome).replace(
                    "https://api.openai.com/v1",
                    "http://127.0.0.1:1/v1"
                )
            );

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                Map.of("OPENAI_API_KEY", "test-api-key"),
                "doctor",
                "--online",
                "--config",
                configPath.toString()
            );

            assertEquals(3, result.exitCode());
        }

        @Test
        void givenRejectedModelConfigurationWhenValidatingOnlineChecksThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);

            try (var server = OpenAiApiStubServer.startWithModels("whisper-1")) {
                var configPath = SonologTestFixtures.writeConfig(
                    appHome,
                    SonologTestFixtures.validConfigJson(appHome).replace(
                        "https://api.openai.com/v1",
                        server.baseUrl()
                    )
                );

                var result = MainCommandLineTestSupport.runMain(
                    tempDir,
                    Map.of("OPENAI_API_KEY", "test-api-key"),
                    "doctor",
                    "--online",
                    "--config",
                    configPath.toString()
                );

                assertEquals(3, result.exitCode());
            }
        }
    }
}
