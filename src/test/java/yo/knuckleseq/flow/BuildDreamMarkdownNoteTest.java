package yo.knuckleseq.flow;

import yo.knuckleseq.support.MainCommandLineTestSupport;
import yo.knuckleseq.support.fixtures.SonologTestFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDreamMarkdownNoteTest {

    @Test
    void givenAnalyzedDreamWhenBuildingTheNoteThenTheNoteFileIsWritten(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

        var result = MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        assertAll(
            () -> assertEquals(0, result.exitCode()),
            () -> assertTrue(Files.exists(fixture.notePath()))
        );
    }

    @Test
    void givenAnalyzedDreamWhenBuildingTheNoteThenItRendersOrderedYamlFrontmatter(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

        MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        var note = Files.readString(fixture.notePath());

        assertAll(
            () -> assertTrue(note.startsWith("---\n")),
            () -> assertTrue(note.contains("type: dream")),
            () -> assertTrue(note.contains("dream_id: 2026-03-14_001")),
            () -> assertTrue(note.contains("sonolog:"))
        );
    }

    @Test
    void givenAnalyzedDreamWhenBuildingTheNoteThenItRendersTheRequiredMarkdownSectionsInOrder(@TempDir Path tempDir)
        throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

        MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        var note = Files.readString(fixture.notePath());

        assertAll(
            () -> assertTrue(note.contains("# Sueno")),
            () -> assertTrue(note.contains("## Version depurada")),
            () -> assertTrue(note.contains("## Elementos extraidos")),
            () -> assertTrue(note.contains("## Ambiguedades")),
            () -> assertTrue(note.contains("## Transcripcion cruda")),
            () -> assertTrue(note.contains("## Trazabilidad"))
        );
    }

    @Test
    void givenAnalyzedDreamWhenBuildingTheNoteThenTheStoredGeneratedNoteHashIsUpdated(@TempDir Path tempDir) throws Exception {
        var appHome = SonologTestFixtures.createAppHome(tempDir);
        var configPath = SonologTestFixtures.writeValidConfig(appHome);
        var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

        MainCommandLineTestSupport.runMain(
            tempDir,
            "build-note",
            fixture.dreamId(),
            "--config",
            configPath.toString()
        );

        var manifest = Files.readString(fixture.manifestPath());

        assertAll(
            () -> assertTrue(manifest.contains("\"buildNote\": {")),
            () -> assertTrue(manifest.contains("\"noteHash\"")),
            () -> assertTrue(manifest.contains("\"notePath\""))
        );
    }

    @Nested
    class AssembleFinalTitle {

        @Test
        void givenSafeTitleCandidateWhenAssemblingTheFinalTitleThenTheCandidateIsUsed(@TempDir Path tempDir) throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

            MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            var note = Files.readString(fixture.notePath());

            assertTrue(note.contains("Doctor excentrico"));
        }

        @Test
        void givenMissingTitleCandidateWhenAssemblingTheFinalTitleThenTheFallbackTitleIsUsed(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");
            var analysisWithoutTitle = Files.readString(fixture.analysisPath())
                .replace(
                    """
                      "titleCandidate": {
                        "text": "Doctor excentrico",
                        "certainty": "explicit",
                        "evidence": [
                          {
                            "text": "doctor excentrico",
                            "source": "cleaned",
                            "segmentIds": [1]
                          }
                        ]
                      },
                    """,
                    ""
                );
            Files.writeString(fixture.analysisPath(), analysisWithoutTitle);

            MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            var note = Files.readString(fixture.notePath());

            assertTrue(note.contains("Sin titulo"));
        }
    }

    @Nested
    class ValidateRequiredUpstreamArtifacts {

        @Test
        void givenAllRequiredUpstreamArtifactsWhenValidatingUpstreamArtifactsThenTheNoteIsBuilt(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(0, result.exitCode());
        }

        @Test
        void givenMissingCleanedArtifactWhenValidatingUpstreamArtifactsThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");
            Files.deleteIfExists(fixture.cleanedPath());

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(2, result.exitCode());
        }

        @Test
        void givenMissingAnalysisArtifactWhenValidatingUpstreamArtifactsThenTheCommandFails(@TempDir Path tempDir)
            throws Exception {
            var appHome = SonologTestFixtures.createAppHome(tempDir);
            var configPath = SonologTestFixtures.writeValidConfig(appHome);
            var fixture = SonologTestFixtures.createAnalyzedDreamItem(appHome, "2026-03-14_001");
            Files.deleteIfExists(fixture.analysisPath());

            var result = MainCommandLineTestSupport.runMain(
                tempDir,
                "build-note",
                fixture.dreamId(),
                "--config",
                configPath.toString()
            );

            assertEquals(2, result.exitCode());
        }
    }
}
