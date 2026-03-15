package yo.knuckleseq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ProcessService {

    private final ManifestStore manifestStore;
    private final TranscribeService transcribeService;
    private final CleanService cleanService;
    private final AnalyzeService analyzeService;
    private final BuildNoteService buildNoteService;
    private final RunStore runStore;

    ProcessService() {
        this(new ManifestStore(), new TranscribeService(), new CleanService(), new AnalyzeService(), new BuildNoteService(), new RunStore());
    }

    ProcessService(
        ManifestStore manifestStore,
        TranscribeService transcribeService,
        CleanService cleanService,
        AnalyzeService analyzeService,
        BuildNoteService buildNoteService,
        RunStore runStore
    ) {
        this.manifestStore = manifestStore;
        this.transcribeService = transcribeService;
        this.cleanService = cleanService;
        this.analyzeService = analyzeService;
        this.buildNoteService = buildNoteService;
        this.runStore = runStore;
    }

    CommandResult processKnownItem(AppConfig config, String dreamId) {
        var startedAt = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(startedAt);
        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
            manifestStore.load(config, dreamId);
            var outcome = processItemInternal(config, dreamId, runId, startedAt);

            if (outcome.itemResult() != null && outcome.exitCode() != 2) {
                writeRun(config, startedAt, runId, outcome.exitCode() == 0 ? "completed" : "partial", List.of(outcome.itemResult()), "process " + dreamId);
            }

            return switch (outcome.exitCode()) {
                case 0 -> CommandResult.success();
                case 1 -> new CommandResult(1, "", "");
                default -> CommandResult.usageError(CliTexts.PROCESS_FAILED);
            };
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.PROCESS_FAILED);
        }
    }

    CommandResult processAll(AppConfig config) {
        var startedAt = TimeSupport.nowUtc();
        var runId = TimeSupport.runIdFromTimestamp(startedAt);

        try {
            FilesystemSupport.ensureRuntimeDirectories(config);
            var eligibleItems = manifestStore.loadAll(config).stream()
                .filter(item -> !item.manifest().isNoteFresh())
                .toList();

            if (eligibleItems.isEmpty()) {
                return CommandResult.success();
            }

            var itemResults = new ArrayList<RunDocument.ItemResult>();
            boolean anyFailures = false;

            for (var item : eligibleItems) {
                var outcome = processItemInternal(config, item.manifest().dreamId(), runId, startedAt);
                if (outcome.itemResult() != null) {
                    itemResults.add(outcome.itemResult());
                }

                if (outcome.exitCode() != 0) {
                    anyFailures = true;
                    if (!config.execution().continueOnError()) {
                        break;
                    }
                }
            }

            writeRun(config, startedAt, runId, anyFailures ? "partial" : "completed", itemResults, "process");
            return anyFailures ? new CommandResult(1, "", "") : CommandResult.success();
        } catch (IOException exception) {
            return CommandResult.usageError(CliTexts.PROCESS_FAILED);
        }
    }

    private ItemExecution processItemInternal(AppConfig config, String dreamId, String runId, String startedAt) throws IOException {
        var stageResults = new ArrayList<RunDocument.StageResult>();
        boolean anyCompleted = false;

        for (var stageName : List.of("transcribe", "clean", "analyze", "buildNote")) {
            var currentManifest = reloadManifest(config, dreamId);
            var freshBefore = isFresh(stageName, currentManifest);
            if (freshBefore) {
                stageResults.add(new RunDocument.StageResult(stageName, "skipped", attemptCount(stageName, currentManifest)));
                continue;
            }

            var stageResult = runStage(stageName, config, dreamId);
            if (stageResult.exitCode() == 0) {
                anyCompleted = true;
                var updatedManifest = reloadManifest(config, dreamId);
                stageResults.add(new RunDocument.StageResult(stageName, "completed", attemptCount(stageName, updatedManifest)));
                continue;
            }

            stageResults.add(new RunDocument.StageResult(stageName, "failed", attemptCount(stageName, currentManifest) + 1));

            if (!anyCompleted && stageResult.exitCode() == 2) {
                return new ItemExecution(2, itemResult(config, dreamId, currentManifest, stageResults, "failed", stageName, stageResult));
            }

            markFailed(config, dreamId, runId, startedAt, stageName, stageResult);
            var manifestAfterFailure = reloadManifest(config, dreamId);
            return new ItemExecution(1, itemResult(config, dreamId, manifestAfterFailure, stageResults, anyCompleted ? "partial" : "failed", stageName, stageResult));
        }

        var finalManifest = reloadManifest(config, dreamId);
        return new ItemExecution(0, itemResult(config, dreamId, finalManifest, stageResults, "completed", null, null));
    }

    private ManifestDocument reloadManifest(AppConfig config, String dreamId) throws IOException {
        return manifestStore.load(config, dreamId).manifest();
    }

    private void markFailed(AppConfig config, String dreamId, String runId, String timestamp, String stageName, CommandResult stageResult) throws IOException {
        var dreamItem = manifestStore.load(config, dreamId);
        var manifest = dreamItem.manifest();
        var error = new ManifestDocument.ManifestErrorInfo(stageName, "Stage failed", true, timestamp);
        manifestStore.write(dreamItem, manifest.withExecutionState(timestamp, "failed", runId, error));
    }

    private RunDocument.ItemResult itemResult(
        AppConfig config,
        String dreamId,
        ManifestDocument manifest,
        List<RunDocument.StageResult> stageResults,
        String result,
        String failureCode,
        CommandResult stageResult
    ) {
        return new RunDocument.ItemResult(
            manifest.source() == null ? null : manifest.source().detectedInputPath(),
            manifest.source() == null ? null : manifest.source().archivedPath(),
            manifest.source() == null ? null : manifest.source().recordedAt(),
            manifest.dreamId(),
            manifestStore.manifestPath(config, dreamId).toString(),
            manifest.notePath() == null ? null : manifest.notePath().toString(),
            result,
            List.copyOf(stageResults),
            List.of(),
            failureCode == null ? null : new RunDocument.RunError(failureCode, stageResult.stderr().isBlank() ? stageResult.stdout() : stageResult.stderr())
        );
    }

    private void writeRun(AppConfig config, String startedAt, String runId, String status, List<RunDocument.ItemResult> items, String command) throws IOException {
        int completed = 0;
        int failed = 0;
        int skipped = 0;
        var errors = new ArrayList<RunDocument.RunError>();

        for (var item : items) {
            switch (item.result()) {
                case "completed" -> completed++;
                case "failed", "partial" -> failed++;
                case "skipped" -> skipped++;
                default -> {
                }
            }
            if (item.error() != null) {
                errors.add(item.error());
            }
        }

        var runDocument = new RunDocument(
            1,
            runId,
            startedAt,
            TimeSupport.nowUtc(),
            status,
            "0.1",
            config.pipeline().locale(),
            "sonolog " + command,
            List.of("transcribe", "clean", "analyze", "buildNote"),
                new RunDocument.EffectiveConfig(
                    config.configPath().toString(),
                    HashingSupport.sha256(config.configPath()),
                    config.appHomeDir().toString(),
                    config.paths().inputDir().toString(),
                config.paths().archiveDir().toString(),
                config.paths().notesDir().toString(),
                config.paths().workspaceDir().toString(),
                config.pipeline().promptSet(),
                new RunDocument.OpenAiConfigSnapshot(
                    config.openai().transcriptionModel(),
                    config.openai().cleanupModel(),
                    config.openai().analysisModel()
                ),
                new RunDocument.ExecutionSnapshot(config.execution().continueOnError(), config.execution().maxParallelFiles())
            ),
            items,
            new RunDocument.Totals(items.size(), completed, failed, skipped),
            List.of(),
            errors
        );

        runStore.write(config, runDocument);
    }

    private int attemptCount(String stageName, ManifestDocument manifest) {
        return switch (stageName) {
            case "transcribe" -> manifest.stages().transcribe() == null ? 0 : manifest.stages().transcribe().attemptCount();
            case "clean" -> manifest.stages().clean() == null ? 0 : manifest.stages().clean().attemptCount();
            case "analyze" -> manifest.stages().analyze() == null ? 0 : manifest.stages().analyze().attemptCount();
            case "buildNote" -> manifest.stages().buildNote() == null ? 0 : manifest.stages().buildNote().attemptCount();
            default -> 0;
        };
    }

    private boolean isFresh(String stageName, ManifestDocument manifest) {
        return switch (stageName) {
            case "transcribe" -> manifest.isTranscriptionFresh();
            case "clean" -> manifest.isCleanFresh();
            case "analyze" -> manifest.isAnalysisFresh();
            case "buildNote" -> manifest.isNoteFresh();
            default -> false;
        };
    }

    private CommandResult runStage(String stageName, AppConfig config, String dreamId) {
        return switch (stageName) {
            case "transcribe" -> transcribeService.execute(config, dreamId, false);
            case "clean" -> cleanService.execute(config, dreamId, false);
            case "analyze" -> analyzeService.execute(config, dreamId, false);
            case "buildNote" -> buildNoteService.execute(config, dreamId, false);
            default -> CommandResult.usageError(CliTexts.PROCESS_FAILED);
        };
    }

    private record ItemExecution(int exitCode, RunDocument.ItemResult itemResult) {
    }
}
