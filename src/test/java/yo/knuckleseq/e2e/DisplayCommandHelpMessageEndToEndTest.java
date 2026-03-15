package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayCommandHelpMessageEndToEndTest {

    @Test
    void givenCommandHelpRequestWhenRunningTheApplicationEndToEndThenItPrintsTheCommandHelpContract() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help", "process");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("process")),
            () -> assertTrue(result.stdout().contains("<dreamId|path>")),
            () -> assertTrue(result.stdout().contains("--from")),
            () -> assertTrue(result.stdout().contains("--force")),
            () -> assertEquals("", result.stderr())
        );
    }
}
