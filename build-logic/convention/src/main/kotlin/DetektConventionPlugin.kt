import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * detekt for one module, with the shared config in `config/detekt/detekt.yml` and a baseline next to
 * the module's build file.
 *
 * `detektAll` runs every check once: type-resolved tasks where a JVM or Android compilation exists,
 * light source-set tasks for the rest (iOS). `detektBaselineAll` writes the baselines of the same tasks.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            parallel.set(true)
            config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))
        }

        // Generated sources (e.g. BindingVersions.kt in automerge-kmp) are not code to review.
        val buildDir = layout.buildDirectory.get().asFile
        tasks.withType<Detekt>().configureEach {
            exclude { it.file.startsWith(buildDir) }
            // A missing file means no known findings; the task fails on a baseline file that does not exist.
            val file = baselineFile(name.removePrefix("detekt"))
            baseline.set(provider { file.takeIf { it.asFile.exists() } })
            reports {
                checkstyle.required.set(false)
                markdown.required.set(false)
            }
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            exclude { it.file.startsWith(buildDir) }
            baseline.set(baselineFile(name.removePrefix("detektBaseline")))
        }

        val checkTasks = provider { analysisTaskNames().map { "detekt$it" } }
        val detektAll = tasks.register("detektAll") {
            group = "verification"
            description = "Runs detekt on all source sets of this module, with type resolution where possible."
            dependsOn(checkTasks)
        }
        tasks.register("detektBaselineAll") {
            group = "verification"
            description = "Writes the detekt baselines for the tasks of detektAll."
            dependsOn(provider { analysisTaskNames().map { "detektBaseline$it" } })
        }

        // The plain `detekt` task reads src/main and src/test only: empty in KMP modules, and a
        // duplicate of detektMain/detektTest elsewhere. detektAll replaces it in `check`.
        pluginManager.withPlugin("base") {
            tasks.named("check") {
                setDependsOn(dependsOn.filterNot { it.isTaskNamed("detekt") })
                dependsOn(detektAll)
            }
        }
    }

    /**
     * One file per task: the JVM and Android tasks of a KMP module would otherwise share
     * `detekt-baseline-main.xml` and overwrite each other's findings.
     */
    private fun Project.baselineFile(taskSuffix: String) =
        layout.projectDirectory.file("detekt-baselines/${taskSuffix.replaceFirstChar { it.lowercase() }}.xml")

    /** Task name suffixes shared by `detekt<Suffix>` and `detektBaseline<Suffix>`. */
    private fun Project.analysisTaskNames(): List<String> {
        if (!pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            // kotlin.jvm, and com.android.application where detektMain/detektTest span all variants.
            return listOf("Main", "Test")
        }
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        val typeResolvedTargets = kotlin.targets.filter {
            it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm
        }
        val compilations = typeResolvedTargets.flatMap { target ->
            target.compilations.map { it to target }
        }
        val typeResolved = compilations.map { (compilation, target) ->
            compilation.name.capitalized() + target.name.capitalized()
        }
        val covered = compilations.flatMap { (compilation, _) -> compilation.allKotlinSourceSets }
            .map { it.name }
            .toSet()
        val lightOnly = kotlin.sourceSets.map { it.name }
            .filterNot { it in covered }
            .map { it.capitalized() + "SourceSet" }
        return typeResolved + lightOnly
    }

    private fun Any.isTaskNamed(name: String): Boolean = when (this) {
        is TaskProvider<*> -> this.name == name
        is Task -> this.name == name
        is String -> this == name
        else -> false
    }

    private fun String.capitalized() = replaceFirstChar { it.uppercase() }
}
