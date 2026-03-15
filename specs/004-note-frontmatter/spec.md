# Sonolog Note Spec

## Purpose
This document is a self-contained contract for generated `note.md` files in Sonolog.

Sonolog is a personal CLI tool for Debian Linux that:
- reads existing dream audio files from a folder,
- transcribes them with OpenAI,
- cleans and structures the text with an OpenAI LLM,
- extracts tags, people, places, emotions and motifs,
- writes one Markdown note per audio for later review in Obsidian,
- preserves originals and intermediate artifacts.

This document is intended to be usable by future sessions or other models without extra conversation context.

## Scope
- Defines the runtime contract for generated Markdown notes.
- Defines the YAML frontmatter contract.
- Defines note regeneration and overwrite-protection rules.
- Clarifies how notes relate to `manifest.json` and `analysis.json`.

## Core rules
- One note per `dreamId`.
- The note is a human-facing derived artifact, not a source of truth.
- The note should be optimized for reading and later manual review in Obsidian.
- The note must preserve both processed and original material.
- Sonolog must not overwrite a note that was modified outside Sonolog unless `--force` is explicitly used.
- No absolute file paths should appear in the note body or frontmatter.
- Frontmatter should stay useful for Obsidian; deep technical metadata should live under `sonolog`.
- Output should be deterministic when upstream artifacts are unchanged.

## Terms
- `generated note hash`: hash of the last note version produced by Sonolog and stored in `manifest.json`.
- `external modification`: a note file whose current on-disk hash differs from the last generated note hash.
- `protected note`: an externally modified note that Sonolog must not overwrite automatically.

## Source of truth
- `workspace/<dreamId>/manifest.json`: current machine state for one audio.
- `workspace/<dreamId>/analysis/analysis.json`: canonical structured extraction.
- `workspace/<dreamId>/stt/transcript.txt`: canonical raw transcript text.
- `workspace/<dreamId>/llm/cleaned.json`: canonical cleaned/structured text.
- `notes/<dreamId>.md`: human-facing rendered note for Obsidian.

## Note location and naming
- Default location: `<notesDir>/<dreamId>.md`.
- One note file per audio.
- Filename templating is intentionally deferred.

## Generation policy
- If the note file does not exist, Sonolog may create it.
- If the note file exists and its current hash matches the last generated note hash stored in `manifest.json`, Sonolog may overwrite it during regeneration.
- If the note file exists and its current hash differs from the last generated note hash, Sonolog must treat it as externally modified and must not overwrite it automatically.
- If the note file exists but `manifest.json` has no prior generated `noteHash`, Sonolog should treat the note as protected by default.
- `--force` may bypass protection and overwrite the existing note.
- A forced overwrite must update the stored `noteHash` in `manifest.json`.

## Recommended behavior on protected notes
- The `buildNote` stage should stop before writing.
- The item should surface a clear error or warning such as `note_overwrite_protected`.
- The existing note file must remain untouched.
- The user should be able to rerun with `--force` if desired.

## Encoding and formatting rules
- Notes should be UTF-8 text files.
- Line endings should be `\n`.
- The file should end with a trailing newline.
- Frontmatter field order should be deterministic.
- Section order in the body should be deterministic.

## Frontmatter

### Role
- Exposes human-useful metadata to Obsidian.
- Keeps common extracted fields easy to query.
- Stores compact technical traceability under `sonolog`.

### Schema
```yaml
---
type: dream
dream_id: 2026-03-14_001
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
  source_hash: "sha256:..."
  transcript_hash: "sha256:..."
  cleaned_hash: "sha256:..."
  analysis_hash: "sha256:..."
  models:
    transcription: "whisper-1"
    cleanup: "gpt-4.1-mini"
    analysis: "gpt-4.1-mini"
---
```

### Required top-level fields
- `type`
- `dream_id`
- `title`
- `recorded_at`
- `created_at`
- `generated_at`
- `language`
- `needs_review`
- `tags`
- `people`
- `places`
- `emotions`
- `motifs`
- `source_audio_name`
- `sonolog`

### Top-level field semantics
- `type`: fixed value `dream` in v0.1.
- `dream_id`: must match `manifest.json.dreamId`.
- `title`: final rendered note title.
- Literal user-facing title format: `Sue\u00f1o YYYY_MM_DD(<ordinal romano>) - <titulo>`.
- For ASCII consistency, repo examples may show `Sueno` instead of the literal `Sue\u00f1o` prefix.
- `recorded_at`: recording timestamp, expected to come from the recorder filename in the normal v0.1 workflow; in v0.1 this is recorder-local time, not UTC.
- `created_at`: stable item creation time, typically from `manifest.createdAt`.
- `generated_at`: time this note version was rendered.
- `language`: effective note language; v0.1 supports only `es`.
- `needs_review`: copied from current machine state; should default to `true` in v0.1.
- `tags`, `people`, `places`, `emotions`, `motifs`: rendered from `analysis.json`.
- `source_audio_name`: basename only, never a full path.

### `sonolog` object
- Stores compact technical traceability.
- Required fields:
  - `note_schema_version`
  - `pipeline_version`
  - `run_id`
  - `dream_day_index`
  - `dream_day_ordinal`
  - `title_candidate`
  - `source_hash`
  - `transcript_hash`
  - `cleaned_hash`
  - `analysis_hash`
  - `models`

### `models` object
- Required fields:
  - `transcription`
  - `cleanup`
  - `analysis`

Notes:
- `title_candidate` may be `null` when no safe subtitle candidate exists.
- If `title_candidate` is `null`, the final title should use the fallback subtitle `Sin titulo`.

### Frontmatter rules
- Top-level arrays should contain only normalized display strings.
- Technical paths, prompt text, config values and request ids must not appear in frontmatter.
- Frontmatter should be regenerated deterministically from upstream artifacts.
- Frontmatter keys should appear in a fixed order.

## Markdown body

### Role
- Presents the dream in a reading-first format.
- Preserves the cleaned text, extracted structure, ambiguity and raw transcript.
- Keeps traceability visible without overwhelming the note.

### Required section order
1. `# <title>`
2. `## Version depurada`
3. `## Elementos extraidos`
4. `## Ambiguedades`
5. `## Transcripcion cruda`
6. `## Trazabilidad`

### Optional section
- `## Secuencia` may appear between `## Elementos extraidos` and `## Ambiguedades` when `analysis.json.timeline` is not empty.

### Reference structure
```md
# Sueno 2025_01_08(I) - Doctor excentrico

## Version depurada
...

## Elementos extraidos

### Personas
- madre

### Lugares
- casa

### Emociones
- miedo

### Motivos
- persecucion

### Tags
- familia

## Secuencia
1. Primero aparece en la casa de infancia.

## Ambiguedades
- [inaudible] en segmento 7
- Entidad ambigua: figura masculina no identificada

## Transcripcion cruda
...

## Trazabilidad
- Dream ID: 2026-03-14_001
- Run ID: 2026-03-14T09-15-02Z
- Grabado: 2025-01-08T07:14:40
- Fuente: A-20250108-071440.wav
- Hash fuente: sha256:...
- Modelos: whisper-1 / gpt-4.1-mini / gpt-4.1-mini
```

### Body rules
- `Version depurada` should be rendered from the cleaned output.
- `Transcripcion cruda` should always be present.
- `Elementos extraidos` should be rendered from `analysis.json.entities`.
- `Secuencia` should be rendered from `analysis.json.timeline` when available.
- `Ambiguedades` should be rendered from `analysis.json.uncertainties`.
- `Trazabilidad` should stay concise and should not include absolute paths.

### Empty-state rules
- If an extracted list is empty, render `- Ninguno`.
- If `timeline` is absent or empty, omit `## Secuencia`.
- If `uncertainties` is empty, render `- Ninguna detectada` under `## Ambiguedades`.
- If the cleaned text is missing, note generation should fail rather than emit partial fabricated content.

## Determinism rules
- Frontmatter key order should be fixed.
- Section order should be fixed.
- Extracted lists should follow the order already normalized in `analysis.json`.
- Re-rendering the same upstream artifacts should produce byte-identical note content.

## Validation rules
- `dream_id` must match `manifest.json.dreamId` and `analysis.json.dreamId`.
- `title` must match `manifest.json.naming.titleFinal`.
- `language` must be `es` in v0.1.
- `recorded_at` should match `manifest.json.source.recordedAt`.
- `sonolog.dream_day_index` should match `manifest.json.naming.dreamDayIndex`.
- `sonolog.dream_day_ordinal` should match `manifest.json.naming.dreamDayOrdinal`.
- `tags`, `people`, `places`, `emotions` and `motifs` must match the normalized values from `analysis.json`.
- `sonolog.analysis_hash` should match the current `analysis.json` file hash.
- `sonolog.transcript_hash` should match the current transcript hash.
- `sonolog.cleaned_hash` should match the current cleaned output hash.
- `sonolog.source_hash` should match `manifest.json.source.sha256`.

## Relationship with other artifacts
- `note.md` is generated from upstream artifacts and may be recreated if not protected.
- `manifest.json` should store the last generated `noteHash` and `notePath`.
- `analysis.json` is the source for extracted metadata shown in the note.
- `analysis.json.titleCandidate` feeds the subtitle segment of the final note title when available.
- Manual edits in Obsidian turn the note into a protected note until `--force` is used.
- Manual deletion of the archived source audio does not invalidate an existing note, but prevents retranscription from the original audio.

## Deferred details
- Custom title templates are deferred.
- Alternative note filenames are deferred.
- Obsidian-specific wiki links are deferred.
- Rich callouts or styling markers are deferred.

## Implementation notes for Java
- Recommended renderer input: `manifest.json` + `analysis.json` + transcript text + cleaned text.
- Use deterministic YAML serialization for frontmatter.
- Prefer a template-based renderer over ad hoc string concatenation.

## Status
- This spec refines `specs/001-sonolog-foundation/spec.md`.
- This spec should be read together with `specs/002-config-manifest/spec.md` and `specs/003-analysis-run/spec.md`.
- If there is a conflict specific to `note.md`, this file is authoritative.
