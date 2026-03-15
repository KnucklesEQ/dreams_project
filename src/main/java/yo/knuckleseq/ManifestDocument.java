package yo.knuckleseq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ManifestDocument(
    int schemaVersion,
    String dreamId,
    String createdAt,
    String updatedAt,
    String pipelineVersion,
    String locale,
    String status,
    boolean needsReview,
    String configFingerprint,
    String lastRunId,
    ManifestSource source,
    ManifestNaming naming,
    ManifestStages stages,
    ManifestCurrentArtifacts currentArtifacts,
    ManifestErrorInfo lastError
) {

    static ManifestDocument read(Path manifestPath) throws IOException {
        return JsonSupport.LENIENT_MAPPER.readValue(Files.readString(manifestPath), ManifestDocument.class);
    }

    static ManifestDocument imported(
        AppConfig config,
        ImportedDreamDraft draft,
        String createdAt,
        String configFingerprint,
        String lastRunId
    ) {
        return new ManifestDocument(
            1,
            draft.dreamId(),
            createdAt,
            createdAt,
            "0.1",
            config.pipeline().locale(),
            "imported",
            true,
            configFingerprint,
            lastRunId,
            new ManifestSource(
                draft.detectedInputPath(),
                draft.archivedPath(),
                draft.originalFileName(),
                draft.sourceHash(),
                draft.sizeBytes(),
                0,
                "audio/wav",
                draft.recordedAt(),
                "filename"
            ),
            new ManifestNaming(
                draft.dreamDayIndex(),
                draft.dreamDayOrdinal(),
                true,
                null,
                draft.titleFinal()
            ),
            ManifestStages.imported(createdAt),
            new ManifestCurrentArtifacts(null, null, null, null),
            null
        );
    }

    boolean isStale() {
        return "stale".equals(status)
            || stages != null && stages.buildNote() != null && "stale".equals(stages.buildNote().status());
    }

    boolean isFailed() {
        return "failed".equals(status) || lastError != null;
    }

    String effectiveNotePath() {
        if (stages != null && stages.buildNote() != null && stages.buildNote().notePath() != null && !stages.buildNote().notePath().isBlank()) {
            return stages.buildNote().notePath();
        }

        if (currentArtifacts == null) {
            return null;
        }

        return currentArtifacts.notePath();
    }

    String effectiveNoteHash() {
        if (stages == null || stages.buildNote() == null) {
            return null;
        }

        return stages.buildNote().noteHash();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestSource(
        String detectedInputPath,
        String archivedPath,
        String originalFileName,
        String sha256,
        long sizeBytes,
        long durationMs,
        String mimeType,
        String recordedAt,
        String recordedAtSource
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestNaming(
        int dreamDayIndex,
        String dreamDayOrdinal,
        boolean ordinalFrozen,
        String titleCandidate,
        String titleFinal
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestStages(
        @JsonProperty("import") ManifestStageState importStage,
        ManifestStageState transcribe,
        ManifestStageState clean,
        ManifestStageState analyze,
        ManifestStageState buildNote
    ) {

        static ManifestStages imported(String timestamp) {
            return new ManifestStages(
                ManifestStageState.completed(timestamp),
                ManifestStageState.pending(),
                ManifestStageState.pending(),
                ManifestStageState.pending(),
                ManifestStageState.pending()
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestStageState(
        String status,
        String startedAt,
        String finishedAt,
        int attemptCount,
        List<ManifestWarningInfo> warnings,
        ManifestErrorInfo error,
        String notePath,
        String noteHash
    ) {

        static ManifestStageState completed(String timestamp) {
            return new ManifestStageState("completed", timestamp, timestamp, 1, List.of(), null, null, null);
        }

        static ManifestStageState pending() {
            return new ManifestStageState("pending", null, null, 0, List.of(), null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestCurrentArtifacts(
        String transcriptPath,
        String cleanedPath,
        String analysisPath,
        String notePath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestWarningInfo(String code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestErrorInfo(String code, String message, Boolean retryable, String occurredAt) {
    }
}
