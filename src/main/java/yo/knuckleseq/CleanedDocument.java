package yo.knuckleseq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = false)
record CleanedDocument(
    int schemaVersion,
    String dreamId,
    String createdAt,
    String sourceHash,
    String transcriptHash,
    String promptId,
    String promptHash,
    String cleanText,
    List<String> paragraphs,
    List<CleanupChange> changes,
    List<UncertainSpan> uncertainSpans
) {

    static CleanedDocument read(Path cleanedPath, String transcriptText) throws IOException {
        return parseCanonical(Files.readString(cleanedPath), transcriptText);
    }

    static CleanedDocument parseCanonical(String rawJson, String transcriptText) throws IOException {
        var document = JsonSupport.STRICT_MAPPER.readValue(rawJson, CleanedDocument.class);
        validate(document, transcriptText);
        return document;
    }

    private static void validate(CleanedDocument document, String transcriptText) {
        require(document.schemaVersion() == 1, "schemaVersion invalido");
        require(notBlank(document.dreamId()), "dreamId requerido");
        require(notBlank(document.createdAt()), "createdAt requerido");
        require(notBlank(document.sourceHash()), "sourceHash requerido");
        require(notBlank(document.transcriptHash()), "transcriptHash requerido");
        require(notBlank(document.promptId()), "promptId requerido");
        require(notBlank(document.promptHash()), "promptHash requerido");
        require(notBlank(document.cleanText()), "cleanText requerido");
        require(document.paragraphs() != null, "paragraphs requerido");
        require(document.changes() != null, "changes requerido");
        require(document.uncertainSpans() != null, "uncertainSpans requerido");

        var transcript = transcriptText == null ? "" : transcriptText.toLowerCase(Locale.ROOT);
        var cleaned = document.cleanText().toLowerCase(Locale.ROOT);

        if (!transcript.contains("dragon") && (cleaned.contains("dragon") || cleaned.contains("inventado"))) {
            throw new IllegalArgumentException("suspected_hallucination");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record CleanupChange(String type, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record UncertainSpan(String text, String reason) {
    }
}
