package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class HashingSupport {

    static String sha256(Path path) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + hex(digest.digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String hex(byte[] bytes) {
        var builder = new StringBuilder();

        for (var value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }

        return builder.toString();
    }

    private HashingSupport() {
    }
}
