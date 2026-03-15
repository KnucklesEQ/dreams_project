# Sonolog Help and Version

## Purpose
- Define two basic informational CLI commands: `help` and `version`.
- Keep both commands safe, local, and available even when the rest of the application is not configured yet.

## Scope
- Add `sonolog help [command]`.
- Add `sonolog version`.
- Add top-level aliases `sonolog --help` and `sonolog --version`.
- Define expected output, error behavior, and safety rules.

## Requirements
- `help` and `version` must be side-effect free.
- `help` and `version` must not read or write project artifacts.
- `help` and `version` must not require a valid config file.
- `help` and `version` must not require network access.
- `help` and `version` must work even if `inputDir`, `archiveDir`, or `notesDir` do not exist.
- User-facing text must be in Spanish in v1.

## Command surface
- `sonolog help`
- `sonolog help <command>`
- `sonolog version`
- `sonolog --help`
- `sonolog --version`

## `help`

### Role
- Explain how to use the CLI.
- Be the first stop for a user who does not remember the available commands or flags.

### `sonolog help`
- Shows a concise command overview.
- Lists top-level commands.
- Shows the most important global flags.
- Includes a short examples section.

### `sonolog help <command>`
- Shows usage for one concrete command.
- Explains positional arguments.
- Explains command-specific flags.
- Includes short safety notes when relevant, for example for `--force`.

### `--help`
- `sonolog --help` is equivalent to `sonolog help`.

### Output rules
- Output should be human-readable by default.
- Output should remain concise.
- Command names and flags should be stable across runs.
- Help text should not depend on filesystem state, config state, or network state.

### Error rules
- `sonolog help <unknown-command>` should fail with a usage error.
- The error should clearly state that the command is unknown.
- The output should suggest `sonolog help` to discover valid commands.

## `version`

### Role
- Report the application version only.

### `sonolog version`
- Prints the application version in a stable, human-readable form.

### `--version`
- `sonolog --version` is equivalent to `sonolog version`.

### Output contract
- Output must include the application name and the application version.
- Recommended format:

```text
Sonolog <app-version>
```

### Hard exclusions
- Do not print pipeline version.
- Do not print commit hash.
- Do not print build timestamp.
- Do not print Java runtime details.
- Do not print environment or config paths.

## Arguments and validation
- `sonolog help` accepts zero or one positional command name.
- `sonolog version` accepts no positional arguments.
- Extra unexpected arguments should produce a usage error.

## Exit behavior
- Successful `help` returns exit code `0`.
- Successful `version` returns exit code `0`.
- Usage errors, including unknown commands passed to `help`, return exit code `2`.

## Examples

### General help
```bash
sonolog help
```

### Help for one command
```bash
sonolog help process
```

### Version
```bash
sonolog version
```

### Aliases
```bash
sonolog --help
sonolog --version
```

## Acceptance Criteria
- `sonolog help` lists the main top-level commands.
- `sonolog help process` explains `process` usage.
- `sonolog --help` behaves like `sonolog help`.
- `sonolog version` prints only the application name and application version.
- `sonolog --version` behaves like `sonolog version`.
- None of these commands create, modify, or delete files.
- None of these commands require OpenAI access.
- None of these commands require a valid project config.

## Out of Scope
- JSON output for `help` or `version`.
- Build metadata beyond the application version.
- Integration with remote services.
- Shell completion generation.

## Status
- Draft feature spec for addition after the current base specs are stabilized.
