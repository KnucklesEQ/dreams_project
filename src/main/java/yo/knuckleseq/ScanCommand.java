package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

final class ScanCommand {

    private static final DateTimeFormatter RECORDER_FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    CommandResult execute(List<String> arguments) {
        Path configPath;

        try {
            configPath = parseConfigPath(arguments);
        } catch (IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.SCAN_USAGE);
        }

        try {
            var config = AppConfig.load(configPath);
            FilesystemSupport.ensureRuntimeDirectories(config);
            return scanInputDirectory(config);
        } catch (IOException | IllegalArgumentException exception) {
            return CommandResult.usageError(CliTexts.SCAN_FAILED);
        }
    }

    private Path parseConfigPath(List<String> arguments) {
        Path configPath = AppConfig.defaultConfigPath();

        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);

            if (!"--config".equals(argument) || index + 1 >= arguments.size()) {
                throw new IllegalArgumentException("Invalid scan options");
            }

            configPath = Path.of(arguments.get(++index));
        }

        return configPath.toAbsolutePath().normalize();
    }

    private CommandResult scanInputDirectory(AppConfig config) throws IOException {
        var candidates = inputAudioFiles(config.paths().inputDir());

        if (candidates.isEmpty()) {
            return CommandResult.success(CliTexts.NO_NEW_AUDIO);
        }

        var knownHashes = knownAudioHashes(config.paths().workspaceDir());
        var importedItems = 0;

        for (var candidate : candidates) {
            var metadata = parseRecorderMetadata(candidate);
            if (metadata == null) {
                return CommandResult.usageError(CliTexts.UNSUPPORTED_AUDIO_NAME);
            }

            var audioHash = HashingSupport.sha256(candidate);
            var archivePath = archivePathFor(config, candidate.getFileName().toString());

            if (knownHashes.contains(audioHash)) {
                Files.move(candidate, archivePath, StandardCopyOption.REPLACE_EXISTING);
                continue;
            }

            var dreamDayIndex = nextDreamDayIndex(config.paths().workspaceDir(), metadata.recordedAt().toLocalDate());
            var dreamId = metadata.recordedAt().toLocalDate() + "_" + String.format(Locale.ROOT, "%03d", dreamDayIndex);
            var workspaceDir = config.paths().workspaceDir().resolve(dreamId);
            Files.createDirectories(workspaceDir);
            Files.move(candidate, archivePath, StandardCopyOption.REPLACE_EXISTING);
            writeManifest(config, workspaceDir.resolve("manifest.json"), dreamId, metadata, archivePath, audioHash, dreamDayIndex);
            knownHashes.add(audioHash);
            importedItems++;
        }

        return CommandResult.success(importedItems == 0 ? CliTexts.NO_NEW_AUDIO : CliTexts.SCAN_OK);
    }

    private List<Path> inputAudioFiles(Path inputDir) throws IOException {
        try (Stream<Path> paths = Files.list(inputDir)) {
            return paths
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".wav"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        }
    }

    private Set<String> knownAudioHashes(Path workspaceDir) throws IOException {
        var hashes = new HashSet<String>();

        try (Stream<Path> paths = Files.walk(workspaceDir)) {
            paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .forEach(path -> hashes.add(loadSourceHash(path)));
        }

        hashes.remove("");
        return hashes;
    }

    private String loadSourceHash(Path manifestPath) {
        try {
            var manifest = ManifestDocument.read(manifestPath);
            return manifest.source() == null || manifest.source().sha256() == null ? "" : manifest.source().sha256();
        } catch (IOException exception) {
            return "";
        }
    }

    private RecorderMetadata parseRecorderMetadata(Path inputPath) {
        var fileName = inputPath.getFileName().toString();
        if (!fileName.matches("A-\\d{8}-\\d{6}\\.wav")) {
            return null;
        }

        var timestamp = fileName.substring(2, 17);
        var recordedAt = LocalDateTime.parse(timestamp, RECORDER_FILENAME_TIMESTAMP);
        return new RecorderMetadata(fileName, recordedAt, inputPath);
    }

    private Path archivePathFor(AppConfig config, String fileName) throws IOException {
        Files.createDirectories(config.paths().archiveDir());
        return config.paths().archiveDir().resolve(fileName);
    }

    private int nextDreamDayIndex(Path workspaceDir, LocalDate recordingDate) throws IOException {
        var indexes = new ArrayList<Integer>();

        try (Stream<Path> paths = Files.walk(workspaceDir)) {
            paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .forEach(path -> loadDreamDayIndex(path, recordingDate).ifPresent(indexes::add));
        }

        return indexes.stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private java.util.Optional<Integer> loadDreamDayIndex(Path manifestPath, LocalDate recordingDate) {
        try {
            var manifest = ManifestDocument.read(manifestPath);
            var recordedAt = manifest.source() == null ? "" : manifest.source().recordedAt();

            if (recordedAt.isBlank() || !recordingDate.equals(LocalDate.parse(recordedAt.substring(0, 10)))) {
                return java.util.Optional.empty();
            }

            return java.util.Optional.of(manifest.naming() == null ? 0 : manifest.naming().dreamDayIndex());
        } catch (Exception exception) {
            return java.util.Optional.empty();
        }
    }

    private void writeManifest(
        AppConfig config,
        Path manifestPath,
        String dreamId,
        RecorderMetadata metadata,
        Path archivePath,
        String sourceHash,
        int dreamDayIndex
    ) throws IOException {
        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString();
        var romanOrdinal = toRoman(dreamDayIndex);
        var manifest = ManifestDocument.imported(
            config,
            new ImportedDreamDraft(
                dreamId,
                metadata.detectedInputPath().toAbsolutePath().normalize().toString(),
                archivePath.toAbsolutePath().normalize().toString(),
                metadata.fileName(),
                sourceHash,
                Files.size(archivePath),
                metadata.recordedAt().toString(),
                dreamDayIndex,
                romanOrdinal,
                "Sueno " + metadata.recordedAt().toLocalDate().toString().replace('-', '_') + "(" + romanOrdinal + ") - Sin titulo"
            ),
            now,
            HashingSupport.sha256(config.configPath()),
            now.replace(":", "-").replace("+00-00", "Z")
        );

        FilesystemSupport.atomicWrite(manifestPath, JsonSupport.toStableJson(manifest));
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(number);
        };
    }

    private record RecorderMetadata(String fileName, LocalDateTime recordedAt, Path detectedInputPath) {
    }
}
