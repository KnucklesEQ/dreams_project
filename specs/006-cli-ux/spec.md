# Sonolog CLI UX Spec

## Purpose
This document is a self-contained contract for Sonolog CLI behavior in v0.1.

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
- Defines command semantics.
- Defines high-level CLI UX defaults.
- Defines common flags, stage-specific flags and exit codes.
- Defines protection behavior for edited notes.

## Core UX rules
- `sonolog process` is the main happy-path command.
- `sonolog process` must always run `scan` first in v0.1.
- `sonolog scan` must import and archive new audio; it is not just a listing command.
- The CLI must be idempotent by default.
- Successful upstream artifacts must be kept even when downstream stages fail.
- Notes edited outside Sonolog are protected from overwrite unless `--force` is used.
- Safe defaults beat convenience when the two conflict.

## Command overview
- `sonolog init`
- `sonolog doctor`
- `sonolog scan`
- `sonolog process [<dreamId|path>]`
- `sonolog transcribe <dreamId>`
- `sonolog clean <dreamId>`
- `sonolog analyze <dreamId>`
- `sonolog build-note <dreamId>`
- `sonolog status [<dreamId>]`

## Common flags
- `--config <path>`: use an explicit config file.
- `--json`: machine-readable output.
- `--verbose`: richer diagnostic output.
- `--dry-run`: show intent without mutating files or calling remote APIs.
- `--force`: bypass overwrite or freshness protections when the command allows it.

## `init`

### Role
- Bootstrap Sonolog in a chosen home directory.

### Behavior
- Create folder layout.
- Create `config/config.json` if missing.
- Write default config derived from `--home` when provided.
- Refuse to overwrite existing config unless `--force` is used.

### Flags
- `--home <dir>`
- `--force`

## `doctor`

### Role
- Validate environment and configuration.

### Behavior
- Validate config file readability and schema.
- Validate required folders and write permissions.
- Validate API key reference presence.
- Validate filename pattern handling.
- With `--online`, test remote reachability and model configuration.

### Flags
- `--online`

## `scan`

### Role
- Discover and import new audio from `inputDir`.

### Behavior
- Find eligible `.wav` files.
- Parse `recordedAt` from filenames matching `A-YYYYMMDD-HHMMSS.wav`.
- Compute source hash.
- Detect duplicates via hash.
- Assign `dreamId`.
- Assign and freeze daily ordinal metadata.
- Move imported source audio to `archiveDir`.
- Create or update `manifest.json`.
- Never call remote models.

### Idempotency rules
- A file already known by hash must not create a second logical dream item.
- Re-running `scan` with no new files should be a no-op.

### Flags
- `--input <dir>`
- `--archive <dir>`
- `--reindex`

### Notes
- `--reindex` is for manifest/index reconciliation only and must not renumber frozen ordinals.

## `process`

### Role
- Main end-to-end command for daily use.

### Behavior
- Always run `scan` first.
- Select eligible items.
- Run `transcribe -> clean -> analyze -> build-note`.
- Skip stages already fresh unless forced or stale.
- Preserve partial artifacts if a later stage fails.

### Selection rules
- No args: process all eligible items discovered via `scan`.
- `<dreamId>`: process exactly that known item.
- `<path>`: allow explicit single-path import and processing when supported by implementation.

### Flags
- `--from scan|transcribe|clean|analyze|build-note`
- `--to transcribe|clean|analyze|build-note`
- `--retry-failed`
- `--force`

### `--force` semantics
- May force re-execution of fresh stages.
- May allow overwriting a protected note during `build-note`.
- Must not silently destroy unrelated artifacts.

## `transcribe`

### Role
- Run only the transcription stage for one item.

### Behavior
- Read archived audio from manifest.
- Skip if fresh unless `--force`.
- Write canonical transcript artifacts.

### Flags
- `--model <name>`
- `--force`

## `clean`

### Role
- Run only the cleanup stage for one item.

### Behavior
- Read transcript artifacts.
- Skip if fresh unless `--force`.
- Validate structured JSON output before promoting it.

### Flags
- `--model <name>`
- `--force`

## `analyze`

### Role
- Run only the analysis stage for one item.

### Behavior
- Read cleaned artifact.
- Skip if fresh unless `--force`.
- Validate evidence-bearing structured output before promoting it.

### Flags
- `--model <name>`
- `--force`

## `build-note`

### Role
- Render or rerender the final Markdown note for one item.

### Behavior
- Read manifest plus upstream artifacts.
- Refuse overwrite if the current note is protected.
- With `--force`, overwrite the note and update the stored generated hash.

### Flags
- `--stdout`
- `--force`

### Protection rule
- A note is protected when its current on-disk hash differs from the last Sonolog-generated `noteHash` recorded in manifest state.

## `status`

### Role
- Report current state of one item or all known items.

### Behavior
- Default output should be concise and human-readable.
- `--json` should expose machine-readable status.
- Should make note protection, stale stages and failed stages obvious.

### Recommended filters
- `--failed`
- `--stale`
- `--protected`
- `--pending`
- `--partial`

## Status vocabulary
- `imported`
- `transcribed`
- `cleaned`
- `analyzed`
- `note_built`
- `failed`
- `stale`
- `partial`
- `protected_note`

## Recommended error codes
- `note_overwrite_protected`
- `duplicate_audio_hash`
- `recorded_at_parse_failed`
- `upstream_missing`
- `schema_validation_failed`
- `suspected_hallucination`
- `openai_request_failed`

## Exit codes
- `0`: success
- `1`: partial success or mixed batch result
- `2`: usage error or invalid config
- `3`: external service failure
- `4`: overwrite-protection or other safety block

## Example flows

### First-time setup
```bash
sonolog init --home ~/Documents/sonolog
sonolog doctor --online
```

### Daily happy path
```bash
# copy A-20250326-071440.wav to inputDir
sonolog process
```

### Inspect one item
```bash
sonolog status 2026-03-14_001
```

### Rebuild a protected note only if you really mean it
```bash
sonolog build-note 2026-03-14_001 --force
```

## Relationship with other specs
- `specs/005-prompt-stage-contracts/spec.md` defines prompt strategy and stage I/O contracts.
- `specs/002-config-manifest/spec.md` defines canonical machine state.
- `specs/004-note-frontmatter/spec.md` defines note overwrite protection.

## Status
- This spec refines `specs/001-sonolog-foundation/spec.md`.
- If there is a conflict specific to CLI semantics, flags or exit codes, this file is authoritative.
