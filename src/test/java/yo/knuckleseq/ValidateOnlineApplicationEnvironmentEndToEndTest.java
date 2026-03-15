package yo.knuckleseq;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidateOnlineApplicationEnvironmentEndToEndTest {

    @Test
    void givenReachableRemoteServiceAndSupportedModelsWhenRunningDoctorOnlineEndToEndThenItReportsSuccessfulOnlineReadiness(@TempDir Path tempDir)
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
}
