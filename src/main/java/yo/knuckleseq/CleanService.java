package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class CleanService {

    private static final String CLEAN_PROMPT_ID = "cleanup-es-v1";
    private static final String CLEAN_PROMPT_TEXT = "editor conservador de transcripciones";

    private final EnvironmentSupport environmentSupport;
    private final OpenAiLlmClient llmClient;
    private final ManifestStore manifestStore;

    CleanService() {
        this(new EnvironmentSupport(), new OpenAiLlmClient(), new ManifestStore());
    }

    CleanService(EnvironmentSupport environmentSupport, OpenAiLlmClient llmClient, ManifestStore manifestStore) {
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
            return CommandResult.usageError(CliTexts.CLEAN_FAILED);
        }

        var manifest = dreamItem.manifest();
        if (!force && manifest.isCleanFresh()) {
            return CommandResult.success();
        }

        var transcriptPath = manifest.transcriptPath();
        if (transcriptPath == null || !Files.exists(transcriptPath)) {
            return CommandResult.usageError(CliTexts.CLEAN_FAILED);
        }

        var apiKey = environmentSupport.resolveEnvReference(config.openai().apiKeyRef());
        if (apiKey == null) {
            return CommandResult.usageError(CliTexts.MISSING_API_KEY);
        }

        OpenAiLlmClient.CompletionPayload payload;
        String transcriptText;

        try {
            transcriptText = Files.readString(transcriptPath);
            payload = llmClient.completeJson(config.openai(), apiKey, transcriptText);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        } catch (IOException exception) {
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        }

        CleanedDocument cleanedDocument;

        try {
            cleanedDocument = CleanedDocument.parseCanonical(payload.outputText(), transcriptText);
            writeArtifactsAndUpdateManifest(config, dreamItem, payload, cleanedDocument, transcriptPath);
            return CommandResult.success();
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.externalError(CliTexts.INVALID_CLEAN_OUTPUT);
        }
    }

    private void writeArtifactsAndUpdateManifest(
        AppConfig config,
        ManifestStore.DreamItem dreamItem,
        OpenAiLlmClient.CompletionPayload payload,
        CleanedDocument cleanedDocument,
        Path transcriptPath
    ) throws IOException {
        var manifest = dreamItem.manifest();
        var workspaceDir = dreamItem.manifestPath().getParent();
        var llmDir = workspaceDir.resolve("llm");
        var rawResponsePath = llmDir.resolve("raw-response.json").toAbsolutePath().normalize();
        var cleanedPath = llmDir.resolve("cleaned.json").toAbsolutePath().normalize();
        var now = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(now);
        var canonicalJson = JsonSupport.toStableJson(cleanedDocument);

        Files.createDirectories(llmDir);
        FilesystemSupport.atomicWrite(rawResponsePath, payload.rawResponseBody());
        FilesystemSupport.atomicWrite(cleanedPath, canonicalJson);

        var attemptCount = manifest.stages() == null || manifest.stages().clean() == null
            ? 1
            : manifest.stages().clean().attemptCount() + 1;
        var cleanStage = new ManifestDocument.ManifestStageState(
            "completed",
            now,
            now,
            attemptCount,
            "openai",
            config.openai().cleanupModel(),
            null,
            CLEAN_PROMPT_ID,
            HashingSupport.sha256(CLEAN_PROMPT_TEXT),
            HashingSupport.sha256(transcriptPath),
            HashingSupport.sha256(cleanedPath),
            rawResponsePath.toString(),
            null,
            null,
            cleanedPath.toString(),
            java.util.List.of(),
            null,
            null,
            null
        );

        var updatedManifest = manifest
            .withStages(manifest.stages().withClean(cleanStage))
            .withCurrentArtifacts(manifest.currentArtifacts().withCleanedPath(cleanedPath.toString()))
            .withExecutionState(now, "cleaned", runId, null);

        manifestStore.write(dreamItem, updatedManifest);
    }
}
