# Sonolog Prompt and Stage Contract Spec

## Purpose
This document is a self-contained contract for Sonolog stage boundaries, prompt behavior and structured stage outputs.

Sonolog is a personal CLI tool for Debian Linux that:
- reads existing dream audio files from a folder,
- archives the original `.wav` audio,
- transcribes it with OpenAI,
- cleans and structures the text with an OpenAI LLM,
- extracts tags, people, places, emotions and motifs,
- writes one Markdown note per audio for later review in Obsidian,
- preserves intermediate artifacts for traceability and resume.

This document is intended to be usable by future sessions or other models without extra conversation context.

## Scope
- Defines stage boundaries for `import`, `transcribe`, `clean`, `analyze` and `buildNote`.
- Defines prompt strategy for LLM-backed stages.
- Defines structured output contracts for `cleaned.json` and stage-level expectations for `analysis.json`.
- Clarifies title generation inputs.

## Core rules
- Stage boundaries must stay strict.
- `import`, `transcribe` and `buildNote` must not use an LLM in v0.1.
- `clean` and `analyze` must return structured JSON only.
- Raw model responses should be stored before normalization when possible.
- Prompts must be versioned by `promptSet`, `promptId` and `promptHash`.
- No stage may invent dream content.
- If a downstream stage fails, successful upstream artifacts must be preserved.
- Validation happens before a stage output becomes canonical.

## Stage summary
- `import`: discover audio, compute hash, parse `recordedAt`, assign frozen daily ordinal, move source audio to `archiveDir`.
- `transcribe`: decode archived `.wav` audio to raw transcript artifacts.
- `clean`: improve readability conservatively and preserve uncertainty.
- `analyze`: extract structured metadata with evidence and propose a conservative title candidate.
- `buildNote`: render final Markdown deterministically from existing artifacts.

## Shared stage identity
Every stage should be able to resolve at least:
- `dreamId`
- `sourceHash`
- `recordedAt`
- `locale`
- `pipelineVersion`
- `promptSet` when applicable

## `import`

### Role
- Register one new audio item.
- Parse recorder metadata from filename.
- Assign stable naming metadata for later note generation.

### Input
- One `.wav` file in `inputDir`.
- Expected filename pattern in v0.1: `A-YYYYMMDD-HHMMSS.wav`.

### Output
- Archived original audio in `archiveDir`.
- `manifest.json` with source metadata.
- Frozen daily ordinal metadata.

### Import rules
- Parse `recordedAt` from filename when the pattern matches.
- Assign `dreamDayIndex` as the next ordinal for that local recording date.
- Render `dreamDayOrdinal` as uppercase Roman numeral.
- Freeze `dreamDayIndex` and `dreamDayOrdinal` after first assignment.
- Never renumber older dreams automatically if a newly imported item belongs to an earlier time on the same day.

## `transcribe`

### Role
- Produce canonical raw transcript artifacts from archived audio.

### Input
- Archived source audio from `manifest.json.source.archivedPath`.
- Effective locale `es`.

### Provider call
- Provider: OpenAI speech-to-text.
- Recommended parameters in v0.1:
  - `language=es`
  - `response_format=verbose_json`
- No freeform prompt text in v0.1.

### Output artifacts
- `stt/raw-response.json`
- `stt/transcript.txt`
- `stt/segments.json`

### Success rules
- The audio is decoded into transcript artifacts.
- Low-quality or fragmentary output may still count as success if artifacts are structurally valid.

### Failure surfaces
- unreadable_audio
- unsupported_audio_format
- openai_request_failed
- openai_timeout
- empty_transcript
- malformed_transcript_payload

## `clean`

### Role
- Improve readability conservatively.
- Remove low-value disfluencies and punctuation noise without changing meaning.
- Preserve uncertainty explicitly.

### Input
- `stt/transcript.txt`
- Optional segment data from `stt/segments.json` for traceability only

### Output artifact
- `llm/cleaned.json`

### Contract for `cleaned.json`
```json
{
  "schemaVersion": 1,
  "dreamId": "2026-03-14_001",
  "createdAt": "2026-03-14T09:17:00Z",
  "sourceHash": "sha256:...",
  "transcriptHash": "sha256:...",
  "promptId": "cleanup-es-v1",
  "promptHash": "sha256:...",
  "cleanText": "texto limpio y legible",
  "paragraphs": [
    "texto limpio y legible"
  ],
  "changes": [
    {
      "type": "disfluency_removal",
      "description": "Se eliminaron muletillas repetitivas"
    }
  ],
  "uncertainSpans": [
    {
      "text": "[inaudible]",
      "reason": "inaudible"
    }
  ]
}
```

### Prompt id
- `cleanup-es-v1`

### Prompt text
```text
Eres un editor conservador de transcripciones de suenos en espanol.

Objetivo:
Mejorar legibilidad y estructura del texto sin cambiar hechos, significado ni ambiguedades relevantes.

Reglas:
- No anadas detalles, nombres, lugares, emociones, causas ni contexto no dicho.
- No resumas.
- No interpretes.
- No transformes el sueno en analisis.
- Conserva dudas, contradicciones, repeticiones relevantes e inaudibles cuando afecten al contenido.
- Puedes eliminar muletillas o ruido verbal solo si no cambian significado.
- Mantener idioma espanol.
- Devuelve solo JSON valido segun el esquema dado.
```

### Validation rules
- Output must be valid JSON.
- `cleanText` must be non-empty when transcript input is non-empty.
- `cleanText` must not introduce unsupported entities or facts.
- Unknown keys should fail validation.
- If the output is clearly more interpretive than editorial, the stage should fail with a validation error.

### Failure surfaces
- malformed_clean_output
- schema_validation_failed
- suspected_hallucination
- openai_request_failed
- openai_timeout

## `analyze`

### Role
- Extract structured metadata from cleaned text.
- Produce the canonical semantic artifact used by note rendering.
- Propose a conservative `titleCandidate`.

### Input
- `llm/cleaned.json`
- Manifest metadata for traceability and naming only

### Output artifact
- `analysis/analysis.json`

### Additions required for `analysis.json`
- `titleCandidate` should be supported as an optional top-level object.
- If present, it must include literal evidence.
- If no safe title exists, omit it or set `text` to `null` depending on implementation choice, but do not guess.

### `titleCandidate` contract
```json
{
  "text": "Doctor excentrico",
  "certainty": "explicit",
  "evidence": [
    {
      "text": "habia un doctor rarisimo y excentrico",
      "source": "cleaned",
      "segmentIds": [5]
    }
  ]
}
```

### Title candidate rules
- Prefer short titles, usually 2 to 8 words.
- Prefer literal people, places, objects or situations explicitly present.
- Do not invent symbolism, themes or meanings.
- Do not use vague titles if a concrete one exists.
- If no trustworthy title is available, use no candidate and let `buildNote` fall back to `Sin titulo`.

### Prompt id
- `analysis-es-v1`

### Prompt text
```text
Eres un extractor de informacion, no un narrador creativo.

Objetivo:
Extraer datos estructurados de un sueno a partir del texto dado.

Reglas:
- Usa solo informacion explicitamente presente.
- No completes huecos.
- No deduzcas identidades, causas, simbolismos ni interpretaciones psicologicas.
- Si algo no se sabe, usa listas vacias o marcas de incertidumbre.
- Cada elemento extraido debe incluir evidencia textual literal.
- Si propones un titleCandidate, debe ser corto, fiel y apoyado por evidencia literal.
- Devuelve solo JSON valido segun el esquema dado.
```

### Validation rules
- Output must be valid JSON.
- Every extracted entity must include at least one evidence item.
- `titleCandidate`, if present with non-null text, must include evidence.
- If an extracted item lacks evidence, reject it or move it to uncertainties.
- Unknown keys should fail validation.

### Failure surfaces
- malformed_analysis_output
- schema_validation_failed
- unsupported_evidence
- suspected_hallucination
- openai_request_failed
- openai_timeout

## `buildNote`

### Role
- Render `note.md` deterministically.
- Never call an LLM in v0.1.

### Input
- `manifest.json`
- `stt/transcript.txt`
- `llm/cleaned.json`
- `analysis/analysis.json`

### Title assembly
- The user-facing title formula is:
  - `Sue\u00f1o YYYY_MM_DD(<ordinal romano>) - <titulo>`
- For ASCII consistency, repo examples may show `Sueno` instead of the literal `Sue\u00f1o` prefix.
- Date comes from `recordedAt`.
- Ordinal comes from frozen manifest naming metadata.
- Title text comes from `analysis.titleCandidate.text` when safe.
- If no title candidate exists, use `Sin titulo`.

### Example final title
- ASCII example: `Sueno 2025_03_26(I) - Doctor excentrico`

## Partial artifact policy
- If `transcribe` succeeds and `clean` fails, transcript artifacts remain canonical.
- If `clean` succeeds and `analyze` fails, cleaned artifacts remain canonical.
- If `analyze` succeeds and `buildNote` fails, analysis artifacts remain canonical.
- A later rerun should resume from the earliest failed or stale stage.

## Relationship with other specs
- `specs/002-config-manifest/spec.md` defines canonical machine state.
- `specs/003-analysis-run/spec.md` defines `analysis.json` and `run.json`.
- `specs/004-note-frontmatter/spec.md` defines final note rendering and overwrite protection.

## Status
- This spec refines `specs/001-sonolog-foundation/spec.md`.
- If there is a conflict specific to stage prompts or stage I/O contracts, this file is authoritative.
