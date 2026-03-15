package yo.knuckleseq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record RunDocument(
    int schemaVersion,
    String runId,
    String startedAt,
    String finishedAt,
    String status,
    String pipelineVersion,
    String locale,
    String command,
    List<String> requestedStages,
    EffectiveConfig effectiveConfig,
    List<ItemResult> items,
    Totals totals,
    List<String> warnings,
    List<RunError> errors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EffectiveConfig(
        String configPath,
        String configFingerprint,
        String appHomeDir,
        String inputDir,
        String archiveDir,
        String notesDir,
        String workspaceDir,
        String promptSet,
        OpenAiConfigSnapshot openai,
        ExecutionSnapshot execution
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiConfigSnapshot(String transcriptionModel, String cleanupModel, String analysisModel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExecutionSnapshot(boolean continueOnError, int maxParallelFiles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItemResult(
        String detectedInputPath,
        String archivePath,
        String recordedAt,
        String dreamId,
        String manifestPath,
        String notePath,
        String result,
        List<StageResult> stages,
        List<String> warnings,
        RunError error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StageResult(String stage, String status, int attemptCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Totals(int itemCount, int completed, int failed, int skipped) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RunError(String code, String message) {
    }
}
