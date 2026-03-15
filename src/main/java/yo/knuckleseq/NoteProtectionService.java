package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class NoteProtectionService {

    boolean isProtected(ManifestDocument manifest) throws IOException {
        var notePath = manifest.notePath();
        if (notePath == null || !Files.exists(notePath)) {
            return false;
        }

        var storedNoteHash = manifest.effectiveNoteHash();
        if (storedNoteHash == null || storedNoteHash.isBlank()) {
            return true;
        }

        return !storedNoteHash.equals(HashingSupport.sha256(notePath));
    }
}
