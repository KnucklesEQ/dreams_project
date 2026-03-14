# Sonolog Config and Manifest Spec

## Purpose
This document is a self-contained contract for `config.json` and `manifest.json` in Sonolog.

Sonolog is a personal CLI tool for Debian Linux that:
- reads existing dream audio files from a folder,
- transcribes them with OpenAI,
- cleans and structures the text with an OpenAI LLM,
- extracts tags, people, places, emotions and motifs,
- writes one Markdown note per audio for later review in Obsidian,
- preserves originals and intermediate artifacts.

This document is intended to be usable by future sessions or other models without extra conversation context.

## Scope
- Defines the runtime contract for `config.json`.
- Defines the per-audio state contract for `manifest.json`.
- Clarifies how both relate to `run.json`.
- Detailed `analysis.json` and `run.json` contracts live in `docs/analysis-run-spec-v0.1.md`.

## Core rules
- `config.json` is human-edited and should stay small.
- `manifest.json` is machine-written and should be treated as internal state.
- `run.json` is per execution; `manifest.json` is per audio.
- Unknown keys in `config.json` should be treated as validation errors.
- All writes to `manifest.json` must be atomic.
- No secret values should be copied into `manifest.json`.

## Terms
- `appHomeDir`: root folder for Sonolog data and runtime folders.
- `dreamId`: stable identifier for one source audio.
- `runId`: identifier for one CLI execution.
- `stage`: one pipeline step: `import`, `transcribe`, `clean`, `analyze`, `buildNote`.
- `stale`: stage state meaning the stored output is no longer current because an upstream input changed.

## Source of truth
- `config/config.json`: user configuration.
- `archiveDir/...`: archived original audio while the user chooses to retain it.
- `workspace/<dreamId>/manifest.json`: current machine state for one audio.
- `runs/<runId>/run.json`: batch/execution summary.
- `analysis/analysis.json`: canonical structured extraction output for that audio.
- `notes/<dreamId>.md`: human-facing note derived from structured artifacts.

## `config.json`

### Role
- Defines how Sonolog runs on one machine.
- Should be stable across executions.
- Should avoid ephemeral runtime state.

### Location
- Default location: `<appHomeDir>/config/config.json`.

### Path resolution rules
- `~` must expand to the current user home.
- Absolute paths are accepted.
- Relative paths are resolved relative to `appHomeDir`.
- At runtime, Sonolog should normalize all paths to absolute canonical paths.

### Secret handling
- In v1, `openai.apiKeyRef` should support `env:VARNAME`.
- Recommended default: `env:OPENAI_API_KEY`.
- Storing raw API keys directly in `config.json` is not recommended and should not be required by the spec.

### Schema
```json
{
  "schemaVersion": 1,
  "appHomeDir": "~/Documents/sonolog",
  "paths": {
    "inputDir": "~/Documents/sonolog/input",
    "archiveDir": "~/Documents/sonolog/archive",
    "notesDir": "~/Documents/sonolog/notes",
    "workspaceDir": "~/Documents/sonolog/workspace",
    "runsDir": "~/Documents/sonolog/runs",
    "logsDir": "~/Documents/sonolog/logs",
    "tmpDir": "~/Documents/sonolog/tmp"
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
```

### Required fields
- `schemaVersion`
- `appHomeDir`
- `paths.inputDir`
- `paths.notesDir`
- `openai.apiKeyRef`
- `openai.transcriptionModel`
- `openai.cleanupModel`
- `openai.analysisModel`
- `pipeline.locale`
- `pipeline.promptSet`

### Optional fields with defaults
- `paths.archiveDir`: `<appHomeDir>/archive`
- `paths.workspaceDir`: `<appHomeDir>/workspace`
- `paths.runsDir`: `<appHomeDir>/runs`
- `paths.logsDir`: `<appHomeDir>/logs`
- `paths.tmpDir`: `<appHomeDir>/tmp`
- `openai.baseUrl`: `https://api.openai.com/v1`
- `openai.timeoutSeconds`: `120`
- `openai.maxRetries`: `3`
- `execution.maxParallelFiles`: `2`
- `execution.continueOnError`: `true`
- `pipeline.skipIfUnchanged`: `true`

### Field semantics
- `schemaVersion`: config schema version, integer, starts at `1`.
- `appHomeDir`: root folder chosen by the user for Sonolog.
- `paths.inputDir`: source folder for existing audio files, typically recorder `.wav` files in v0.1.
- `paths.archiveDir`: canonical archive folder for original audio files after import.
- `paths.notesDir`: final destination for generated Markdown notes.
- `paths.workspaceDir`: technical artifacts and per-audio manifests.
- `paths.runsDir`: per-execution summaries.
- `paths.logsDir`: application logs.
- `paths.tmpDir`: temporary files.
- `openai.transcriptionModel`: model id used for speech-to-text.
- `openai.cleanupModel`: model id used for cleanup/structuring.
- `openai.analysisModel`: model id used for extraction.
- `pipeline.locale`: language of prompts and output labels; v1 supports only `es`.
- `pipeline.promptSet`: logical prompt bundle id; do not store full prompts in `config.json`.
- `pipeline.skipIfUnchanged`: if `true`, skip stages whose effective inputs did not change.
- `execution.maxParallelFiles`: file-level concurrency cap.
- `execution.continueOnError`: if `true`, batch runs continue after one item fails.

### Validation rules
- `schemaVersion` must be `1` in v0.1.
- Empty strings are invalid.
- `pipeline.locale` must be `es` in v1.
- `timeoutSeconds` must be a positive integer.
- `maxRetries` must be `0` or greater.
- `maxParallelFiles` must be `1` or greater.
- Derived folders should be created automatically if missing.
- `inputDir` and `notesDir` may point outside `appHomeDir`.
- `archiveDir` may point outside `appHomeDir`.
- `inputDir` is treated as temporary ingest space; imported audio should be moved to `archiveDir`.

### Design notes
- `config.json` should not store transient counters, hashes or per-run state.
- Prompt text should live in code or prompt resources, identified by `promptSet` plus stage-level prompt ids.
- Future versions may add more locales without breaking the nested structure.

## `manifest.json`

### Role
- Canonical machine state for one imported audio.
- Tracks current stage status, hashes, timestamps, artifact locations and latest error.
- Must be enough to resume work without rereading unrelated state.

### Location
- `<workspaceDir>/<dreamId>/manifest.json`

### Ownership
- Written only by Sonolog.
- Not intended for manual editing.
- Must be updated atomically using temp file + rename.

### High-level rules
- One manifest per audio.
- Must never embed large raw payloads inline.
- Must not duplicate full analysis content; store paths to artifact files instead.
- Must store enough metadata to decide whether downstream stages are stale.
- Must not contain secret values.

### Schema
```json
{
  "schemaVersion": 1,
  "dreamId": "2026-03-14_001",
  "createdAt": "2026-03-14T09:15:02Z",
  "updatedAt": "2026-03-14T09:18:11Z",
  "pipelineVersion": "0.1",
  "locale": "es",
  "status": "note_built",
  "needsReview": true,
  "configFingerprint": "sha256:...",
  "lastRunId": "2026-03-14T09-15-02Z",
  "source": {
    "detectedInputPath": "/home/pablo/Documents/sonolog/input/A-20250108-071440.wav",
    "archivedPath": "/home/pablo/Documents/sonolog/archive/A-20250108-071440.wav",
    "originalFileName": "A-20250108-071440.wav",
    "sha256": "sha256:...",
    "sizeBytes": 1843200,
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
    "transcribe": {
      "status": "completed",
      "startedAt": "2026-03-14T09:15:03Z",
      "finishedAt": "2026-03-14T09:16:10Z",
      "attemptCount": 1,
      "provider": "openai",
      "model": "whisper-1",
      "language": "es",
      "inputHash": "sha256:...",
      "outputHash": "sha256:...",
      "rawResponsePath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/stt/raw-response.json",
      "transcriptPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/stt/transcript.txt",
      "segmentsPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/stt/segments.json",
      "usage": {
        "requestId": "req_..."
      },
      "warnings": [],
      "error": null
    },
    "clean": {
      "status": "completed",
      "startedAt": "2026-03-14T09:16:10Z",
      "finishedAt": "2026-03-14T09:17:00Z",
      "attemptCount": 1,
      "provider": "openai",
      "model": "gpt-4.1-mini",
      "promptId": "cleanup-es-v1",
      "promptHash": "sha256:...",
      "inputHash": "sha256:...",
      "outputHash": "sha256:...",
      "rawResponsePath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/llm/raw-response.json",
      "outputPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/llm/cleaned.json",
      "warnings": [],
      "error": null
    },
    "analyze": {
      "status": "completed",
      "startedAt": "2026-03-14T09:17:00Z",
      "finishedAt": "2026-03-14T09:17:40Z",
      "attemptCount": 1,
      "provider": "openai",
      "model": "gpt-4.1-mini",
      "promptId": "analysis-es-v1",
      "promptHash": "sha256:...",
      "inputHash": "sha256:...",
      "outputHash": "sha256:...",
      "outputPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/analysis/analysis.json",
      "warnings": [],
      "error": null
    },
    "buildNote": {
      "status": "completed",
      "startedAt": "2026-03-14T09:17:40Z",
      "finishedAt": "2026-03-14T09:18:11Z",
      "attemptCount": 1,
      "inputHash": "sha256:...",
      "notePath": "/home/pablo/Documents/sonolog/notes/2026-03-14_001.md",
      "noteHash": "sha256:...",
      "warnings": [],
      "error": null
    }
  },
  "currentArtifacts": {
    "transcriptPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/stt/transcript.txt",
    "cleanedPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/llm/cleaned.json",
    "analysisPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/analysis/analysis.json",
    "notePath": "/home/pablo/Documents/sonolog/notes/2026-03-14_001.md"
  },
  "lastError": null
}
```

### Top-level fields
- `schemaVersion`: manifest schema version; integer.
- `dreamId`: stable id for this audio.
- `createdAt`: first creation time of the manifest.
- `updatedAt`: last successful manifest write time.
- `pipelineVersion`: version label for the application pipeline.
- `locale`: effective locale for this item.
- `status`: overall item state.
- `needsReview`: should default to `true` in v1.
- `configFingerprint`: hash of relevant effective config excluding secrets.
- `lastRunId`: latest CLI execution that touched this manifest.
- `source`: immutable source-audio metadata.
- `naming`: stable naming metadata used to build the final note title.
- `stages`: per-stage state objects.
- `currentArtifacts`: latest successful paths for downstream use.
- `lastError`: latest unrecovered error, if any.

### `source` object
- `detectedInputPath`: path where the audio was first detected before import.
- `archivedPath`: canonical path of the archived original audio after import.
- `originalFileName`: original basename.
- `sha256`: source file content hash.
- `sizeBytes`: file size.
- `mimeType`: detected MIME type when known.
- `durationMs`: detected audio duration when known.
- `recordedAt`: recording timestamp parsed from the recorder filename when available; in v0.1 this is recorder-local time, not UTC.
- `recordedAtSource`: source used to derive `recordedAt`; in v0.1 expected `filename`.

### `naming` object
- `dreamDayIndex`: integer ordinal assigned at first import for that recording date.
- `dreamDayOrdinal`: uppercase Roman numeral derived from `dreamDayIndex`.
- `ordinalFrozen`: should be `true` once the item is first imported.
- `titleCandidate`: conservative short title candidate, typically from `analysis.json`; may be `null`.
- `titleFinal`: final rendered note title.

Rules:
- `dreamDayIndex` and `dreamDayOrdinal` must be assigned during import and then frozen.
- Sonolog must not renumber older dreams automatically after first assignment.
- `titleFinal` should be deterministic from `recordedAt`, frozen ordinal and `titleCandidate` or fallback text.
- Literal user-facing title format should be `Sue\u00f1o YYYY_MM_DD(<ordinal romano>) - <titulo>`; repo examples may show `Sueno` for ASCII consistency.

### Stage contract
Every stage object must include:
- `status`
- `startedAt`
- `finishedAt`
- `attemptCount`
- `warnings`
- `error`

Recommended supporting objects:

```json
{
  "warnings": [
    {
      "code": "low_confidence_segment",
      "message": "Segment 12 had low confidence"
    }
  ],
  "error": {
    "code": "openai_rate_limit",
    "message": "Rate limit reached",
    "retryable": true,
    "occurredAt": "2026-03-14T09:16:10Z"
  }
}
```

### Stage-specific notes
- `import`: local import/move-to-archive/freeze step; usually no provider/model.
- `import`: should parse `recordedAt` from filenames matching `A-YYYYMMDD-HHMMSS.wav`.
- `import`: should assign and freeze `dreamDayIndex` and `dreamDayOrdinal`.
- `transcribe`: should store provider, model, language, input/output hashes and artifact paths.
- `clean`: should store provider, model, `promptId`, `promptHash`, hashes and output path.
- `analyze`: same pattern as `clean`, but output points to `analysis.json`.
- `buildNote`: should store input hash, note path and note hash of the last Sonolog-generated note.
- `buildNote`: should render `naming.titleFinal` from frozen ordinal metadata plus title candidate or fallback.
- `buildNote`: must not overwrite an externally modified note unless `--force` is used.

### Stage status enum
- `pending`
- `running`
- `completed`
- `failed`
- `stale`
- `skipped`

### Top-level status enum
- `imported`
- `transcribed`
- `cleaned`
- `analyzed`
- `note_built`
- `failed`
- `stale`

### Stale propagation rules
- If `source.sha256` changes, every downstream stage becomes `stale`.
- If the transcript changes, `clean`, `analyze` and `buildNote` become `stale`.
- If the cleaned output changes, `analyze` and `buildNote` become `stale`.
- If the analysis output changes, `buildNote` becomes `stale`.
- If `configFingerprint` changes in a way that affects a stage, that stage and downstream stages become `stale`.
- If the target note file is missing, `buildNote` becomes `stale`.

### Artifact rules
- Paths in `manifest.json` should be stored normalized and absolute.
- `currentArtifacts` should point only to the latest successful artifacts.
- Raw API responses should live in separate files, never embedded inline.
- `analysis.json` is the canonical machine-readable extraction output.
- `note.md` is derived output and may be regenerated from upstream artifacts.
- Successful upstream artifacts must remain available even if a downstream stage fails.
- If the current note file hash differs from the stored generated `noteHash`, Sonolog should treat the note as externally modified and protect it from automatic overwrite.
- Manual deletion of the archived source audio does not invalidate existing transcript, analysis or note artifacts, but prevents future retranscription from the original source.

### Error rules
- `lastError` should mirror the most recent unrecovered stage error.
- A recovered error may remain in stage history only if the implementation chooses to retain it; v0.1 does not require stage history arrays.
- If all stages are healthy, `lastError` should be `null`.

## Relationship with `run.json`
- `manifest.json` answers: "what is the current state of this audio?"
- `run.json` answers: "what happened during this CLI execution?"
- `run.json` is especially useful for batch processing, partial failure, retries and timing summaries.
- `manifest.json` should not try to become a full execution log.

## Implementation notes for Java
- Recommended mapping: dedicated POJOs for `Config`, `PathsConfig`, `OpenAiConfig`, `PipelineConfig`, `ExecutionConfig`, `Manifest`, `StageState`, `WarningInfo`, `ErrorInfo`.
- Use Jackson for JSON parsing/serialization.
- Enable strict deserialization for `config.json` to catch unknown keys early.
- Use deterministic serialization order for easier diffs.

## Status
- This spec refines `docs/spec-v0.1.md`.
- This spec should be read together with `docs/analysis-run-spec-v0.1.md`.
- If both docs disagree, this file is authoritative for `config.json` and `manifest.json`.
