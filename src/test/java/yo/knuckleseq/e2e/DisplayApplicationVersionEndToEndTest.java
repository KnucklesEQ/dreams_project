package yo.knuckleseq.e2e;

import yo.knuckleseq.support.MainCommandLineTestSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayApplicationVersionEndToEndTest {

    @Test
    void givenVersionRequestWhenRunningTheApplicationEndToEndThenItPrintsTheStableVersionContract() throws Exception {
        var result = MainCommandLineTestSupport.runMain("version");

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertEquals("Sonolog 0.0.1\n", result.stdout()),
            () -> assertEquals("", result.stderr())
        );
    }
}
