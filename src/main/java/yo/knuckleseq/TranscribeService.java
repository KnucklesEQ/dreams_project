package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class TranscribeService {

    private final EnvironmentSupport environmentSupport;
    private final OpenAiSpeechClient speechClient;
    private final ManifestStore manifestStore;

    TranscribeService() {
        this(new EnvironmentSupport(), new OpenAiSpeechClient(), new ManifestStore());
    }

    TranscribeService(EnvironmentSupport environmentSupport, OpenAiSpeechClient speechClient, ManifestStore manifestStore) {
        this.environmentSupport = environmentSupport;
        this.speechClient = speechClient;
        this.manifestStore = manifestStore;
    }

    CommandResult execute(AppConfig config, String dreamId, boolean force) {
        ManifestStore.DreamItem dreamItem;

        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
            dreamItem = manifestStore.load(config, dreamId);
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.TRANSCRIBE_FAILED);
        }

        var manifest = dreamItem.manifest();
        if (!force && manifest.isTranscriptionFresh()) {
            return CommandResult.success();
        }

        var archivedAudioPath = manifest.archivedAudioPath();
        if (archivedAudioPath == null || !Files.exists(archivedAudioPath)) {
            return CommandResult.usageError(CliTexts.TRANSCRIBE_FAILED);
        }

        var apiKey = environmentSupport.resolveEnvReference(config.openai().apiKeyRef());
        if (apiKey == null) {
            return CommandResult.usageError(CliTexts.MISSING_API_KEY);
        }

        OpenAiSpeechClient.TranscriptionPayload payload;

        try {
            payload = speechClient.transcribe(config.openai(), archivedAudioPath, apiKey);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        } catch (IOException exception) {
            return CommandResult.externalError(CliTexts.REMOTE_UNAVAILABLE);
        }

        if (payload.transcriptText() == null || payload.transcriptText().isBlank()) {
            return CommandResult.externalError(CliTexts.EMPTY_TRANSCRIPT);
        }

        try {
            writeArtifactsAndUpdateManifest(config, dreamItem, payload, archivedAudioPath);
            return CommandResult.success();
        } catch (IOException exception) {
            return CommandResult.externalError(CliTexts.EMPTY_TRANSCRIPT);
        }
    }

    private void writeArtifactsAndUpdateManifest(
        AppConfig config,
        ManifestStore.DreamItem dreamItem,
        OpenAiSpeechClient.TranscriptionPayload payload,
        Path archivedAudioPath
    ) throws IOException {
        var manifest = dreamItem.manifest();
        var workspaceDir = dreamItem.manifestPath().getParent();
        var sttDir = workspaceDir.resolve("stt");
        var rawResponsePath = sttDir.resolve("raw-response.json").toAbsolutePath().normalize();
        var transcriptPath = sttDir.resolve("transcript.txt").toAbsolutePath().normalize();
        var segmentsPath = sttDir.resolve("segments.json").toAbsolutePath().normalize();
        var now = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(now);

        Files.createDirectories(sttDir);
        FilesystemSupport.atomicWrite(rawResponsePath, payload.rawResponseBody());
        FilesystemSupport.atomicWrite(transcriptPath, payload.transcriptText() + "\n");
        FilesystemSupport.atomicWrite(segmentsPath, payload.segmentsJson());

        var attemptCount = manifest.stages() == null || manifest.stages().transcribe() == null
            ? 1
            : manifest.stages().transcribe().attemptCount() + 1;
        var transcribeStage = new ManifestDocument.ManifestStageState(
            "completed",
            now,
            now,
            attemptCount,
            "openai",
            config.openai().transcriptionModel(),
            payload.language().isBlank() ? "es" : payload.language(),
            null,
            null,
            HashingSupport.sha256(archivedAudioPath),
            HashingSupport.sha256(transcriptPath),
            rawResponsePath.toString(),
            transcriptPath.toString(),
            segmentsPath.toString(),
            null,
            java.util.List.of(),
            null,
            null,
            null
        );

        var updatedManifest = manifest
            .withStages(manifest.stages().withTranscribe(transcribeStage))
            .withCurrentArtifacts(manifest.currentArtifacts().withTranscriptPath(transcriptPath.toString()))
            .withExecutionState(now, "transcribed", runId, null);

        manifestStore.write(dreamItem, updatedManifest);
    }
}
