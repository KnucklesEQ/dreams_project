package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class AnalyzeService {

    private static final String ANALYSIS_PROMPT_ID = "analysis-es-v1";
    private static final String ANALYSIS_PROMPT_TEXT = "extractor de informacion evidence-bearing";

    private final EnvironmentSupport environmentSupport;
    private final OpenAiLlmClient llmClient;
    private final ManifestStore manifestStore;

    AnalyzeService() {
        this(new EnvironmentSupport(), new OpenAiLlmClient(), new ManifestStore());
    }

    AnalyzeService(EnvironmentSupport environmentSupport, OpenAiLlmClient llmClient, ManifestStore manifestStore) {
        this.environmentSupport = environmentSupport;
        this.llmClient = llmClient;
        this.manifestStore = manifestStore;
    }

    CommandResult execute(AppConfig config, String dreamId, boolean force) {
        ManifestStore.DreamItem dreamItem;

        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
            dreamItem = manifestStore.load(config, dreamId);
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.ANALYZE_FAILED);
        }

        var manifest = dreamItem.manifest();
        if (!force && manifest.isAnalysisFresh()) {
            return CommandResult.success();
        }

        var cleanedPath = manifest.cleanedPath();
        if (cleanedPath == null || !Files.exists(cleanedPath)) {
            return CommandResult.usageError(CliTexts.ANALYZE_FAILED);
        }

        var apiKey = environmentSupport.resolveEnvReference(config.openai().apiKeyRef());
        if (apiKey == null) {
            return CommandResult.usageError(CliTexts.MISSING_API_KEY);
        }

        OpenAiLlmClient.CompletionPayload payload;
        String cleanedJson;
        CleanedDocument cleanedDocument;

        try {
            cleanedJson = Files.readString(cleanedPath);
            cleanedDocument = CleanedDocument.parseCanonical(cleanedJson, manifest.transcriptPath() == null ? "" : Files.readString(manifest.transcriptPath()));
            payload = llmClient.completeJson(config.openai(), apiKey, cleanedDocument.cleanText());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.externalError(CliTexts.ANALYZE_FAILED);
        }

        AnalysisDocument analysisDocument;

        try {
            analysisDocument = AnalysisDocument.parseCanonical(payload.outputText(), cleanedDocument.cleanText());
            writeArtifactsAndUpdateManifest(config, dreamItem, payload, analysisDocument, cleanedPath);
            return CommandResult.success();
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.externalError(CliTexts.INVALID_ANALYSIS_OUTPUT);
        }
    }

    private void writeArtifactsAndUpdateManifest(
        AppConfig config,
        ManifestStore.DreamItem dreamItem,
        OpenAiLlmClient.CompletionPayload payload,
        AnalysisDocument analysisDocument,
        Path cleanedPath
    ) throws IOException {
        var manifest = dreamItem.manifest();
        var workspaceDir = dreamItem.manifestPath().getParent();
        var analysisDir = workspaceDir.resolve("analysis");
        var analysisPath = analysisDir.resolve("analysis.json").toAbsolutePath().normalize();
        var now = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(now);
        var canonicalJson = JsonSupport.toStableJson(analysisDocument);
        var titleCandidate = analysisDocument.titleCandidate() == null ? null : analysisDocument.titleCandidate().text();
        var titleFinal = finalTitle(manifest, titleCandidate);

        Files.createDirectories(analysisDir);
        FilesystemSupport.atomicWrite(analysisPath, canonicalJson);

        var attemptCount = manifest.stages() == null || manifest.stages().analyze() == null
            ? 1
            : manifest.stages().analyze().attemptCount() + 1;
        var analyzeStage = new ManifestDocument.ManifestStageState(
            "completed",
            now,
            now,
            attemptCount,
            "openai",
            config.openai().analysisModel(),
            null,
            ANALYSIS_PROMPT_ID,
            HashingSupport.sha256(ANALYSIS_PROMPT_TEXT),
            HashingSupport.sha256(cleanedPath),
            HashingSupport.sha256(analysisPath),
            null,
            null,
            null,
            analysisPath.toString(),
            java.util.List.of(),
            null,
            null,
            null
        );

        var updatedManifest = manifest
            .withStages(manifest.stages().withAnalyze(analyzeStage))
            .withCurrentArtifacts(manifest.currentArtifacts().withAnalysisPath(analysisPath.toString()))
            .withNaming(manifest.naming().withTitleCandidate(titleCandidate, titleFinal))
            .withExecutionState(now, "analyzed", runId, null);

        manifestStore.write(dreamItem, updatedManifest);
    }

    private String finalTitle(ManifestDocument manifest, String titleCandidate) {
        var recordedAt = manifest.source() == null ? "1970-01-01T00:00:00" : manifest.source().recordedAt();
        var ordinal = manifest.naming() == null ? "I" : manifest.naming().dreamDayOrdinal();
        var subtitle = titleCandidate == null || titleCandidate.isBlank() ? "Sin titulo" : titleCandidate;
        return "Sueno " + recordedAt.substring(0, 10).replace('-', '_') + "(" + ordinal + ") - " + subtitle;
    }
}
