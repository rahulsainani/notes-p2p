plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: each module brings its own Kotlin plugin; the convention only reads its targets.
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("detekt") {
            id = "notesp2p.detekt"
            implementationClass = "DetektConventionPlugin"
        }
    }
}
