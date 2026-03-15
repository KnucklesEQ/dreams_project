---
name: spec-organization
description: Organize stable docs and change-focused specs using a consistent specs/<nnn-slug>/ layout.
---

## Use this skill when
- You are about to create a new specification in this repository.
- You need to decide whether a document belongs in `docs/` or `specs/`.
- You want another model or future session to follow the same spec layout and file naming rules.
- You are setting up a spec-driven workflow for new work.

## Hard rules
- Use `docs/` only for stable project documentation such as architecture notes, conventions, onboarding, and user-facing docs.
- Use `specs/` only for change-focused work such as features, refactors, initiatives, contracts, and implementation-scoped design.
- Create one folder per spec using `specs/<nnn-slug>/`.
- `spec.md` is mandatory for every spec folder.
- `plan.md` is optional and should exist only when an implementation sequence adds value.
- `tasks.md` is optional and should exist only when a real checklist adds value.
- Spec folder names must use a three-digit numeric prefix and kebab-case slug, such as `001-user-auth`.
- Do not put new change-focused specs directly under `docs/`.
- Do not create empty `plan.md` or `tasks.md` files just to satisfy a pattern.

## Core model
- `docs/` answers: stable knowledge the project should keep over time.
- `specs/` answers: a concrete change or scoped body of work the project is defining.
- `spec.md` captures the contract, decisions, scope, and acceptance criteria.
- `plan.md` captures a phased or ordered implementation approach.
- `tasks.md` captures actionable work items, not background explanation.

## Default layout
```text
docs/
  architecture.md
  conventions.md
  user-guide.md

specs/
  001-some-feature/
    spec.md
  002-another-change/
    spec.md
    plan.md
    tasks.md
```

## How to choose between `docs/` and `specs/`
- Put a file in `docs/` when it should remain true and useful beyond one specific change.
- Put a file in `specs/` when it exists to define, constrain, or guide one specific change or initiative.
- If a document mainly explains how to implement one upcoming change, it belongs in `specs/`.
- If a document mainly explains how the project works in general, it belongs in `docs/`.

## Spec creation workflow
1. Decide whether the new work is stable documentation or a change-focused specification.
2. If it is a spec, inspect existing `specs/` folders and choose the next available three-digit number.
3. Create `specs/<nnn-slug>/spec.md`.
4. Add `plan.md` only if sequencing, phases, or dependencies matter.
5. Add `tasks.md` only if a real checklist will help execution.
6. Update any relevant index or `README.md` references if the repository keeps one.

## Naming rules
- Use lowercase kebab-case for spec folder slugs.
- Keep slugs short and descriptive.
- Good: `001-user-auth`, `002-cli-overhaul`, `003-api-contracts`
- Avoid: `feature1`, `newStuff`, `Spec For Login`

## `spec.md` template
```md
# <Spec Title>

## Purpose
- Explain what this spec is for.

## Scope
- Define what is in and out.

## Requirements
- Capture the core rules or expected behavior.

## Decisions
- Record important design choices.

## Interfaces or Data
- Describe contracts, file formats, APIs, schemas, or interactions when relevant.

## Acceptance Criteria
- Define what must be true for the spec to be satisfied.

## Status
- Record whether the spec is draft, approved, implemented, or superseded.
```

## `plan.md` template
```md
# <Plan Title>

## Goal
- State the implementation goal.

## Phases
1. First phase.
2. Second phase.

## Risks or Dependencies
- Record sequencing constraints, risks, or blockers.

## Unresolved Questions
- List any open questions.
```

## `tasks.md` template
```md
# <Tasks Title>

- [ ] First actionable task
- [ ] Second actionable task
- [ ] Verification task
```

## What to avoid
- Do not store change-specific specs in `docs/`.
- Do not create one giant spec folder for unrelated work.
- Do not create `plan.md` when the work has no meaningful sequence.
- Do not create `tasks.md` as decoration.
- Do not skip numbering in folder names.
- Do not rely on ad hoc filenames when `spec.md` is the repository convention.
