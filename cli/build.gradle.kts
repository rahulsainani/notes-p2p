plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

version = "0.1.0"

tasks.jar {
    manifest { attributes("Implementation-Version" to project.version) }
}

application {
    applicationName = "notes"
    mainClass = "app.droidster.notesp2p.cli.MainKt"
}
