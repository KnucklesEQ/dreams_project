package yo.knuckleseq;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayCommandHelpMessageTest {

    @Nested
    class ResolveCommandSpecificHelpInvocation {

        @Test
        void givenKnownCommandWhenResolvingRequestedCommandThenRequestIsAccepted() throws Exception {
            var result = MainCommandLineTestSupport.runMain("help", "process");

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenUnknownCommandWhenResolvingRequestedCommandThenRequestFailsWithUsageError() throws Exception {
            var result = MainCommandLineTestSupport.runMain("help", "unknown-command");

            assertAll(
                () -> assertEquals(2, result.exitCode()),
                () -> assertTrue(result.stdout().contains("sonolog help") || result.stderr().contains("sonolog help"))
            );
        }

        @Test
        void givenTooManyArgumentsWhenResolvingRequestedCommandThenRequestFailsWithUsageError() throws Exception {
            var result = MainCommandLineTestSupport.runMain("help", "process", "unexpected");

            assertEquals(2, result.exitCode());
        }
    }

    @Test
    void givenResolvedCommandHelpRequestWhenRenderingOutputThenItDisplaysTheRequestedCommandUsage() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help", "process");

        assertAll(
            () -> assertTrue(result.stdout().contains("process")),
            () -> assertTrue(result.stdout().contains("<dreamId|path>"))
        );
    }

    @Test
    void givenResolvedCommandHelpRequestWhenRenderingOutputThenItDisplaysTheCommandArgumentsAndFlags() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help", "process");

        assertAll(
            () -> assertTrue(result.stdout().contains("--from")),
            () -> assertTrue(result.stdout().contains("--to")),
            () -> assertTrue(result.stdout().contains("--retry-failed")),
            () -> assertTrue(result.stdout().contains("--force"))
        );
    }

    @Test
    void givenResolvedCommandHelpRequestWhenRenderingOutputThenItDisplaysRelevantSafetyNotes() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help", "process");

        assertTrue(result.stdout().toLowerCase().contains("force"));
    }

    @Test
    void givenResolvedCommandHelpRequestWhenRenderingOutputThenItDoesNotDependOnConfigOrProjectFiles(@TempDir Path tempDir)
        throws Exception {
        var result = MainCommandLineTestSupport.runMain(tempDir, "help", "process");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.list(tempDir).findAny().isEmpty())
        );
    }
}
