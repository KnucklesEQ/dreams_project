package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializeApplicationHomeDirectoryEndToEndTest {

    @Test
    void givenInitializationRequestWhenRunningTheApplicationEndToEndThenItCreatesTheWorkspaceAndDefaultConfig(@TempDir Path tempDir)
        throws Exception {
        var appHome = tempDir.resolve("sonolog-home");
        var result = MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.isDirectory(appHome.resolve("input"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("archive"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("notes"))),
            () -> assertTrue(Files.exists(appHome.resolve("config/config.json"))),
            () -> assertEquals("", result.stderr())
        );
    }
}
