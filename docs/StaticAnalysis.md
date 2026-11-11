# Static analysis

Two tools check the code. They do not overlap.

| Tool                              | Checks                                                                         | Command                   |
|-----------------------------------|--------------------------------------------------------------------------------|---------------------------|
| Spotless (ktlint + Compose rules) | Formatting and Compose API style                                               | `./gradlew spotlessCheck` |
| detekt                            | Code smells, complexity, potential bugs, coroutine misuse, project conventions | `./gradlew detektAll`     |

`./gradlew check` runs `detektAll` in every module.

## One convention plugin

Every Kotlin module applies `id("notesp2p.detekt")` from `build-logic/convention`. The plugin
applies `dev.detekt`, points it at the shared config `config/detekt/detekt.yml`, and adds two
tasks per module: `detektAll` and `detektBaselineAll`. A new module needs only this line in its
`plugins {}` block:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("notesp2p.detekt")
}
```

## Light and type-resolved analysis

detekt has two modes. Light analysis reads the source text only. Type-resolved analysis also gets
the compile classpath, so it knows types and nullability. Most coroutine and potential-bug rules
(`SuspendFunWithFlowReturnType`, `UnnecessarySafeCall`, `ForbiddenAnnotation`) run only with types.

`detektAll` picks the mode for each source set:

| Module type | Tasks |
|---|---|
| KMP | `detekt<Compilation><Target>` for JVM and Android compilations (e.g. `detektMainJvm`, `detektHostTestAndroid`); `detekt<SourceSet>SourceSet` for source sets no such compilation includes (`iosMain`, `appleMain`, …) |
| Kotlin JVM, Android app | `detektMain`, `detektTest` |

Type resolution is available only for JVM and Android, so iOS-only code gets light analysis.
`commonMain` code is part of both the JVM and the Android compilation, so a finding there shows
in both reports.

The plain `detekt` task is not part of `check`. It reads `src/main` and `src/test` only.

## Config

`config/detekt/detekt.yml` holds only overrides. detekt's defaults apply to every other rule
(`buildUponDefaultConfig`). The overrides cover:

- Compose naming and previews, e.g. `FunctionNaming` ignores `@Composable` functions.
- Inactive rules turned on, e.g. `SuspendFunSwallowedCancellation`.
- Project conventions: no `@OptIn` annotation (opt-ins go in `compilerOptions`), no
  `androidx.compose.material.icons`, and a second `ForbiddenImport` instance
  (`ForbiddenImport/domain`) that keeps `domain/` packages free of `data`, `ui`, Automerge and
  framework imports.

## Baselines

A baseline lists known findings. detekt does not report them again. Each task has its own
baseline file, `<module>/detekt-baselines/<task>.xml`, e.g. `automerge-kmp/detekt-baselines/mainJvm.xml`.
A module with no findings has no file. `./gradlew detektBaselineAll` rewrites the files of a
module from its current findings.

A new finding is fixed, or suppressed in code with the reason:

```kotlin
@Suppress("ReturnCount") // Each early return rejects one malformed part of the input.
fun parseKey(raw: String): Key?
```

Reports: `<module>/build/reports/detekt/<task>.html` and `.sarif`.
