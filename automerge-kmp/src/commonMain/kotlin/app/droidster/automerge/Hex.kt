package app.droidster.automerge

internal fun String.hexToActorBytes(): ByteArray = hexToBytes(name = "Actor ID")

internal fun String.hexToHashBytes(): ByteArray = hexToBytes(name = "Change hash")

private fun String.hexToBytes(name: String): ByteArray = try {
    hexToByteArray()
} catch (e: IllegalArgumentException) {
    throw AutomergeException("$name is not hex: $this", e)
}
