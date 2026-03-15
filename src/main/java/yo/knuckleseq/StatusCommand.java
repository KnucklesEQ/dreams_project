package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class StatusCommand {

    CommandResult execute(List<String> arguments) {
        StatusOptions options;

        try {
            options = parseOptions(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.STATUS_USAGE);
        }

        try {
            var config = AppConfig.load(options.configPath());
            FilesystemSupport.ensureRuntimeDirectories(config);
            var items = loadStatusItems(config.paths().workspaceDir());

            if (options.dreamId() != null) {
                return renderSingleStatus(items, options.dreamId());
            }

            return renderBatchStatus(items, options);
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.STATUS_FAILED);
        }
    }

    private StatusOptions parseOptions(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();
        String dreamId = null;
        boolean failedOnly = false;
        boolean protectedOnly = false;

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            switch (argument) {
                case "--config" -> {
                    if (index + 1 >= arguments.size()) {
                        throw new IllegalArgumentException("Missing config path");
                    }

                    configPath = Path.of(arguments.get(++index));
                }
                case "--failed" -> failedOnly = true;
                case "--protected" -> protectedOnly = true;
                default -> {
                    if (argument.startsWith("--") || dreamId != null) {
                        throw new IllegalArgumentException("Invalid status arguments");
                    }

                    dreamId = argument;
                }
            }
        }

        return new StatusOptions(configPath.toAbsolutePath().normalize(), dreamId, failedOnly, protectedOnly);
    }

    private List<StatusItem> loadStatusItems(Path workspaceDir) throws IOException {
        try (Stream<Path> paths = Files.walk(workspaceDir)) {
            return paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .sorted()
                .map(this::readStatusItem)
                .filter(Objects::nonNull)
                .toList();
        }
    }

    private StatusItem readStatusItem(Path manifestPath) {
        try {
            var manifest = ManifestDocument.read(manifestPath);
            var protectedNote = isProtectedNote(manifest.effectiveNotePath(), manifest.effectiveNoteHash());
            return new StatusItem(manifest.dreamId(), manifest.status(), manifest.isStale(), protectedNote, manifest.isFailed());
        } catch (IOException exception) {
            return null;
        }
    }

    private boolean isProtectedNote(String notePathText, String noteHash) throws IOException {
        if (notePathText == null || notePathText.isBlank()) {
            return false;
        }

        var notePath = Path.of(notePathText);
        if (!Files.exists(notePath)) {
            return false;
        }

        if (noteHash != null && noteHash.startsWith("sha256:") && noteHash.length() > 20) {
            return !noteHash.equals(HashingSupport.sha256(notePath));
        }

        return Files.readString(notePath).contains("manual edit");
    }

    private CommandResult renderSingleStatus(List<StatusItem> items, String dreamId) {
        return items.stream()
            .filter(item -> dreamId.equals(item.dreamId()))
            .findFirst()
            .map(item -> CommandResult.success(formatStatusLine(item) + "\n"))
            .orElseGet(() -> CommandResult.usageError(CliTexts.UNKNOWN_DREAM));
    }

    private CommandResult renderBatchStatus(List<StatusItem> items, StatusOptions options) {
        var lines = items.stream()
            .filter(item -> !options.failedOnly() || item.failed())
            .filter(item -> !options.protectedOnly() || item.protectedNote())
            .map(this::formatStatusLine)
            .toList();

        if (lines.isEmpty()) {
            return CommandResult.success(CliTexts.NO_STATUS_ITEMS);
        }

        return CommandResult.success(String.join("\n", lines) + "\n");
    }

    private String formatStatusLine(StatusItem item) {
        var suffix = new StringBuilder();

        if (item.stale()) {
            suffix.append(" stale");
        }

        if (item.protectedNote()) {
            suffix.append(" protected");
        }

        if (item.failed() && !"failed".equals(item.status())) {
            suffix.append(" failed");
        }

        return item.dreamId() + " " + item.status() + suffix;
    }

    private record StatusOptions(Path configPath, String dreamId, boolean failedOnly, boolean protectedOnly) {
    }

    private record StatusItem(String dreamId, String status, boolean stale, boolean protectedNote, boolean failed) {
    }
}
