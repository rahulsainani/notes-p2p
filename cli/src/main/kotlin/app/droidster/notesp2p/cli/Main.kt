package app.droidster.notesp2p.cli

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "--version" -> println("notes ${object {}.javaClass.`package`.implementationVersion}")

        else -> {
            System.err.println("Usage: notes --version")
            exitProcess(2)
        }
    }
}
