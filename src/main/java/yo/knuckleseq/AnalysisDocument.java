package yo.knuckleseq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = false)
record AnalysisDocument(
    int schemaVersion,
    String dreamId,
    String createdAt,
    String updatedAt,
    String pipelineVersion,
    String locale,
    AnalysisSource source,
    AnalysisProvenance provenance,
    AnalysisReview review,
    AnalysisTitleCandidate titleCandidate,
    AnalysisEntities entities,
    List<AnalysisTimelineItem> timeline,
    List<AnalysisUncertainty> uncertainties,
    AnalysisCoverage coverage
) {

    static AnalysisDocument read(Path analysisPath, String cleanedText) throws IOException {
        return parseCanonical(Files.readString(analysisPath), cleanedText);
    }

    static AnalysisDocument parseCanonical(String rawJson, String cleanedText) throws IOException {
        var document = JsonSupport.STRICT_MAPPER.readValue(rawJson, AnalysisDocument.class);
        validate(document, cleanedText, rawJson);
        return document;
    }

    private static void validate(AnalysisDocument document, String cleanedText, String rawJson) {
        require(document.schemaVersion() == 1, "schemaVersion invalido");
        require(notBlank(document.dreamId()), "dreamId requerido");
        require(notBlank(document.createdAt()), "createdAt requerido");
        require(notBlank(document.updatedAt()), "updatedAt requerido");
        require(notBlank(document.pipelineVersion()), "pipelineVersion requerido");
        require("es".equals(document.locale()), "locale invalido");
        require(document.source() != null, "source requerido");
        require(document.provenance() != null, "provenance requerido");
        require(document.review() != null, "review requerido");
        require(document.entities() != null, "entities requerido");
        require(document.uncertainties() != null, "uncertainties requerido");
        require(document.coverage() != null, "coverage requerido");
        require(notBlank(document.source().manifestPath()), "manifestPath requerido");
        require(notBlank(document.source().sourceAudioName()), "sourceAudioName requerido");
        require(notBlank(document.source().sourceSha256()), "sourceSha256 requerido");
        require(notBlank(document.source().transcriptHash()), "transcriptHash requerido");
        require(notBlank(document.source().cleanedHash()), "cleanedHash requerido");
        require(notBlank(document.source().recordedAt()), "recordedAt requerido");
        require(List.of("transcript", "cleaned", "mixed").contains(document.source().sourceTextUsed()), "sourceTextUsed invalido");
        require(notBlank(document.provenance().runId()), "runId requerido");
        require(notBlank(document.provenance().provider()), "provider requerido");
        require(notBlank(document.provenance().model()), "model requerido");
        require(notBlank(document.provenance().promptId()), "promptId requerido");
        require(notBlank(document.provenance().promptHash()), "promptHash requerido");
        require(notBlank(document.provenance().inputHash()), "inputHash requerido");
        require(document.review().reviewReasons() != null, "reviewReasons requerido");
        require("text_only".equals(document.coverage().evidencePolicy()), "evidencePolicy invalido");
        validateTitleCandidate(document.titleCandidate());
        validateEntities(document.entities());
        validateUncertainties(document.uncertainties());
        validateTimeline(document.timeline());

        var cleaned = cleanedText == null ? "" : cleanedText.toLowerCase(Locale.ROOT);
        var raw = rawJson.toLowerCase(Locale.ROOT);
        if (!cleaned.contains("dragon") && raw.contains("dragon")) {
            throw new IllegalArgumentException("suspected_hallucination");
        }
    }

    private static void validateTitleCandidate(AnalysisTitleCandidate titleCandidate) {
        if (titleCandidate == null || titleCandidate.text() == null || titleCandidate.text().isBlank()) {
            return;
        }

        require(validCertainty(titleCandidate.certainty()), "titleCandidate certainty invalido");
        require(titleCandidate.evidence() != null && !titleCandidate.evidence().isEmpty(), "titleCandidate evidence requerido");
        titleCandidate.evidence().forEach(AnalysisDocument::validateEvidence);
    }

    private static void validateEntities(AnalysisEntities entities) {
        require(entities.people() != null, "people requerido");
        require(entities.places() != null, "places requerido");
        require(entities.emotions() != null, "emotions requerido");
        require(entities.motifs() != null, "motifs requerido");
        require(entities.tags() != null, "tags requerido");

        entities.people().forEach(AnalysisDocument::validateEntity);
        entities.places().forEach(AnalysisDocument::validateEntity);
        entities.emotions().forEach(AnalysisDocument::validateEntity);
        entities.motifs().forEach(AnalysisDocument::validateEntity);
        entities.tags().forEach(AnalysisDocument::validateEntity);
    }

    private static void validateEntity(AnalysisEntity entity) {
        require(notBlank(entity.value()), "entity value requerido");
        require(notBlank(entity.normalized()), "entity normalized requerido");
        require(validCertainty(entity.certainty()), "entity certainty invalido");
        require(entity.evidence() != null && !entity.evidence().isEmpty(), "entity evidence requerido");
        entity.evidence().forEach(AnalysisDocument::validateEvidence);
    }

    private static void validateTimeline(List<AnalysisTimelineItem> timeline) {
        if (timeline == null) {
            return;
        }

        for (var item : timeline) {
            require(item.order() > 0, "timeline order invalido");
            require(notBlank(item.text()), "timeline text requerido");
            require(validCertainty(item.certainty()), "timeline certainty invalido");
            require(item.evidence() != null && !item.evidence().isEmpty(), "timeline evidence requerido");
            item.evidence().forEach(AnalysisDocument::validateEvidence);
        }
    }

    private static void validateUncertainties(List<AnalysisUncertainty> uncertainties) {
        for (var uncertainty : uncertainties) {
            require(notBlank(uncertainty.type()), "uncertainty type requerido");
            require(notBlank(uncertainty.text()), "uncertainty text requerido");
            require(uncertainty.evidence() != null && !uncertainty.evidence().isEmpty(), "uncertainty evidence requerido");
            uncertainty.evidence().forEach(AnalysisDocument::validateEvidence);
        }
    }

    private static void validateEvidence(AnalysisEvidence evidence) {
        require(notBlank(evidence.text()), "evidence text requerido");
        require(List.of("transcript", "cleaned").contains(evidence.source()), "evidence source invalido");
    }

    private static boolean validCertainty(String certainty) {
        return List.of("explicit", "inferred_light", "unclear").contains(certainty);
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
    record AnalysisSource(
        String manifestPath,
        String sourceAudioName,
        String sourceSha256,
        String transcriptHash,
        String cleanedHash,
        String recordedAt,
        String recordedAtSource,
        String sourceTextUsed
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisProvenance(
        String runId,
        String provider,
        String model,
        String promptId,
        String promptHash,
        String inputHash
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisReview(boolean needsReview, List<String> reviewReasons) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisTitleCandidate(String text, String certainty, List<AnalysisEvidence> evidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisEntities(
        List<AnalysisEntity> people,
        List<AnalysisEntity> places,
        List<AnalysisEntity> emotions,
        List<AnalysisEntity> motifs,
        List<AnalysisEntity> tags
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisEntity(String value, String normalized, String certainty, List<AnalysisEvidence> evidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisEvidence(String text, String source, List<Integer> segmentIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisTimelineItem(int order, String text, String certainty, List<AnalysisEvidence> evidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisUncertainty(String type, String text, List<AnalysisEvidence> evidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record AnalysisCoverage(String evidencePolicy, int unknownCount) {
    }
}
