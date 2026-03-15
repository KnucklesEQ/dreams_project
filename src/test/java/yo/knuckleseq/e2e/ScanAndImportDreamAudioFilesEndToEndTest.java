package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanAndImportDreamAudioFilesEndToEndTest {

    @Test
    void givenNewRecorderAudioWhenRunningScanEndToEndThenItArchivesTheAudioAndCreatesTheManifest(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "fake-wav-audio");

        var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.exists(appHome.resolve("archive/A-20250108-071440.wav"))),
            () -> assertTrue(Files.exists(appHome.resolve("workspace"))),
            () -> assertEquals("", result.stderr())
        );
    }
}
