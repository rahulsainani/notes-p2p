package app.droidster.notesp2p

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform