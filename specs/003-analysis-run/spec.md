# Sonolog Analysis and Run Spec

## Purpose
This document is a self-contained contract for `analysis.json` and `runs/<runId>/run.json` in Sonolog.

Sonolog is a personal CLI tool for Debian Linux that:
- reads existing dream audio files from a folder,
- transcribes them with OpenAI,
- cleans and structures the text with an OpenAI LLM,
- extracts tags, people, places, emotions and motifs,
- writes one Markdown note per audio for later review in Obsidian,
- preserves originals and intermediate artifacts.

This document is intended to be usable by future sessions or other models without extra conversation context.

## Scope
- Defines the runtime contract for `analysis.json`.
- Defines the runtime contract for `run.json`.
- Clarifies how both relate to `manifest.json` and `note.md`.
- Leaves room for later refinement of vocabularies and extraction depth.

## Core rules
- `analysis.json` is the canonical semantic extraction output for one audio.
- `run.json` is the summary ledger for one CLI execution.
- `manifest.json` remains the canonical current state file for one audio.
- `note.md` remains a human-facing derived artifact.
- No entity, tag or motif should appear in `analysis.json` without evidence.
- No secrets should be copied into `analysis.json` or `run.json`.
- Execution timestamps should use UTC ISO-8601 strings.
- `recordedAt` is an exception in v0.1 and represents recorder-local time parsed from the filename.
- All stored paths should be normalized absolute paths.

## Terms
- `dreamId`: stable identifier for one source audio.
- `runId`: identifier for one CLI execution, expected to be a timestamp-safe UTC string in v0.1.
- `evidence`: text snippet and source metadata supporting an extracted item.
- `certainty`: confidence level constrained to a small enum in v0.1.
- `source text`: transcript and/or cleaned text used as upstream material.

## Source of truth
- `archiveDir/...`: archived original audio while the user chooses to retain it.
- `workspace/<dreamId>/manifest.json`: current state for one audio.
- `workspace/<dreamId>/analysis/analysis.json`: canonical structured extraction.
- `notes/<dreamId>.md`: human-facing note derived from upstream artifacts.
- `runs/<runId>/run.json`: execution summary across one or more inputs.

## `analysis.json`

### Role
- Stores canonical structured extraction for one audio.
- Feeds Markdown frontmatter and extracted sections.
- Preserves semantic output separately from note formatting.
- Enables future re-rendering of notes without re-running the model.

### Location
- Default location: `<workspaceDir>/<dreamId>/analysis/analysis.json`.

### Ownership
- Written only by Sonolog.
- Not intended for manual editing.
- May be regenerated if the analyze stage is rerun.

### High-level rules
- One `analysis.json` per `dreamId` current state.
- Must not embed full raw API responses.
- Must not duplicate the full transcript or full cleaned text.
- Must store enough provenance to trace the analysis back to upstream artifacts.
- Must stay deterministic in ordering and normalization.

### Schema
```json
{
  "schemaVersion": 1,
  "dreamId": "2026-03-14_001",
  "createdAt": "2026-03-14T09:17:40Z",
  "updatedAt": "2026-03-14T09:17:40Z",
  "pipelineVersion": "0.1",
  "locale": "es",
  "source": {
    "manifestPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/manifest.json",
    "sourceAudioName": "A-20250108-071440.wav",
    "sourceSha256": "sha256:...",
    "transcriptHash": "sha256:...",
    "cleanedHash": "sha256:...",
    "recordedAt": "2025-01-08T07:14:40",
    "recordedAtSource": "filename",
    "sourceTextUsed": "cleaned"
  },
  "provenance": {
    "runId": "2026-03-14T09-15-02Z",
    "provider": "openai",
    "model": "gpt-4.1-mini",
    "promptId": "analysis-es-v1",
    "promptHash": "sha256:...",
    "inputHash": "sha256:..."
  },
  "review": {
    "needsReview": true,
    "reviewReasons": [
      "low_confidence_source"
    ]
  },
  "titleCandidate": {
    "text": "Doctor excentrico",
    "certainty": "explicit",
    "evidence": [
      {
        "text": "habia un doctor rarisimo y excentrico",
        "source": "cleaned",
        "segmentIds": [5]
      }
    ]
  },
  "summary": {
    "oneParagraph": "Sueno narrado en espanol, manteniendo solo contenido presente o marcado como dudoso.",
    "gist": [
      "Aparece la madre del sonador.",
      "El sueno transcurre entre una casa y una playa."
    ]
  },
  "entities": {
    "people": [
      {
        "value": "madre",
        "normalized": "madre",
        "certainty": "explicit",
        "evidence": [
          {
            "text": "mi madre estaba conmigo",
            "source": "cleaned",
            "segmentIds": [12]
          }
        ]
      }
    ],
    "places": [],
    "emotions": [],
    "motifs": [],
    "tags": []
  },
  "timeline": [
    {
      "order": 1,
      "text": "Primero aparece en la casa de infancia.",
      "certainty": "explicit",
      "evidence": [
        {
          "text": "estaba en mi casa de cuando era pequeno",
          "source": "cleaned",
          "segmentIds": [3]
        }
      ]
    }
  ],
  "uncertainties": [
    {
      "type": "inaudible",
      "text": "palabra no determinada",
      "evidence": [
        {
          "text": "[inaudible]",
          "source": "transcript",
          "segmentIds": [7]
        }
      ]
    }
  ],
  "coverage": {
    "evidencePolicy": "text_only",
    "unknownCount": 1
  }
}
```

### Required top-level fields
- `schemaVersion`
- `dreamId`
- `createdAt`
- `updatedAt`
- `pipelineVersion`
- `locale`
- `source`
- `provenance`
- `review`
- `entities`
- `uncertainties`
- `coverage`

### Optional top-level fields
- `titleCandidate`
- `summary`
- `timeline`

### `source` object
- `manifestPath`: absolute path to the matching manifest.
- `sourceAudioName`: original basename of the archived audio.
- `sourceSha256`: source audio hash.
- `transcriptHash`: hash of the current transcript input.
- `cleanedHash`: hash of the current cleaned input.
- `recordedAt`: recording timestamp when available; in v0.1 this is recorder-local time parsed from filename.
- `recordedAtSource`: source used to derive `recordedAt`; in v0.1 expected `filename`.
- `sourceTextUsed`: enum describing which upstream text the analyzer treated as primary input.

Allowed `sourceTextUsed` values:
- `transcript`
- `cleaned`
- `mixed`

### `provenance` object
- `runId`: execution id that produced this analysis.
- `provider`: in v1 expected `openai`.
- `model`: model id used for the analysis stage.
- `promptId`: logical prompt id.
- `promptHash`: hash of the effective prompt text.
- `inputHash`: hash of the effective analyze-stage input.

### `review` object
- `needsReview`: should default to `true` in v0.1.
- `reviewReasons`: machine flags explaining why review is recommended.

Recommended `reviewReasons` values:
- `low_confidence_source`
- `ambiguous_entity`
- `new_entity_risk`
- `unclear_sequence`
- `heavy_inference_risk`

### `titleCandidate`
- Optional in v0.1.
- If present, it should be short, conservative and evidence-backed.
- If no safe title exists, omit it or leave it null depending on implementation choice.
- Build-note should fall back to `Sin titulo` when no candidate is available.

Recommended fields:
- `text`
- `certainty`
- `evidence`

Recommended `certainty` values:
- `explicit`
- `inferred_light`
- `unclear`

### `summary` object
- Optional in v0.1.
- If present, should remain conservative and only restate supported content.
- `oneParagraph`: short prose summary.
- `gist`: short ordered list of short statements.

### `entities` object
- Contains the canonical extracted lists used for note frontmatter and extracted sections.
- Required keys:
  - `people`
  - `places`
  - `emotions`
  - `motifs`
  - `tags`
- Each list may be empty.

### Entity item contract
Each entity item should use this shape:

```json
{
  "value": "madre",
  "normalized": "madre",
  "certainty": "explicit",
  "evidence": [
    {
      "text": "mi madre estaba conmigo",
      "source": "cleaned",
      "segmentIds": [12]
    }
  ]
}
```

Required fields:
- `value`
- `normalized`
- `certainty`
- `evidence`

Allowed `certainty` values:
- `explicit`
- `inferred_light`
- `unclear`

Rules:
- `value` is the display form.
- `normalized` is the canonical deduplication form.
- `evidence` must be non-empty.
- `inferred_light` should be used sparingly in v0.1.
- `people` and `places` should prefer `explicit` over inference.

### Evidence item contract
Each evidence item should use this shape:

```json
{
  "text": "mi madre estaba conmigo",
  "source": "cleaned",
  "segmentIds": [12]
}
```

Required fields:
- `text`
- `source`

Optional fields:
- `segmentIds`

Allowed `source` values:
- `transcript`
- `cleaned`

Notes:
- Char offsets are intentionally deferred for now.
- `segmentIds` are preferred when transcript segmentation exists.

### `timeline`
- Optional in v0.1.
- If present, each item should include `order`, `text`, `certainty`, `evidence`.
- Timeline exists to preserve rough event order without rewriting the whole dream.

### `uncertainties`
- Required array, may be empty.
- Records ambiguity, inaudible fragments or extraction doubts.

Recommended item shape:

```json
{
  "type": "inaudible",
  "text": "palabra no determinada",
  "evidence": [
    {
      "text": "[inaudible]",
      "source": "transcript",
      "segmentIds": [7]
    }
  ]
}
```

Recommended `type` values:
- `inaudible`
- `ambiguous_entity`
- `low_confidence`
- `conflicting_signal`
- `other`

### `coverage`
- Summarizes how much of the upstream text could be turned into structured output.
- Required fields:
  - `evidencePolicy`
  - `unknownCount`

Allowed `evidencePolicy` values:
- `text_only`

### Validation rules
- `schemaVersion` must be `1` in v0.1.
- `dreamId` must match `manifest.json.dreamId`.
- `locale` must be `es` in v0.1.
- `provenance.inputHash` should match `manifest.stages.analyze.inputHash`.
- The file hash of `analysis.json` should match `manifest.stages.analyze.outputHash`.
- `titleCandidate`, if present with non-null text, must include evidence.
- Empty strings are invalid.
- Duplicate entity items within the same list should be collapsed by `normalized`.
- Entity arrays should be deterministically ordered by first evidence position, then `normalized`.
- Every entity item must include at least one evidence item.
- If evidence is missing, the item should be rejected or moved to `uncertainties`.

### Relationship with other artifacts
- `manifest.json` should point to the latest `analysis.json` path.
- `analysis.json` is the source for note frontmatter fields like `tags`, `people`, `places`, `emotions` and `motifs`.
- `analysis.json.titleCandidate` is the preferred source for the note subtitle segment.
- `note.md` may be regenerated from `manifest.json` plus upstream artifacts, subject to note overwrite-protection rules.
- `analysis.json` should not try to become a full execution log.

## `run.json`

### Role
- Summarizes one CLI execution.
- Records what was attempted, what succeeded, what failed and what artifacts were touched.
- Supports batch processing, retries, timing inspection and troubleshooting.

### Location
- Default location: `<runsDir>/<runId>/run.json`.

### Ownership
- Written only by Sonolog.
- Not intended for manual editing.
- Should be append-only at the execution level: one file per execution.

### High-level rules
- One `run.json` per CLI execution.
- Must not embed large raw payloads.
- Must not duplicate full manifest or analysis content.
- Must snapshot effective config without secrets.
- Must remain useful even when the execution fails partway through.
- May point to successful partial artifacts when later stages fail.

### Schema
```json
{
  "schemaVersion": 1,
  "runId": "2026-03-14T09-15-02Z",
  "startedAt": "2026-03-14T09:15:02Z",
  "finishedAt": "2026-03-14T09:18:11Z",
  "status": "completed",
  "pipelineVersion": "0.1",
  "locale": "es",
  "command": "sonolog process /home/pablo/Documents/sonolog/input",
  "requestedStages": [
    "transcribe",
    "clean",
    "analyze",
    "buildNote"
  ],
  "effectiveConfig": {
    "configPath": "/home/pablo/Documents/sonolog/config/config.json",
    "configFingerprint": "sha256:...",
    "appHomeDir": "/home/pablo/Documents/sonolog",
    "inputDir": "/home/pablo/Documents/sonolog/input",
    "archiveDir": "/home/pablo/Documents/sonolog/archive",
    "notesDir": "/home/pablo/Documents/sonolog/notes",
    "workspaceDir": "/home/pablo/Documents/sonolog/workspace",
    "promptSet": "es-v1",
    "openai": {
      "transcriptionModel": "whisper-1",
      "cleanupModel": "gpt-4.1-mini",
      "analysisModel": "gpt-4.1-mini"
    },
    "execution": {
      "continueOnError": true,
      "maxParallelFiles": 2
    }
  },
  "items": [
    {
      "detectedInputPath": "/home/pablo/Documents/sonolog/input/A-20250108-071440.wav",
      "archivePath": "/home/pablo/Documents/sonolog/archive/A-20250108-071440.wav",
      "recordedAt": "2025-01-08T07:14:40",
      "dreamId": "2026-03-14_001",
      "manifestPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/manifest.json",
      "notePath": "/home/pablo/Documents/sonolog/notes/2026-03-14_001.md",
      "result": "completed",
      "stages": {
        "transcribe": {
          "status": "completed",
          "attemptCount": 1,
          "durationMs": 67000
        },
        "clean": {
          "status": "completed",
          "attemptCount": 1,
          "durationMs": 50000
        },
        "analyze": {
          "status": "completed",
          "attemptCount": 1,
          "durationMs": 40000
        },
        "buildNote": {
          "status": "completed",
          "attemptCount": 1,
          "durationMs": 31000
        }
      },
      "artifacts": {
        "analysisPath": "/home/pablo/Documents/sonolog/workspace/2026-03-14_001/analysis/analysis.json",
        "analysisHash": "sha256:...",
        "noteHash": "sha256:..."
      },
      "warnings": [],
      "error": null
    }
  ],
  "totals": {
    "itemCount": 1,
    "completed": 1,
    "failed": 0,
    "skipped": 0
  },
  "warnings": [],
  "errors": []
}
```

### Required top-level fields
- `schemaVersion`
- `runId`
- `startedAt`
- `finishedAt`
- `status`
- `pipelineVersion`
- `locale`
- `command`
- `requestedStages`
- `effectiveConfig`
- `items`
- `totals`
- `warnings`
- `errors`

### `requestedStages`
- Ordered list of pipeline stages the execution intended to run.
- Allowed values:
  - `import`
  - `transcribe`
  - `clean`
  - `analyze`
  - `buildNote`

### `effectiveConfig`
- Snapshot of the effective configuration used by this execution.
- Must omit secrets.
- Should be sufficient to explain why a stage was considered fresh or stale.

### `items`
- One item per processed input.
- `detectedInputPath` should be present when the item came from `inputDir`.
- `archivePath` should be present once import succeeded.
- `recordedAt` should be present when it could be derived from the recorder filename.
- `dreamId` may be omitted only if the execution failed before Sonolog could assign or resolve it.
- `result` describes the item outcome.

Allowed `result` values:
- `completed`
- `failed`
- `skipped`
- `partial`

### Item stage snapshots
- Each stage snapshot should be lightweight and execution-specific.
- Recommended fields:
  - `status`
  - `attemptCount`
  - `durationMs`
- Optional fields:
  - `startedAt`
  - `finishedAt`
  - `error`
  - `warnings`

Allowed stage `status` values:
- `completed`
- `failed`
- `skipped`
- `stale`

### `artifacts`
- Lightweight references to important outputs touched during the execution.
- Recommended keys:
  - `analysisPath`
  - `analysisHash`
  - `noteHash`

### `totals`
- Required fields:
  - `itemCount`
  - `completed`
  - `failed`
  - `skipped`

### Top-level `status`
Allowed values:
- `completed`
- `completed_with_errors`
- `failed`
- `partial`

### Error and warning items
Recommended shape:

```json
{
  "code": "openai_rate_limit",
  "message": "Rate limit reached",
  "retryable": true,
  "itemRef": "2026-03-14_001"
}
```

Rules:
- Top-level `errors` hold execution-level or aggregated errors.
- Item-level `error` holds the final item error when relevant.
- Warnings should be lightweight and human-readable.
- `note_overwrite_protected` is a recommended error code when `buildNote` refuses to overwrite a protected note.

### Validation rules
- `schemaVersion` must be `1` in v0.1.
- `runId` should be unique per execution.
- `locale` must be `es` in v0.1.
- `items[*].dreamId` values must be unique when present.
- `totals.itemCount` must equal `items.length`.
- `totals.completed + totals.failed + totals.skipped` must not exceed `itemCount`.
- `effectiveConfig` must never contain the OpenAI API key.
- `command` should preserve the invoked CLI command in a human-readable form.

### Relationship with other artifacts
- `manifest.json.lastRunId` should point to the most recent run that touched the item.
- `run.json` should not be the source of truth for current item state; that remains `manifest.json`.
- `run.json` may reference `analysis.json` and `note.md` hashes for auditability.
- If notes are regenerated later, a new `run.json` should be produced.

## Deferred details
- Controlled vocabularies for emotions, motifs and tags are intentionally deferred.
- Character offsets inside evidence snippets are intentionally deferred.
- Full attempt history arrays are intentionally deferred.
- Cost/accounting fields may be added later if needed.

## Implementation notes for Java
- Recommended POJOs: `AnalysisDocument`, `AnalysisSource`, `AnalysisProvenance`, `ReviewState`, `EntityGroup`, `EntityItem`, `EvidenceItem`, `TimelineItem`, `UncertaintyItem`, `CoverageInfo`, `RunDocument`, `RunItem`, `RunStageSnapshot`, `RunTotals`, `RunWarning`, `RunError`.
- Use Jackson with deterministic serialization order.
- Prefer strict deserialization for both files.

## Status
- This spec refines `specs/001-sonolog-foundation/spec.md`.
- This spec complements `specs/002-config-manifest/spec.md`.
- If there is a conflict specific to `analysis.json` or `run.json`, this file is authoritative.
