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

    Path archivedAudioPath() {
        return source == null || source.archivedPath() == null || source.archivedPath().isBlank()
            ? null
            : Path.of(source.archivedPath());
    }

    Path transcriptPath() {
        if (currentArtifacts != null && currentArtifacts.transcriptPath() != null && !currentArtifacts.transcriptPath().isBlank()) {
            return Path.of(currentArtifacts.transcriptPath());
        }

        if (stages != null && stages.transcribe() != null && stages.transcribe().transcriptPath() != null && !stages.transcribe().transcriptPath().isBlank()) {
            return Path.of(stages.transcribe().transcriptPath());
        }

        return null;
    }

    Path cleanedPath() {
        if (currentArtifacts != null && currentArtifacts.cleanedPath() != null && !currentArtifacts.cleanedPath().isBlank()) {
            return Path.of(currentArtifacts.cleanedPath());
        }

        if (stages != null && stages.clean() != null && stages.clean().outputPath() != null && !stages.clean().outputPath().isBlank()) {
            return Path.of(stages.clean().outputPath());
        }

        return null;
    }

    Path analysisPath() {
        if (currentArtifacts != null && currentArtifacts.analysisPath() != null && !currentArtifacts.analysisPath().isBlank()) {
            return Path.of(currentArtifacts.analysisPath());
        }

        if (stages != null && stages.analyze() != null && stages.analyze().outputPath() != null && !stages.analyze().outputPath().isBlank()) {
            return Path.of(stages.analyze().outputPath());
        }

        return null;
    }

    Path notePath() {
        var notePathText = effectiveNotePath();
        return notePathText == null || notePathText.isBlank() ? null : Path.of(notePathText);
    }

    boolean isTranscriptionFresh() {
        return stages != null
            && stages.transcribe() != null
            && "completed".equals(stages.transcribe().status())
            && transcriptPath() != null
            && Files.exists(transcriptPath());
    }

    boolean isCleanFresh() {
        return stages != null
            && stages.clean() != null
            && "completed".equals(stages.clean().status())
            && cleanedPath() != null
            && Files.exists(cleanedPath());
    }

    boolean isAnalysisFresh() {
        return stages != null
            && stages.analyze() != null
            && "completed".equals(stages.analyze().status())
            && analysisPath() != null
            && Files.exists(analysisPath());
    }

    boolean isNoteFresh() {
        return stages != null
            && stages.buildNote() != null
            && "completed".equals(stages.buildNote().status())
            && notePath() != null
            && Files.exists(notePath());
    }

    ManifestDocument withNaming(ManifestNaming updatedNaming) {
        return new ManifestDocument(
            schemaVersion,
            dreamId,
            createdAt,
            updatedAt,
            pipelineVersion,
            locale,
            status,
            needsReview,
            configFingerprint,
            lastRunId,
            source,
            updatedNaming,
            stages,
            currentArtifacts,
            lastError
        );
    }

    ManifestDocument withCurrentArtifacts(ManifestCurrentArtifacts updatedArtifacts) {
        return new ManifestDocument(
            schemaVersion,
            dreamId,
            createdAt,
            updatedAt,
            pipelineVersion,
            locale,
            status,
            needsReview,
            configFingerprint,
            lastRunId,
            source,
            naming,
            stages,
            updatedArtifacts,
            lastError
        );
    }

    ManifestDocument withStages(ManifestStages updatedStages) {
        return new ManifestDocument(
            schemaVersion,
            dreamId,
            createdAt,
            updatedAt,
            pipelineVersion,
            locale,
            status,
            needsReview,
            configFingerprint,
            lastRunId,
            source,
            naming,
            updatedStages,
            currentArtifacts,
            lastError
        );
    }

    ManifestDocument withExecutionState(String updatedAt, String status, String lastRunId, ManifestErrorInfo lastError) {
        return new ManifestDocument(
            schemaVersion,
            dreamId,
            createdAt,
            updatedAt,
            pipelineVersion,
            locale,
            status,
            needsReview,
            configFingerprint,
            lastRunId,
            source,
            naming,
            stages,
            currentArtifacts,
            lastError
        );
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

        ManifestNaming withTitleCandidate(String updatedTitleCandidate, String updatedTitleFinal) {
            return new ManifestNaming(dreamDayIndex, dreamDayOrdinal, ordinalFrozen, updatedTitleCandidate, updatedTitleFinal);
        }
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

        ManifestStages withTranscribe(ManifestStageState updatedStage) {
            return new ManifestStages(importStage, updatedStage, clean, analyze, buildNote);
        }

        ManifestStages withClean(ManifestStageState updatedStage) {
            return new ManifestStages(importStage, transcribe, updatedStage, analyze, buildNote);
        }

        ManifestStages withAnalyze(ManifestStageState updatedStage) {
            return new ManifestStages(importStage, transcribe, clean, updatedStage, buildNote);
        }

        ManifestStages withBuildNote(ManifestStageState updatedStage) {
            return new ManifestStages(importStage, transcribe, clean, analyze, updatedStage);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestStageState(
        String status,
        String startedAt,
        String finishedAt,
        int attemptCount,
        String provider,
        String model,
        String language,
        String promptId,
        String promptHash,
        String inputHash,
        String outputHash,
        String rawResponsePath,
        String transcriptPath,
        String segmentsPath,
        String outputPath,
        List<ManifestWarningInfo> warnings,
        ManifestErrorInfo error,
        String notePath,
        String noteHash
    ) {

        static ManifestStageState completed(String timestamp) {
            return new ManifestStageState(
                "completed",
                timestamp,
                timestamp,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null
            );
        }

        static ManifestStageState pending() {
            return new ManifestStageState(
                "pending",
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null
            );
        }

        ManifestStageState withAttemptCount(int updatedAttemptCount) {
            return new ManifestStageState(
                status,
                startedAt,
                finishedAt,
                updatedAttemptCount,
                provider,
                model,
                language,
                promptId,
                promptHash,
                inputHash,
                outputHash,
                rawResponsePath,
                transcriptPath,
                segmentsPath,
                outputPath,
                warnings,
                error,
                notePath,
                noteHash
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestCurrentArtifacts(
        String transcriptPath,
        String cleanedPath,
        String analysisPath,
        String notePath
    ) {

        ManifestCurrentArtifacts withTranscriptPath(String updatedTranscriptPath) {
            return new ManifestCurrentArtifacts(updatedTranscriptPath, cleanedPath, analysisPath, notePath);
        }

        ManifestCurrentArtifacts withCleanedPath(String updatedCleanedPath) {
            return new ManifestCurrentArtifacts(transcriptPath, updatedCleanedPath, analysisPath, notePath);
        }

        ManifestCurrentArtifacts withAnalysisPath(String updatedAnalysisPath) {
            return new ManifestCurrentArtifacts(transcriptPath, cleanedPath, updatedAnalysisPath, notePath);
        }

        ManifestCurrentArtifacts withNotePath(String updatedNotePath) {
            return new ManifestCurrentArtifacts(transcriptPath, cleanedPath, analysisPath, updatedNotePath);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestWarningInfo(String code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ManifestErrorInfo(String code, String message, Boolean retryable, String occurredAt) {
    }
}
