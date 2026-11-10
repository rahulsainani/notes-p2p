package app.droidster.automerge

/**
 * One Automerge document. Every platform wraps the Rust Automerge core, so the bytes from [save] load
 * on any platform.
 *
 * Failures throw [AutomergeException] on every platform. Call [close] when done: on JVM and Android
 * the document holds native memory.
 */
interface AutomergeDocument : AutoCloseable {
    /** Sets [key] in the root map to [value]. */
    fun putString(key: String, value: String)

    /** The string at [key] in the root map, or null if the key is missing or holds another type. */
    fun getStringOrNull(key: String): String?

    /** The full document, history included. */
    fun save(): ByteArray

    /** Merges the changes of [other] into this document. [other] stays unchanged. */
    fun merge(other: AutomergeDocument)
}

/** A new, empty document. */
expect fun AutomergeDocument(): AutomergeDocument

/** A document loaded from [bytes] saved on any platform. Throws [AutomergeException] for invalid bytes. */
expect fun AutomergeDocument(bytes: ByteArray): AutomergeDocument

/** The platform wrapper and its version, e.g. `automerge-java-0.0.9`. Keys the format fixtures. */
internal expect val bindingId: String

class AutomergeException(message: String, cause: Throwable? = null) : Exception(message, cause)
