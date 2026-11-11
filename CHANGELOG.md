# Changelog

All notable changes to this project are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Kotlin Multiplatform project with Android, iOS and desktop apps sharing one Compose UI module.
- Spotless with ktlint and the Compose rules (`./gradlew spotlessCheck`, `./gradlew spotlessApply`).
- Turbine and kotlinx-coroutines-test for shared tests.
- `automerge-kmp`: one Automerge document API for Android, desktop and iOS, over `automerge-java` and
  `automerge-swift`, with cross-platform fixture tests.
- `cli` module: `notes --version`.
- detekt in every module through the `notesp2p.detekt` convention plugin (`./gradlew detektAll`, part
  of `check`), with type resolution on JVM and Android code.
