import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("notesp2p.detekt")
}

kotlin {
    explicitApi()
    jvm()
    android {
        namespace = "app.droidster.notesp2p.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlinx.coroutines.ExperimentalCoroutinesApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":automerge-kmp"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.io.core)
            implementation(libs.cryptography.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.turbine)
        }
        jvmMain.dependencies {
            implementation(libs.cryptography.provider.jdk)
        }
        // Android has no Ed25519 in its platform providers at minSdk 29; BouncyCastle brings it.
        androidMain.dependencies {
            implementation(libs.cryptography.provider.jdkBc)
        }
        iosMain.dependencies {
            implementation(libs.cryptography.provider.cryptokit)
        }
    }
}
