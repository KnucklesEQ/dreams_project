package yo.knuckleseq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class FilesystemSupport {

    static void ensureRuntimeDirectories(AppConfig config) throws IOException {
        ensureDirectoryReady(config.paths().inputDir());
        ensureDirectoryReady(config.paths().archiveDir());
        ensureDirectoryReady(config.paths().notesDir());
        ensureDirectoryReady(config.paths().workspaceDir());
        ensureDirectoryReady(config.paths().runsDir());
        ensureDirectoryReady(config.paths().logsDir());
        ensureDirectoryReady(config.paths().tmpDir());
    }

    static void ensureDirectoryReady(Path directory) throws IOException {
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IOException("Path is not a directory");
        }

        Files.createDirectories(directory);
    }

    static void atomicWrite(Path targetPath, String content) throws IOException {
        Files.createDirectories(targetPath.getParent());
        var tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        Files.writeString(tempPath, content);
        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private FilesystemSupport() {
    }
}
