# Notes p2p

`README.md` is the front door: what the project is, modules, run and test commands. Read it first.

## Working agreement

Work as a good engineering team: the aim is maintainable, well-designed software, not output that only
looks done.

- **Research first.** Before building a mechanic (gestures, drag and drop, pickers, insets, text
  editing, sync plumbing), search for an official API, then a maintained library. Before a design,
  check prior art and platform docs. Compare candidates in a short table; build our own only when none
  fits, and state why.
- **Find and use the tools.** At the start of each milestone and ticket, list the installed skills that
  apply, load them for writing and for review, and cite their rules. Also look for skills and tools not
  installed yet (e.g. the Android CLI skills) and propose them, with the reason.
- **Design before code.** Each ticket states its domain model, layer boundaries and invariants under the
  Architecture rules below. Open design questions go to the user before code.
- **Milestones in tickets.** A phase is a milestone of tickets. A ticket is one user-visible change, about
  300–600 lines, one commit, with its dependencies and whether it can run in parallel. It is reviewed,
  tested and checked by the user before a ticket that depends on it starts.
- **Done** = it builds; tests run on the real implementation (UI or screenshot tests once they exist);
  it runs on the emulator, never the user's phone, and its screens match the design in light and dark;
  it is reviewed with the matching skills.
- **Retro after each milestone.** What broke, why, what changes in the process; this agreement is updated
  from it.

## Board

Work is tracked on a local [Backlog.md](https://github.com/MrLesk/Backlog.md) board: one Markdown file
per ticket in `docs/plans/backlog/` (gitignored, shared by all worktrees), config in
`backlog.config.yml`. View it with `backlog board` or `backlog browser`.

- Flow: idea → PRD (`write-prd`) → tech spec (`write-spec`) → tickets (`to-tickets`) → work
  (`work-ticket`) → retro (`milestone-retro`). A milestone groups the tickets of one PRD.
- Statuses. A ticket is in exactly one; only the named role moves it there.

  | Status      | Means                                                                                                                                                                                                                             | Moved there by |
  |-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
  | To Do       | The ticket exists. Not approved: design or scope may still change.                                                                                                                                                                | Anyone         |
  | Ready       | Approved: goal, design and acceptance criteria are settled; blockers are listed.                                                                                                                                                  | The user       |
  | In Progress | One agent claimed it and works on it. Work has started, even if only partly built.                                                                                                                                                | The agent      |
  | Review      | The agent holds the work complete: DoD items 1–6 are checked with evidence, or each unchecked one is named in the notes with the reason. The uncommitted diff waits for the user.                                                 | The agent      |
  | Done        | The user reviewed it, it is committed, and the SHA is in the final summary. **Not committed = not Done.** A ticket with no repo change is Done when its output is published (link in the final summary) and the user accepted it. | The user       |

- The commit is part of every ticket, never a ticket of its own. An agent commits only after the user's
  explicit "commit" for that ticket. A phase is a parent ticket; its commits are subtasks.
- Agents pick only Ready tickets whose blockers are Done. A finding in review goes back to In Progress.
- Assignee: the session name, e.g. `@egm`. The user is `@rahul`.
- Change the board only through the `backlog` CLI, never by editing its files. `--plain` output only.
- A worktree needs a symlink to the main checkout's `backlog.config.yml` (excluded in
  `.git/info/exclude`), like `docs/plans`.

## Build and test

- Build, test and lint only through `gq` with the tasks from `README.md`, e.g. `gq spotlessCheck`,
  `gq :shared:jvmTest`. Never raw `./gradlew`.
- Static analysis: `gq detektAll`. A finding is fixed, or suppressed with `@Suppress("RuleId")` and a
  comment with the reason. `gq detektBaselineAll` only on request.

## Gradle

- AGP 9 rule: `androidApp` uses `com.android.application` only, never the KMP plugin. Shared code goes
  in `shared`, a KMP library (`com.android.kotlin.multiplatform.library`).
- Every Kotlin module applies `id("notesp2p.detekt")` (convention plugin in `build-logic/`).
- Dependencies use `implementation`, never `api`: `api` leaks the dependency to every consumer and
  slows the build. A consumer that needs a library declares it itself.
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

- Fakes, not mocks. This wins over skills that mock at system boundaries (`tdd`): at a boundary
  (clock, files, network), write a fake.
- Test fixtures with default parameters for models (`createNote(title = "Test")`), so a model change
  touches one place.
- One assertion per object: compare the whole expected state, not field by field.
- Turbine for hot flows.
- Tests use the generated `kotlin.test` setup for now.

## Docs

- `README.md`: what the project is, modules, how to run. Front door + index only.
- `docs/*.md`: one topic each, ~60 lines, the concept + concrete examples. Technical, impersonal voice.
- Doc file names are PascalCase: `docs/NoteModel.md`, `docs/TrashAndPurge.md`, never kebab-case.
- `docs/plans/` (gitignored, local only): `backlog/` holds the board, and its `docs/` holds every PRD,
  spec, research note, review and retro as a Backlog.md doc (`backlog doc list --plain`). `done/` is
  the archive of plans from before the board. No new plan files outside the board. Never link or cite
  a plan or ticket from tracked docs, code or comments; comments state the reason themselves.
- The project is built in the open: tracked docs describe what exists, not what is planned.
- Domain language (`domain-modeling` skill): `docs/Glossary.md`, never a root `GLOSSARY.md`. Terms only,
  each linked to the topic doc that holds its structure; topic docs do not redefine a term. Type rules
  (count the states, sealed vs enum, typed IDs) stay in research doc-16 and the spec's "Domain model".
- Decisions (the skill's ADRs): Backlog.md decisions, never `docs/adr/`. Create with
  `backlog decision create "<title>" -s accepted --plain`, then fill its Context, Decision and
  Consequences sections in the file (the CLI sets only title and status). Record one only when the
  decision is hard to reverse, surprising without context, and a real trade-off.

## Git

- Commit only on an explicit request (the `commit` skill). Never push.
