package yo.knuckleseq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayApplicationHelpMessageEndToEndTest {

    @Test
    void givenGeneralHelpRequestWhenRunningTheApplicationEndToEndThenItPrintsTheGeneralHelpContract() throws Exception {
        var result = MainCommandLineTestSupport.runMain("help");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(result.stdout().contains("init")),
            () -> assertTrue(result.stdout().contains("process")),
            () -> assertTrue(result.stdout().contains("--config")),
            () -> assertTrue(result.stdout().contains("sonolog help")),
            () -> assertEquals("", result.stderr())
        );
    }
}
