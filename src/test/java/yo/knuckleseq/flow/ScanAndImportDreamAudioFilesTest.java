package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanAndImportDreamAudioFilesTest {

    @Test
    void givenNewRecorderAudioWhenScanningThenEligibleWaveFilesAreDiscovered(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "fake-wav-audio");

        var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(countWorkspaceItems(appHome) >= 1)
        );
    }

    @Test
    void givenNewRecorderAudioWhenScanningThenDreamIdentityAndFrozenNamingMetadataAreAssigned(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "fake-wav-audio");

        MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        var manifest = Files.readString(firstManifestPath(appHome));

        assertAll(
            () -> assertTrue(manifest.contains("\"dreamId\"")),
            () -> assertTrue(manifest.contains("\"dreamDayIndex\"")),
            () -> assertTrue(manifest.contains("\"dreamDayOrdinal\"")),
            () -> assertTrue(manifest.contains("\"ordinalFrozen\": true"))
        );
    }

    @Test
    void givenNewRecorderAudioWhenScanningThenTheOriginalAudioIsArchivedAndTheManifestIsCreated(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var inputPath = appHome.resolve("input/A-20250108-071440.wav");
        Files.writeString(inputPath, "fake-wav-audio");

        var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertFalse(Files.exists(inputPath)),
            () -> assertTrue(Files.exists(appHome.resolve("archive/A-20250108-071440.wav"))),
            () -> assertTrue(countWorkspaceItems(appHome) >= 1)
        );
    }

    @Test
    void givenNoNewAudioWhenScanningThenTheCommandBehavesLikeANoOp(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);

        MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertEquals(0, countWorkspaceItems(appHome)),
            () -> assertEquals(0, countArchivedAudioFiles(appHome))
        );
    }

    @Nested
    class ParseRecorderFilename {

        @Test
        void givenSupportedRecorderFilenameWhenParsingRecorderFilenameThenRecordedAtIsStoredInTheManifest(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "fake-wav-audio");

            MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

            var manifest = Files.readString(firstManifestPath(appHome));

            assertAll(
                () -> assertTrue(manifest.contains("2025-01-08T07:14:40")),
                () -> assertTrue(manifest.contains("\"recordedAtSource\": \"filename\""))
            );
        }

        @Test
        void givenUnsupportedRecorderFilenameWhenParsingRecorderFilenameThenTheCommandFailsClearly(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/invalid-name.wav"), "fake-wav-audio");

            var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

            assertAll(
                () -> assertEquals(2, result.exitCode()),
                () -> assertFalse(result.stderr().isBlank() && result.stdout().isBlank())
            );
        }
    }

    @Nested
    class DetectDuplicateAudio {

        @Test
        void givenNewAudioHashWhenDetectingDuplicatesThenANewLogicalItemIsCreated(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "audio-one");

            var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(1, countWorkspaceItems(appHome))
            );
        }

        @Test
        void givenKnownAudioHashWhenDetectingDuplicatesThenASecondLogicalItemIsNotCreated(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            Files.writeString(appHome.resolve("input/A-20250108-071440.wav"), "same-audio");
            MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());
            Files.writeString(appHome.resolve("input/A-20250108-081440.wav"), "same-audio");

            var result = MainCommandLineTestSupport.runMain("scan", "--config", configPath.toString());

            assertAll(
                () -> assertEquals(0, result.exitCode()),
                () -> assertEquals(1, countWorkspaceItems(appHome))
            );
        }
    }

    private long countWorkspaceItems(Path appHome) throws IOException {
        try (Stream<Path> paths = Files.list(appHome.resolve("workspace"))) {
            return paths.filter(Files::isDirectory).count();
        }
    }

    private long countArchivedAudioFiles(Path appHome) throws IOException {
        try (Stream<Path> paths = Files.list(appHome.resolve("archive"))) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private Path firstManifestPath(Path appHome) throws IOException {
        try (Stream<Path> paths = Files.walk(appHome.resolve("workspace"))) {
            return paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElseThrow();
        }
    }
}
