package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializeApplicationHomeDirectoryTest {

    @Test
    void givenExplicitHomeDirectoryWhenInitializingApplicationHomeThenItCreatesTheExpectedFolderLayout(@TempDir Path tempDir)
        throws Exception {
        var appHome = tempDir.resolve("sonolog-home");

        var result = MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.isDirectory(appHome.resolve("config"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("input"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("archive"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("notes"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("workspace"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("runs"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("logs"))),
            () -> assertTrue(Files.isDirectory(appHome.resolve("tmp")))
        );
    }

    @Test
    void givenExplicitHomeDirectoryWhenInitializingApplicationHomeThenItCreatesTheConfigFileWhenMissing(@TempDir Path tempDir)
        throws Exception {
        var appHome = tempDir.resolve("sonolog-home");

        var result = MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.exists(appHome.resolve("config/config.json")))
        );
    }

    @Test
    void givenExplicitHomeDirectoryWhenInitializingApplicationHomeThenItWritesTheDefaultConfigContract(@TempDir Path tempDir)
        throws Exception {
        var appHome = tempDir.resolve("sonolog-home");

        MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

        var config = Files.readString(appHome.resolve("config/config.json"));

        assertAll(
            () -> assertTrue(config.contains("\"schemaVersion\": 1")),
            () -> assertTrue(config.contains("\"appHomeDir\"")),
            () -> assertTrue(config.contains("\"paths\"")),
            () -> assertTrue(config.contains("\"openai\"")),
            () -> assertTrue(config.contains("\"pipeline\"")),
            () -> assertTrue(config.contains("\"execution\"")),
            () -> assertTrue(config.contains("env:OPENAI_API_KEY")),
            () -> assertTrue(config.contains(appHome.toString()))
        );
    }

    @Test
    void givenExplicitHomeDirectoryWhenInitializingApplicationHomeThenItLeavesUnrelatedFilesUntouched(@TempDir Path tempDir)
        throws Exception {
        var appHome = tempDir.resolve("sonolog-home");
        var unrelatedFile = tempDir.resolve("keep.txt");
        Files.writeString(unrelatedFile, "keep-me");

        var result = MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.exists(unrelatedFile)),
            () -> assertEquals("keep-me", Files.readString(unrelatedFile))
        );
    }

    @Nested
    class ResolveTargetHomeDirectory {

        @Test
        void givenMissingHomeOptionWhenResolvingTargetHomeDirectoryThenTheDefaultHomeLocationIsUsed(@TempDir Path tempDir)
            throws Exception {
            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                List.of("-Duser.home=" + tempDir),
                "init"
            );

            var defaultHome = tempDir.resolve("Documents/sonolog");

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(Files.exists(defaultHome.resolve("config/config.json")))
            );
        }

        @Test
        void givenInvalidTargetHomePathWhenResolvingTargetHomeDirectoryThenInitializationFails(@TempDir Path tempDir)
            throws Exception {
            var blockedParent = tempDir.resolve("blocked-parent");
            Files.writeString(blockedParent, "not-a-directory");

            var result = MainCommandLineTestSupport.runMain(
                "init",
                "--home",
                blockedParent.resolve("sonolog-home").toString()
            );

            assertAll(
                () -> assertEquals(2, result.exitCode()),
                () -> assertFalse(result.stderr().isBlank() && result.stdout().isBlank())
            );
        }
    }

    @Nested
    class HandleExistingConfiguration {

        @Test
        void givenExistingConfigurationAndForceOptionWhenHandlingConfigurationThenInitializationRewritesTheConfig(@TempDir Path tempDir)
            throws Exception {
            var appHome = tempDir.resolve("sonolog-home");
            Files.createDirectories(appHome.resolve("config"));
            Files.writeString(appHome.resolve("config/config.json"), "{\"custom\":true}");

            var result = MainCommandLineTestSupport.runMain(
                "init",
                "--home",
                appHome.toString(),
                "--force"
            );

            var rewrittenConfig = Files.readString(appHome.resolve("config/config.json"));

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertTrue(rewrittenConfig.contains("\"schemaVersion\": 1")),
                () -> assertFalse(rewrittenConfig.contains("\"custom\":true"))
            );
        }

        @Test
        void givenExistingConfigurationWithoutForceWhenHandlingConfigurationThenInitializationFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = tempDir.resolve("sonolog-home");
            Files.createDirectories(appHome.resolve("config"));
            var configPath = appHome.resolve("config/config.json");
            Files.writeString(configPath, "{\"custom\":true}");

            var result = MainCommandLineTestSupport.runMain("init", "--home", appHome.toString());

            assertAll(
                () -> assertEquals(4, result.exitCode()),
                () -> assertEquals("{\"custom\":true}", Files.readString(configPath))
            );
        }
    }
}
