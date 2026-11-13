# Notes p2p

A local-first notes project in Kotlin Multiplatform. Notes are [Automerge](https://automerge.org)
documents: a CRDT, a data type that two devices can change offline and then merge with no conflict.

What works today: the note and vault model, order keys and device identity (`core`), over the
Automerge API (`automerge-kmp`). The Android, iOS and desktop apps are empty scaffolds.

## Modules

| Module          | Role                                                                                   |
|-----------------|----------------------------------------------------------------------------------------|
| `shared`        | Compose Multiplatform module of the apps; holds the project template only              |
| `automerge-kmp` | Standalone Kotlin Multiplatform API over Automerge ([README](automerge-kmp/README.md)) |
| `core`          | Notes without UI: the note and vault model, order keys, device identity                |
| `cli`           | `notes` command-line tool (JVM)                                                        |
| `androidApp`    | Android entry point                                                                    |
| `desktopApp`    | Desktop (JVM) entry point                                                              |
| `iosApp`        | iOS entry point                                                                        |

## Running

Each app has a run configuration in Android Studio (`androidApp`, `desktopApp`, `iosApp`). iOS needs
Xcode installed, but not open.

From the command line:

- Android: `./gradlew :androidApp:installDebug` installs on a connected device or emulator
- Desktop: `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`)
- iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build`
- CLI: `./gradlew :cli:installDist`, then `cli/build/install/notes/bin/notes --version`

## Tests

- Android host tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`
- Automerge on every platform: `./gradlew :automerge-kmp:jvmTest :automerge-kmp:testAndroidHostTest
  :automerge-kmp:iosSimulatorArm64Test`
- `core`: the same three tasks, e.g. `./gradlew :core:jvmTest`
- Code style: `./gradlew spotlessCheck` (fix with `./gradlew spotlessApply`)
- Static analysis: `./gradlew detektAll`; `./gradlew check` includes it ([details](docs/StaticAnalysis.md))

## Docs

| Doc                                              | Topic                                               |
|--------------------------------------------------|-----------------------------------------------------|
| [docs/Glossary.md](docs/Glossary.md)             | The terms for notes, items, Trash and vaults        |
| [docs/Ordering.md](docs/Ordering.md)             | Order keys for notes and checklist items            |
| [docs/Identity.md](docs/Identity.md)             | Device ID and actor ID                              |
| [docs/StaticAnalysis.md](docs/StaticAnalysis.md) | Spotless and detekt: what each checks, suppressions |

## Tech stack

Kotlin Multiplatform, Compose Multiplatform, Material 3, Automerge.
