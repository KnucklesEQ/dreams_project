package yo.knuckleseq.support.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SonologTestFixtures {

    public static record DreamItemFixture(
        String dreamId,
        Path archivedAudioPath,
        Path manifestPath,
        Path transcriptPath,
        Path cleanedPath,
        Path analysisPath,
        Path notePath
    ) {
    }

    private SonologTestFixtures() {
    }

    public static Path createAppHome(Path rootDirectory) throws IOException {
        var appHome = rootDirectory.resolve("sonolog-home");

        Files.createDirectories(appHome.resolve("config"));
        Files.createDirectories(appHome.resolve("input"));
        Files.createDirectories(appHome.resolve("archive"));
        Files.createDirectories(appHome.resolve("notes"));
        Files.createDirectories(appHome.resolve("workspace"));
        Files.createDirectories(appHome.resolve("runs"));
        Files.createDirectories(appHome.resolve("logs"));
        Files.createDirectories(appHome.resolve("tmp"));

        return appHome;
    }

    public static Path writeValidConfig(Path appHome) throws IOException {
        return writeConfig(appHome, validConfigJson(appHome));
    }

    public static Path writeConfig(Path appHome, String configJson) throws IOException {
        Files.createDirectories(appHome.resolve("config"));

        var configPath = appHome.resolve("config/config.json");
        Files.writeString(configPath, configJson);
        return configPath;
    }

    public static String validConfigJson(Path appHome) {
        var normalizedAppHome = appHome.toAbsolutePath().normalize();

        return """
            {
              "schemaVersion": 1,
              "appHomeDir": "%s",
              "paths": {
                "inputDir": "%s",
                "archiveDir": "%s",
                "notesDir": "%s",
                "workspaceDir": "%s",
                "runsDir": "%s",
                "logsDir": "%s",
                "tmpDir": "%s"
              },
              "openai": {
                "apiKeyRef": "env:OPENAI_API_KEY",
                "baseUrl": "https://api.openai.com/v1",
                "transcriptionModel": "whisper-1",
                "cleanupModel": "gpt-4.1-mini",
                "analysisModel": "gpt-4.1-mini",
                "timeoutSeconds": 120,
                "maxRetries": 3
              },
              "pipeline": {
                "locale": "es",
                "promptSet": "es-v1",
                "skipIfUnchanged": true
              },
              "execution": {
                "maxParallelFiles": 2,
                "continueOnError": true
              }
            }
            """.formatted(
            escape(normalizedAppHome),
            escape(normalizedAppHome.resolve("input")),
            escape(normalizedAppHome.resolve("archive")),
            escape(normalizedAppHome.resolve("notes")),
            escape(normalizedAppHome.resolve("workspace")),
            escape(normalizedAppHome.resolve("runs")),
            escape(normalizedAppHome.resolve("logs")),
            escape(normalizedAppHome.resolve("tmp"))
        );
    }

    public static DreamItemFixture createImportedDreamItem(Path appHome, String dreamId) throws IOException {
        return createDreamItem(appHome, dreamId, "imported", false, false, false, false);
    }

    public static DreamItemFixture createTranscribedDreamItem(Path appHome, String dreamId) throws IOException {
        return createDreamItem(appHome, dreamId, "transcribed", true, false, false, false);
    }

    public static DreamItemFixture createCleanedDreamItem(Path appHome, String dreamId) throws IOException {
        return createDreamItem(appHome, dreamId, "cleaned", true, true, false, false);
    }

    public static DreamItemFixture createAnalyzedDreamItem(Path appHome, String dreamId) throws IOException {
        return createDreamItem(appHome, dreamId, "analyzed", true, true, true, false);
    }

    public static DreamItemFixture createCompletedDreamItem(Path appHome, String dreamId) throws IOException {
        return createDreamItem(appHome, dreamId, "note_built", true, true, true, true);
    }

    private static DreamItemFixture createDreamItem(
        Path appHome,
        String dreamId,
        String overallStatus,
        boolean includeTranscript,
        boolean includeCleaned,
        boolean includeAnalysis,
        boolean includeNote
    )
        throws IOException {
        createAppHome(appHome.getParent());

        var archivedAudioPath = appHome.resolve("archive/A-20250108-071440.wav");
        var workspacePath = appHome.resolve("workspace").resolve(dreamId);
        var transcriptPath = workspacePath.resolve("stt/transcript.txt");
        var segmentsPath = workspacePath.resolve("stt/segments.json");
        var sttRawPath = workspacePath.resolve("stt/raw-response.json");
        var cleanedPath = workspacePath.resolve("llm/cleaned.json");
        var llmRawPath = workspacePath.resolve("llm/raw-response.json");
        var analysisPath = workspacePath.resolve("analysis/analysis.json");
        var notePath = appHome.resolve("notes").resolve(dreamId + ".md");
        var manifestPath = workspacePath.resolve("manifest.json");

        Files.createDirectories(archivedAudioPath.getParent());
        Files.createDirectories(transcriptPath.getParent());
        Files.createDirectories(cleanedPath.getParent());
        Files.createDirectories(analysisPath.getParent());
        Files.createDirectories(notePath.getParent());

        Files.writeString(archivedAudioPath, "fake-wav-audio");

        if (includeTranscript) {
            Files.writeString(transcriptPath, "transcripcion cruda del sueno");
            Files.writeString(segmentsPath, "[]");
            Files.writeString(sttRawPath, "{}");
        }

        if (includeCleaned) {
            Files.writeString(
                cleanedPath,
                """
                    {
                      "schemaVersion": 1,
                      "dreamId": "%s",
                      "createdAt": "2026-03-14T09:17:00Z",
                      "sourceHash": "sha256:source",
                      "transcriptHash": "sha256:transcript",
                      "promptId": "cleanup-es-v1",
                      "promptHash": "sha256:prompt-clean",
                      "cleanText": "texto limpio y legible",
                      "paragraphs": ["texto limpio y legible"],
                      "changes": [],
                      "uncertainSpans": []
                    }
                    """.formatted(dreamId)
            );
            Files.writeString(llmRawPath, "{}");
        }

        if (includeAnalysis) {
            Files.writeString(
                analysisPath,
                """
                    {
                      "schemaVersion": 1,
                      "dreamId": "%s",
                      "createdAt": "2026-03-14T09:17:40Z",
                      "updatedAt": "2026-03-14T09:17:40Z",
                      "pipelineVersion": "0.1",
                      "locale": "es",
                      "source": {
                        "manifestPath": "%s",
                        "sourceAudioName": "A-20250108-071440.wav",
                        "sourceSha256": "sha256:source",
                        "transcriptHash": "sha256:transcript",
                        "cleanedHash": "sha256:cleaned",
                        "recordedAt": "2025-01-08T07:14:40",
                        "recordedAtSource": "filename",
                        "sourceTextUsed": "cleaned"
                      },
                      "provenance": {
                        "runId": "2026-03-14T09-15-02Z",
                        "provider": "openai",
                        "model": "gpt-4.1-mini",
                        "promptId": "analysis-es-v1",
                        "promptHash": "sha256:prompt-analysis",
                        "inputHash": "sha256:analyze-input"
                      },
                      "review": {
                        "needsReview": true,
                        "reviewReasons": []
                      },
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
                      "entities": {
                        "people": [],
                        "places": [],
                        "emotions": [],
                        "motifs": [],
                        "tags": []
                      },
                      "uncertainties": [],
                      "coverage": {
                        "evidencePolicy": "text_only",
                        "unknownCount": 0
                      }
                    }
                    """.formatted(dreamId, escape(manifestPath.toAbsolutePath().normalize()))
            );
        }

        if (includeNote) {
            Files.writeString(
                notePath,
                """
                    ---
                    type: dream
                    dream_id: %s
                    title: "Sueno 2025_01_08(I) - Doctor excentrico"
                    recorded_at: 2025-01-08T07:14:40
                    created_at: 2026-03-14T09:15:02Z
                    generated_at: 2026-03-14T09:18:11Z
                    language: es
                    needs_review: true
                    tags: []
                    people: []
                    places: []
                    emotions: []
                    motifs: []
                    source_audio_name: A-20250108-071440.wav
                    sonolog:
                      note_schema_version: 1
                      pipeline_version: "0.1"
                      run_id: "2026-03-14T09-15-02Z"
                      dream_day_index: 1
                      dream_day_ordinal: "I"
                      title_candidate: "Doctor excentrico"
                      source_hash: "sha256:source"
                      transcript_hash: "sha256:transcript"
                      cleaned_hash: "sha256:cleaned"
                      analysis_hash: "sha256:analysis"
                      models:
                        transcription: "whisper-1"
                        cleanup: "gpt-4.1-mini"
                        analysis: "gpt-4.1-mini"
                    ---

                    # Sueno 2025_01_08(I) - Doctor excentrico
                    """.formatted(dreamId)
            );
        }

        Files.writeString(
            manifestPath,
            manifestJson(
                dreamId,
                overallStatus,
                archivedAudioPath,
                transcriptPath,
                cleanedPath,
                analysisPath,
                notePath,
                includeTranscript,
                includeCleaned,
                includeAnalysis,
                includeNote
            )
        );

        return new DreamItemFixture(dreamId, archivedAudioPath, manifestPath, transcriptPath, cleanedPath, analysisPath, notePath);
    }

    private static String manifestJson(
        String dreamId,
        String overallStatus,
        Path archivedAudioPath,
        Path transcriptPath,
        Path cleanedPath,
        Path analysisPath,
        Path notePath,
        boolean includeTranscript,
        boolean includeCleaned,
        boolean includeAnalysis,
        boolean includeNote
    ) {
        var workspacePath = transcriptPath.getParent().getParent();

        return """
            {
              "schemaVersion": 1,
              "dreamId": "%s",
              "createdAt": "2026-03-14T09:15:02Z",
              "updatedAt": "2026-03-14T09:18:11Z",
              "pipelineVersion": "0.1",
              "locale": "es",
              "status": "%s",
              "needsReview": true,
              "configFingerprint": "sha256:config",
              "lastRunId": "2026-03-14T09-15-02Z",
              "source": {
                "detectedInputPath": "%s",
                "archivedPath": "%s",
                "originalFileName": "A-20250108-071440.wav",
                "sha256": "sha256:source",
                "sizeBytes": 18,
                "mimeType": "audio/wav",
                "durationMs": 183000,
                "recordedAt": "2025-01-08T07:14:40",
                "recordedAtSource": "filename"
              },
              "naming": {
                "dreamDayIndex": 1,
                "dreamDayOrdinal": "I",
                "ordinalFrozen": true,
                "titleCandidate": "Doctor excentrico",
                "titleFinal": "Sueno 2025_01_08(I) - Doctor excentrico"
              },
              "stages": {
                "import": {
                  "status": "completed",
                  "startedAt": "2026-03-14T09:15:02Z",
                  "finishedAt": "2026-03-14T09:15:03Z",
                  "attemptCount": 1,
                  "warnings": [],
                  "error": null
                },
                "transcribe": %s,
                "clean": %s,
                "analyze": %s,
                "buildNote": %s
              },
              "currentArtifacts": {
                "transcriptPath": %s,
                "cleanedPath": %s,
                "analysisPath": %s,
                "notePath": %s
              },
              "lastError": null
            }
            """.formatted(
            dreamId,
            overallStatus,
            escape(archivedAudioPath),
            escape(archivedAudioPath),
            stageJson(
                "transcribe",
                includeTranscript,
                "provider", "openai",
                "model", "whisper-1",
                "language", "es",
                "inputHash", "sha256:source",
                "outputHash", "sha256:transcript",
                "rawResponsePath", escape(workspacePath.resolve("stt/raw-response.json")),
                "transcriptPath", escape(transcriptPath),
                "segmentsPath", escape(workspacePath.resolve("stt/segments.json"))
            ),
            stageJson(
                "clean",
                includeCleaned,
                "provider", "openai",
                "model", "gpt-4.1-mini",
                "promptId", "cleanup-es-v1",
                "promptHash", "sha256:prompt-clean",
                "inputHash", "sha256:transcript",
                "outputHash", "sha256:cleaned",
                "rawResponsePath", escape(workspacePath.resolve("llm/raw-response.json")),
                "outputPath", escape(cleanedPath)
            ),
            stageJson(
                "analyze",
                includeAnalysis,
                "provider", "openai",
                "model", "gpt-4.1-mini",
                "promptId", "analysis-es-v1",
                "promptHash", "sha256:prompt-analysis",
                "inputHash", "sha256:analyze-input",
                "outputHash", "sha256:analysis",
                "outputPath", escape(analysisPath)
            ),
            stageJson(
                "buildNote",
                includeNote,
                "inputHash", "sha256:note-input",
                "notePath", escape(notePath),
                "noteHash", "sha256:note"
            ),
            jsonStringOrNull(includeTranscript ? escape(transcriptPath) : null),
            jsonStringOrNull(includeCleaned ? escape(cleanedPath) : null),
            jsonStringOrNull(includeAnalysis ? escape(analysisPath) : null),
            jsonStringOrNull(includeNote ? escape(notePath) : null)
        );
    }

    private static String stageJson(String stageName, boolean completed, String... extras) {
        if (!completed) {
            return """
                {
                  "status": "pending",
                  "startedAt": null,
                  "finishedAt": null,
                  "attemptCount": 0,
                  "warnings": [],
                  "error": null
                }
                """;
        }

        var extraLines = new StringBuilder();

        for (int index = 0; index < extras.length; index += 2) {
            extraLines.append(",\n      \"")
                .append(extras[index])
                .append("\": ")
                .append(jsonStringOrNull(extras[index + 1]));
        }

        return """
            {
              "status": "completed",
              "startedAt": "2026-03-14T09:15:03Z",
              "finishedAt": "2026-03-14T09:16:10Z",
              "attemptCount": 1%s,
              "warnings": [],
              "error": null
            }
            """.formatted(extraLines);
    }

    private static String jsonStringOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String escape(Path path) {
        return path.toString().replace("\\", "\\\\");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\");
    }
}
