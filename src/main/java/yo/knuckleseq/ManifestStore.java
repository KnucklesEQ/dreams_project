package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class ManifestStore {

    DreamItem load(AppConfig config, String dreamId) throws IOException {
        var manifestPath = manifestPath(config, dreamId);
        if (!Files.exists(manifestPath)) {
            throw new IOException("Dream item manifest not found");
        }

        return new DreamItem(manifestPath, ManifestDocument.read(manifestPath));
    }

    List<DreamItem> loadAll(AppConfig config) throws IOException {
        try (Stream<Path> paths = Files.walk(config.paths().workspaceDir())) {
            return paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .sorted()
                .map(this::readDreamItem)
                .filter(Objects::nonNull)
                .toList();
        }
    }

    void write(Path manifestPath, ManifestDocument manifest) throws IOException {
        FilesystemSupport.atomicWrite(manifestPath, JsonSupport.toStableJson(manifest));
    }

    void write(DreamItem item, ManifestDocument manifest) throws IOException {
        write(item.manifestPath(), manifest);
    }

    Path manifestPath(AppConfig config, String dreamId) {
        return config.paths().workspaceDir().resolve(dreamId).resolve("manifest.json").toAbsolutePath().normalize();
    }

    private DreamItem readDreamItem(Path manifestPath) {
        try {
            return new DreamItem(manifestPath.toAbsolutePath().normalize(), ManifestDocument.read(manifestPath));
        } catch (IOException exception) {
            return null;
        }
    }

    record DreamItem(Path manifestPath, ManifestDocument manifest) {
    }
}
