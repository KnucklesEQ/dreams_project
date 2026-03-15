package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValidateLocalApplicationEnvironmentEndToEndTest {

    @Test
    void givenValidLocalConfigurationWhenRunningDoctorEndToEndThenItReportsSuccessfulLocalReadiness(@TempDir Path tempDir)
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
}
