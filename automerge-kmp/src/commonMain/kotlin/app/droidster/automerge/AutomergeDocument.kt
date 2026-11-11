package app.droidster.automerge

/**
 * One Automerge document. Every platform wraps the Rust Automerge core, so the bytes from [save] load
 * on any platform.
 *
 * Failures throw [AutomergeException] on every platform. Call [close] when done: on JVM and Android
 * the document holds native memory.
 */
public interface AutomergeDocument : AutomergeReader, AutoCloseable {
    /**
     * The actor that signs this document's new changes, as lowercase hex.
     */
    public val actorId: String

    /**
     * The hashes of the newest changes, as sorted lowercase hex. Equal heads mean equal documents.
     */
    public fun heads(): List<String>

    /**
     * Runs [block] and commits its writes as one change. On JVM and Android a failing [block] rolls the
     * writes back; on iOS the writes made before the failure stay.
     */
    public fun <T> transact(block: (AutomergeTransaction) -> T): T

    /**
     * Runs [block] on the document as it was at [heads], and commits its writes as one change that
     * follows that state. [block] reads the old state; changes that came after [heads] stay, and
     * Automerge places the new change beside them. For an editor that still shows the old state.
     *
     * Returns the heads of the state that [block] made: [heads] plus the new change, without the
     * later changes. With no write, that is [heads].
     *
     * On JVM and Android the change has this document's actor. On iOS, it has a new random actor,
     * unless [heads] are the current heads: automerge-swift can only fork at past heads, and a fork
     * with this document's actor could give one sequence number to two changes.
     */
    public fun transactAt(heads: List<String>, block: (AutomergeTransaction) -> Unit): List<String>

    /**
     * The full document, history included.
     */
    public fun save(): ByteArray

    /**
     * Merges the changes of [other] into this document. [other] stays unchanged.
     */
    public fun merge(other: AutomergeDocument)
}

/**
 * A new, empty document. [actorId] is hex; null picks a random actor.
 */
public expect fun AutomergeDocument(actorId: String? = null): AutomergeDocument

/**
 * A document loaded from [bytes] saved on any platform. New changes use [actorId] (hex); null picks a
 * random actor. Throws [AutomergeException] for invalid bytes.
 */
public expect fun AutomergeDocument(bytes: ByteArray, actorId: String? = null): AutomergeDocument

/**
 * The platform wrapper and its version, e.g. `automerge-java-0.0.9`. Keys the format fixtures.
 */
internal expect val bindingId: String
