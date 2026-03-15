package yo.knuckleseq.flow;

import yo.knuckleseq.support.BuildMetadataTestSupport;
import yo.knuckleseq.support.MainCommandLineTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayApplicationHelpMessageTest {

    @Nested
    class ResolveGeneralHelpInvocation {

        @Test
        void givenHelpCommandWhenResolvingInvocationThenRequestIsAccepted() throws Exception {
            var result = MainCommandLineTestSupport.runMain("help");

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenHelpAliasWhenResolvingInvocationThenRequestIsAccepted() throws Exception {
            var result = MainCommandLineTestSupport.runMain("--help");

            assertEquals(0, result.exitCode());
        }
    }

    @Test
    void givenResolvedGeneralHelpRequestWhenRenderingOutputThenItDisplaysTheTopLevelCommandOverview() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help");
        var metadata = BuildMetadataTestSupport.load();

        assertAll(
            () -> assertTrue(result.stdout().contains(metadata.displayName())),
            () -> assertTrue(result.stdout().contains("init")),
            () -> assertTrue(result.stdout().contains("process")),
            () -> assertTrue(result.stdout().contains("status"))
        );
    }

    @Test
    void givenResolvedGeneralHelpRequestWhenRenderingOutputThenItDisplaysTheMainGlobalFlags() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help");

        assertAll(
            () -> assertTrue(result.stdout().contains("--config")),
            () -> assertTrue(result.stdout().contains("--verbose")),
            () -> assertTrue(result.stdout().contains("--dry-run"))
        );
    }

    @Test
    void givenResolvedGeneralHelpRequestWhenRenderingOutputThenItDisplaysShortUsageExamples() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help");

        assertAll(
            () -> assertTrue(result.stdout().contains("sonolog help")),
            () -> assertTrue(result.stdout().contains("sonolog process"))
        );
    }

    @Test
    void givenResolvedGeneralHelpRequestWhenRenderingOutputThenItDoesNotDependOnConfigOrProjectFiles(@TempDir Path tempDir)
        throws Exception {
        var result = MainCommandLineTestSupport.runMain(tempDir, "help");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.list(tempDir).findAny().isEmpty())
        );
    }
}
