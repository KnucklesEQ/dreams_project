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

class DisplayApplicationVersionTest {

    @Nested
    class ResolveVersionInvocation {

        @Test
        void givenVersionCommandWhenResolvingInvocationThenRequestIsAccepted() throws Exception {
            var result = MainCommandLineTestSupport.runMain("version");

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenVersionAliasWhenResolvingInvocationThenRequestIsAccepted() throws Exception {
            var result = MainCommandLineTestSupport.runMain("--version");

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenUnexpectedArgumentsWhenResolvingInvocationThenRequestFailsWithUsageError() throws Exception {
            var result = MainCommandLineTestSupport.runMain("version", "unexpected");

            assertEquals(2, result.exitCode());
        }
    }

    @Test
    void givenResolvedVersionRequestWhenRenderingOutputThenItDisplaysTheApplicationName() throws Exception {
        var result = MainCommandLineTestSupport.runMain("version");

        assertTrue(result.stdout().contains("Sonolog"));
    }

    @Test
    void givenResolvedVersionRequestWhenRenderingOutputThenItDisplaysTheApplicationVersion() throws Exception {
        var result = MainCommandLineTestSupport.runMain("version");

        assertTrue(result.stdout().contains("0.0.1"));
    }

    @Test
    void givenResolvedVersionRequestWhenRenderingOutputThenItOmitsPipelineBuildAndRuntimeMetadata() throws Exception {
        var result = MainCommandLineTestSupport.runMain("version");

        assertAll(
            () -> assertFalse(result.stdout().toLowerCase().contains("pipeline")),
            () -> assertFalse(result.stdout().toLowerCase().contains("commit")),
            () -> assertFalse(result.stdout().toLowerCase().contains("java")),
            () -> assertFalse(result.stdout().toLowerCase().contains("config"))
        );
    }

    @Test
    void givenResolvedVersionRequestWhenRenderingOutputThenItDoesNotDependOnConfigOrProjectFiles(@TempDir Path tempDir)
        throws Exception {
        var result = MainCommandLineTestSupport.runMain(tempDir, "version");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.list(tempDir).findAny().isEmpty())
        );
    }
}
