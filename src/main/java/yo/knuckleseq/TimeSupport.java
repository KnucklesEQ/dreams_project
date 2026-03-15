package yo.knuckleseq;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class TimeSupport {

    static String nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    static String runIdFromTimestamp(String timestamp) {
        return timestamp.replace(":", "-").replace("+00-00", "Z");
    }

    private TimeSupport() {
    }
}
