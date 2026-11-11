import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.spmForKmp)
    id("notesp2p.detekt")
}

// The wrapper versions come from the version catalog only, so code and build cannot disagree.
val generateBindingVersions by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/bindingVersions/kotlin")
    val versions = mapOf(
        "JAVA" to libs.versions.automergeJava.get(),
        "SWIFT" to libs.versions.automergeSwift.get(),
    )
    inputs.properties(versions)
    outputs.dir(outputDir)
    doLast {
        val constants = versions.entries.joinToString("\n") { (name, version) ->
            "    const val $name = \"$version\""
        }
        outputDir.get().file("app/droidster/automerge/BindingVersions.kt").asFile.apply {
            parentFile.mkdirs()
            writeText("package app.droidster.automerge\n\ninternal object BindingVersions {\n$constants\n}\n")
        }
    }
}

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndroid") {
                withJvm()
                withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    jvm()
    android {
        namespace = "app.droidster.automerge"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }
    // automerge-swift ships its Rust core as a static library in a binary XCFramework, which spm4kmp
    // does not link. cinterop embeds the slice of each target in the klib, so consumers need no setup.
    val ffiXcframework = layout.buildDirectory
        .dir("spmKmpPlugin/AutomergeBridge/scratch/artifacts/automerge-swift/automergeFFI/automergeFFI.xcframework")
        .get().asFile
    mapOf(
        iosArm64() to "ios-arm64",
        iosSimulatorArm64() to "ios-arm64_x86_64-simulator",
    ).forEach { (iosTarget, ffiSlice) ->
        iosTarget.compilerOptions {
            optIn.addAll("kotlinx.cinterop.ExperimentalForeignApi", "kotlinx.cinterop.BetaInteropApi")
        }
        // On the task, not the cinterop settings: spm4kmp overwrites the settings' extraOpts.
        val cinteropTask = "cinteropAutomergeBridge${iosTarget.name.replaceFirstChar { it.uppercase() }}"
        tasks.withType<CInteropProcess>().configureEach {
            if (name == cinteropTask) {
                settings.extraOpts(
                    "-libraryPath",
                    ffiXcframework.resolve(ffiSlice).path,
                    "-staticLibrary",
                    "libuniffi_automerge.a",
                )
            }
        }
        iosTarget.swiftPackageConfig(cinteropName = "AutomergeBridge") {
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/automerge/automerge-swift.git"),
                    version = libs.versions.automergeSwift.get(),
                    products = { add("Automerge") },
                )
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBindingVersions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("jvmAndroidMain").dependencies {
            implementation(libs.automerge.java)
        }
    }
}
