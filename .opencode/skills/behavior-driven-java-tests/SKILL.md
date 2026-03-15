---
name: behavior-driven-java-tests
description: Create tests in this repo using a feature-step-example approach with JUnit 5 and Mockito only.
---

## Use this skill when
- You need to create, extend, or refactor tests in this repository.
- You need to turn requirements, manual checklists, or user flows into automated tests.
- You need another model/session to follow the same testing style already agreed in this repo.

## Hard rules
- Do not modify `src/main/java/**` unless the user explicitly lifts that veto.
- Use only JUnit 5 and Mockito as external testing tools/libraries.
- `./gradlew test` must be enough to run the suite.
- Prefer behavior/feature-driven tests over class-by-class unit coverage.
- Keep network-dependent behaviors offline by default.

## Core model
- Start from the specification or user-provided notes.
- Derive `requirement -> feature -> examples`.
- A feature is an action required to satisfy a requirement fully or partially.
- Examples are input/output combinations that help define the flow.
- An example is not the whole flow by itself; it only constrains part of the flow.

## Test structure
- Create exactly one top-level test class per feature.
- The class name must be the feature transcription and end with `Test`.
- Inside the class, model the flow step by step.
- Each step can be represented by `@Test` or `@Nested`.
- A `@Nested` name describes the flow step/action.
- A `@Test` method name uses Given-When-Then style.
- If a `@Nested` grows too much, extract it to a new class ending with `StepTest`.

## Naming rules
- Top-level class: feature/action name, e.g. `DisplayApplicationHelpMessageTest`.
- Nested class: step/action name, e.g. `ParseCommandLineArguments`.
- Test method: `givenXWhenYThenZ`.
- Write names to explain behavior, not implementation internals.

## Choosing test level
- Prefer tests that describe user-visible behavior first.
- Use narrower tests only when they support a flow step clearly.
- For behaviors controlled by `Main` and `System.exit`, it is acceptable to launch the app in a child Java process from JUnit using JDK `ProcessBuilder`.
- That still counts as JUnit-only execution because the suite runs with `./gradlew test` and adds no extra libraries.
- Use Mockito only when mocking collaborators is necessary for the behavior being exercised.

## Repo-specific guidance
- Treat `src/test/java/eu/nevian/speech_to_text_simple_java_client/DisplayApplicationHelpMessageTest.java` as the current reference example.
- Use `docs/step1-cli-test-checklist.md` and `docs/step2-file-preprocessing-test-checklist.md` as seed material for features and examples.
- Keep API-related tests offline.
- If a behavior needs filesystem isolation, use `@TempDir`.
- If a behavior needs full application execution, capture `stdout`, `stderr`, and exit code.

## Suggested workflow
1. Read the relevant requirement/checklist/user story.
2. Write down the feature name.
3. Enumerate the examples that constrain the feature.
4. Derive the flow steps.
5. Create the top-level feature test class.
6. Add `@Nested` blocks for major steps.
7. Add Given-When-Then tests for the examples inside the right step.
8. Run targeted tests.
9. Run `./gradlew test`.

## What to avoid
- Do not organize tests primarily by production class names.
- Do not create one top-level class per example.
- Do not add extra test frameworks.
- Do not touch production Java code just to make tests easier unless the user explicitly allows it.
- Do not default to online/integration API tests.

## Minimal template
```java
class SomeFeatureTest {

    @Nested
    class SomeStep {
        @Test
        void givenSomethingWhenActionThenExpectedOutcome() {
        }
    }
}
```
