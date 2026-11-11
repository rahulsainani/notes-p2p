# Notes p2p

A notes app for Android, iOS and desktop that keeps notes on your devices and syncs them peer to peer.

## First step

Sync plain notes between a phone, a laptop and a home server, and see what breaks when two devices
edit the same note offline.

## Modules

| Module          | Role                                                                                   |
|-----------------|----------------------------------------------------------------------------------------|
| `shared`        | Kotlin Multiplatform code shared by all apps: Compose UI, ViewModels, features         |
| `automerge-kmp` | Standalone Kotlin Multiplatform API over Automerge ([README](automerge-kmp/README.md)) |
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
- Code style: `./gradlew spotlessCheck` (fix with `./gradlew spotlessApply`)
- Static analysis: `./gradlew detektAll`; `./gradlew check` includes it ([details](docs/StaticAnalysis.md))

## Docs

| Doc                                              | Topic                                            |
|--------------------------------------------------|--------------------------------------------------|
| [docs/StaticAnalysis.md](docs/StaticAnalysis.md) | Spotless and detekt: what each checks, baselines |

## Tech stack

Kotlin Multiplatform, Compose Multiplatform, Material 3, Automerge.
