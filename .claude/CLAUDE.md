# Notes p2p

`README.md` is the front door: what the project is, modules, run and test commands. Read it first.

## Build and test

- Build, test and lint only through `gq` with the tasks from `README.md`, e.g. `gq spotlessCheck`,
  `gq :shared:jvmTest`. Never raw `./gradlew`.
- Static analysis: `gq detektAll`. A finding is fixed, or suppressed with `@Suppress("RuleId")` and a
  comment with the reason. `gq detektBaselineAll` only on request.

## Gradle

- AGP 9 rule: `androidApp` uses `com.android.application` only, never the KMP plugin. Shared code goes
  in `shared`, a KMP library (`com.android.kotlin.multiplatform.library`).
- Every Kotlin module applies `id("notesp2p.detekt")` (convention plugin in `build-logic/`).
- Versions live in `gradle/libs.versions.toml`. The Gradle daemon runs on JetBrains JDK 21
  (`gradle/gradle-daemon-jvm.properties`).

## Architecture

- Clean architecture. Dependencies point inward: `ui/` and `data/` depend on `domain/`; `domain/`
  imports nothing from `data/`, UI or framework libraries (no Automerge types in `domain/`).
- Repository **interfaces always live in `domain/`**, implementations in `data/`.
- **Domain entities** are required when a feature has considerable business logic (rules, invariants,
  derived state). They are optional only for simple screens, where the data model already fits.
- **Use cases** are added only when logic spans several repositories or is reused by several
  ViewModels. Otherwise the ViewModel calls the repository directly.
- MVVM + unidirectional data flow: state flows down, actions flow up. ViewModels expose a `StateFlow`
  of a sealed UI state (Loading / Success / Error).
- Features live in `shared/src/commonMain/.../feature/<name>/` with `ui/`, `data/`, `domain/`, `di/`.
- Local-first: the app works fully offline. Sync is a pluggable module behind an interface; the note
  storage never imports a network library.
- Components: app-wide in `ui/component/`, feature-shared in the feature package, single-use in the
  screen's file.
- Icons: vector drawables / SVG in Compose resources. Never `androidx.compose.material.icons`.
- `@OptIn` goes in the module's Gradle `compilerOptions`, never as an annotation.
- A composable that only reads resources or CompositionLocals, and calls no other composables, gets
  `@ReadOnlyComposable`.

## Testing

- Fakes, not mocks.
- Test fixtures with default parameters for models (`createNote(title = "Test")`), so a model change
  touches one place.
- One assertion per object: compare the whole expected state, not field by field.
- Turbine for hot flows.
- Tests use the generated `kotlin.test` setup for now.

## Docs

- `README.md`: what the project is, modules, how to run. Front door + index only.
- `docs/*.md`: one topic each, ~60 lines, the concept + concrete examples. Technical, impersonal voice.
- `docs/plans/`: local-only design and build plans (gitignored). Never link or cite a plan from
  tracked docs, code or comments; comments state the reason themselves.
- The project is built in the open: tracked docs describe what exists, not what is planned.

## Git

- Commit only on an explicit request (the `commit` skill). Never push.
