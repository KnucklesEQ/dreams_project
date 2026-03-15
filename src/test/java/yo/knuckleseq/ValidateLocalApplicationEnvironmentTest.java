package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateLocalApplicationEnvironmentTest {

    @Test
    void givenExplicitConfigPathWhenValidatingLocalEnvironmentThenTheRequestedConfigFileIsUsed(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);

        var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

        assertEquals(0, result.exitCode());
    }

    @Test
    void givenValidConfigurationWhenValidatingLocalEnvironmentThenTheApiKeyReferenceFormatIsAccepted(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);

        var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

        assertEquals(0, result.exitCode());
    }

    @Test
    void givenValidConfigurationWhenValidatingLocalEnvironmentThenItReportsSuccessfulReadiness(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);

        var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertFalse(result.stdout().isBlank()),
            () -> assertEquals("", result.stderr())
        );
    }

    @Nested
    class ValidateConfiguration {

        @Test
        void givenValidConfigurationWhenValidatingConfigurationThenTheLocalChecksPass(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenMissingConfigurationFileWhenValidatingConfigurationThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var missingConfigPath = tempDir.resolve("missing/config.json");

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", missingConfigPath.toString());

            assertAll(
                () -> assertEquals(2, result.exitCode()),
                () -> assertFalse(result.stderr().isBlank() && result.stdout().isBlank())
            );
        }

        @Test
        void givenConfigurationWithUnknownKeysWhenValidatingConfigurationThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome).replace(
                    "\"execution\": {",
                    "\"unexpected\": true,\n  \"execution\": {"
                )
            );

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

            assertEquals(2, result.exitCode());
        }

        @Test
        void givenConfigurationWithInvalidValuesWhenValidatingConfigurationThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome)
                    .replace("\"locale\": \"es\"", "\"locale\": \"en\"")
                    .replace("\"timeoutSeconds\": 120", "\"timeoutSeconds\": -1")
            );

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

            assertEquals(2, result.exitCode());
        }
    }

    @Nested
    class ValidateFilesystemReadiness {

        @Test
        void givenMissingRuntimeDirectoriesWhenValidatingFilesystemReadinessThenDirectoriesThatCanBeCreatedAreAccepted(@TempDir Path tempDir)
            throws Exception {
            var appHome = tempDir.resolve("sonolog-home");
            Files.createDirectories(appHome.resolve("config"));
            var configPath = SonologTestFixtures.writeValidConfig(appHome);

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenARequiredDirectoryThatIsNotWritableWhenValidatingFilesystemReadinessThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var blockedPath = tempDir.resolve("blocked-notes");
            Files.writeString(blockedPath, "not-a-directory");
            var configPath = SonologTestFixtures.writeConfig(
                appHome,
                SonologTestFixtures.validConfigJson(appHome).replace(
                    appHome.resolve("notes").toString(),
                    blockedPath.toString()
                )
            );

            var result = MainCommandLineTestSupport.runMain("doctor", "--config", configPath.toString());

            assertEquals(2, result.exitCode());
        }
    }
}
