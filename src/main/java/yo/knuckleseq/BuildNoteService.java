package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class BuildNoteService {

    private final ManifestStore manifestStore;
    private final NoteProtectionService noteProtectionService;
    private final NoteRenderer noteRenderer;

    BuildNoteService() {
        this(new ManifestStore(), new NoteProtectionService(), new NoteRenderer());
    }

    BuildNoteService(ManifestStore manifestStore, NoteProtectionService noteProtectionService, NoteRenderer noteRenderer) {
        this.manifestStore = manifestStore;
        this.noteProtectionService = noteProtectionService;
        this.noteRenderer = noteRenderer;
    }

    CommandResult execute(AppConfig config, String dreamId, boolean force) {
        ManifestStore.DreamItem dreamItem;

        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
            dreamItem = manifestStore.load(config, dreamId);
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.BUILD_NOTE_FAILED);
        }

        var manifest = dreamItem.manifest();
        var transcriptPath = manifest.transcriptPath();
        var cleanedPath = manifest.cleanedPath();
        var analysisPath = manifest.analysisPath();

        if (transcriptPath == null || cleanedPath == null || analysisPath == null
            || !Files.exists(transcriptPath) || !Files.exists(cleanedPath) || !Files.exists(analysisPath)) {
            return CommandResult.usageError(CliTexts.BUILD_NOTE_FAILED);
        }

        try {
            if (!force && noteProtectionService.isProtected(manifest)) {
                return CommandResult.safetyBlock(CliTexts.NOTE_OVERWRITE_PROTECTED);
            }

            var transcriptText = Files.readString(transcriptPath);
            var cleanedDocument = CleanedDocument.read(cleanedPath, transcriptText);
            var analysisDocument = AnalysisDocument.read(analysisPath, cleanedDocument.cleanText());
            return writeNote(config, dreamItem, transcriptText, cleanedDocument, analysisDocument);
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.BUILD_NOTE_FAILED);
        }
    }

    private CommandResult writeNote(
        AppConfig config,
        ManifestStore.DreamItem dreamItem,
        String transcriptText,
        CleanedDocument cleanedDocument,
        AnalysisDocument analysisDocument
    ) throws IOException {
        var manifest = dreamItem.manifest();
        var now = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(now);
        var notePath = config.paths().notesDir().resolve(manifest.dreamId() + ".md").toAbsolutePath().normalize();
        var titleCandidate = analysisDocument.titleCandidate() == null ? null : analysisDocument.titleCandidate().text();
        var titleFinal = finalTitle(manifest, titleCandidate);
        var noteContent = noteRenderer.render(manifest, cleanedDocument, analysisDocument, transcriptText, config, now, runId, titleFinal);

        Files.createDirectories(notePath.getParent());
        FilesystemSupport.atomicWrite(notePath, noteContent);

        var noteHash = HashingSupport.sha256(notePath);
        var attemptCount = manifest.stages() == null || manifest.stages().buildNote() == null
            ? 1
            : manifest.stages().buildNote().attemptCount() + 1;
        var buildNoteStage = new ManifestDocument.ManifestStageState(
            "completed",
            now,
            now,
            attemptCount,
            null,
            null,
            null,
            null,
            null,
            HashingSupport.sha256(noteContent),
            noteHash,
            null,
            null,
            null,
            null,
            java.util.List.of(),
            null,
            notePath.toString(),
            noteHash
        );

        var updatedManifest = manifest
            .withStages(manifest.stages().withBuildNote(buildNoteStage))
            .withCurrentArtifacts(manifest.currentArtifacts().withNotePath(notePath.toString()))
            .withNaming(manifest.naming().withTitleCandidate(titleCandidate, titleFinal))
            .withExecutionState(now, "note_built", runId, null);

        manifestStore.write(dreamItem, updatedManifest);
        return CommandResult.success();
    }

    private String finalTitle(ManifestDocument manifest, String titleCandidate) {
        var recordedAt = manifest.source() == null ? "1970-01-01T00:00:00" : manifest.source().recordedAt();
        var ordinal = manifest.naming() == null ? "I" : manifest.naming().dreamDayOrdinal();
        var subtitle = titleCandidate == null || titleCandidate.isBlank() ? "Sin titulo" : titleCandidate;
        return "Sueno " + recordedAt.substring(0, 10).replace('-', '_') + "(" + ordinal + ") - " + subtitle;
    }
}
