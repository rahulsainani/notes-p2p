package app.droidster.automerge

import org.automerge.AmValue
import org.automerge.ChangeHash
import org.automerge.Document
import org.automerge.NewValue
import org.automerge.ObjectId
import org.automerge.ObjectType
import org.automerge.PatchLog
import org.automerge.Read
import org.automerge.Transaction

public actual fun AutomergeDocument(actorId: String?): AutomergeDocument {
    val actor = actorId?.hexToActorBytes()
    return JavaAutomergeDocument(if (actor == null) Document() else Document(actor))
}

public actual fun AutomergeDocument(bytes: ByteArray, actorId: String?): AutomergeDocument {
    val actor = actorId?.hexToActorBytes()
    val loaded = wrapAutomergeErrors("Cannot load document") { Document.load(bytes) }
    if (actor == null) return JavaAutomergeDocument(loaded)
    // automerge-java cannot set the actor of a loaded document; a fork with the actor can.
    return try {
        JavaAutomergeDocument(wrapAutomergeErrors("Cannot set actor") { loaded.fork(actor) })
    } finally {
        loaded.free()
    }
}

internal actual val bindingId: String = "automerge-java-${BindingVersions.JAVA}"

private open class JavaAutomergeReader(private val read: Read) : AutomergeReader {
    override val root: ObjId = ROOT

    override fun get(obj: ObjId, key: String): AutomergeValue? =
        wrapAutomergeErrors("Cannot get $key") { read.get(obj.java, key) }.orElse(null)?.toCommon()

    override fun getAll(obj: ObjId, key: String): List<AutomergeValue> =
        wrapAutomergeErrors("Cannot get $key") { read.getAll(obj.java, key) }
            .orElse(null)
            ?.values()
            ?.map { it.toCommon() }
            .orEmpty()

    override fun keys(obj: ObjId): List<String> =
        wrapAutomergeErrors("Cannot read keys") { read.keys(obj.java) }.orElse(null)?.toList().orEmpty()

    override fun text(obj: ObjId): String = wrapAutomergeErrors("Cannot read text") { read.text(obj.java) }
        .orElseThrow { AutomergeException("Not a text object") }

    override fun marks(obj: ObjId): List<Mark> = wrapAutomergeErrors("Cannot read marks") { read.marks(obj.java) }
        .map { Mark(it.start.toInt(), it.end.toInt(), it.name, it.value.toCommon()) }
}

private class JavaAutomergeDocument(val document: Document) :
    JavaAutomergeReader(document),
    AutomergeDocument {
    override val actorId: String get() = document.actorId.toHexString()

    override fun heads(): List<String> = document.heads.map { it.bytes.toHexString() }.sorted()

    override fun <T> transact(block: (AutomergeTransaction) -> T): T = wrapAutomergeErrors("Cannot change document") {
        // close() rolls back a transaction that did not commit.
        document.startTransaction().use { transaction ->
            block(JavaAutomergeTransaction(transaction)).also { transaction.commit() }
        }
    }

    /**
     * automerge-java writes at past heads itself, with `startTransactionAt`, and the change has this
     * document's actor. At past [heads], this function:
     *
     * 1. Forks the document at [heads] and frees the fork at once. The fork is only a check:
     *    `startTransactionAt` accepts heads that the document does not hold, and the change then names a
     *    parent that no device has. A fork at such heads fails with [AutomergeException].
     * 2. Runs [block] in a transaction at [heads], so [block] reads and changes the old state.
     * 3. Commits, and returns the hash of the new change; with no write, [heads].
     */
    override fun transactAt(heads: List<String>, block: (AutomergeTransaction) -> Unit): List<String> {
        if (heads.toSet() == heads().toSet()) {
            transact(block)
            return heads()
        }
        val hashes = heads.map { Hash(it.hexToHashBytes()) }.toTypedArray<ChangeHash>()
        return wrapAutomergeErrors("Cannot change document at $heads") {
            document.fork(hashes).free()
            val patchLog = PatchLog()
            try {
                document.startTransactionAt(patchLog, hashes).use { transaction ->
                    block(JavaAutomergeTransaction(transaction))
                    transaction.commit().map { listOf(it.bytes.toHexString()) }.orElse(heads)
                }
            } finally {
                patchLog.free()
            }
        }
    }

    override fun save(): ByteArray = document.save()

    override fun merge(other: AutomergeDocument) {
        val otherDocument = (other as JavaAutomergeDocument).document
        wrapAutomergeErrors("Cannot merge") { document.merge(otherDocument) }
    }

    override fun close() {
        document.free()
    }
}

private class JavaAutomergeTransaction(private val transaction: Transaction) :
    JavaAutomergeReader(transaction),
    AutomergeTransaction {
    override fun put(obj: ObjId, key: String, value: String) = transaction.set(obj.java, key, value)

    // automerge-java 0.0.9 stores NewValue.integer as a float, so a long goes in as an int or a uint.
    override fun put(obj: ObjId, key: String, value: Long) = when {
        value in Int.MIN_VALUE..Int.MAX_VALUE -> transaction.set(obj.java, key, value.toInt())
        value > 0 -> transaction.setUint(obj.java, key, value)
        else -> throw AutomergeException("automerge-java cannot store an integer below ${Int.MIN_VALUE}: $value")
    }

    override fun put(obj: ObjId, key: String, value: Boolean) = transaction.set(obj.java, key, value)

    override fun putNull(obj: ObjId, key: String) = transaction.setNull(obj.java, key)

    override fun putObject(obj: ObjId, key: String, type: ObjType): ObjId =
        ObjId(transaction.set(obj.java, key, ObjectType.valueOf(type.name)))

    override fun delete(obj: ObjId, key: String) = transaction.delete(obj.java, key)

    override fun spliceText(obj: ObjId, start: Int, deleteCount: Int, text: String) =
        transaction.spliceText(obj.java, start.toLong(), deleteCount.toLong(), text)

    override fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Boolean, expand: ExpandMark) =
        transaction.mark(obj.java, start.toLong(), end.toLong(), name, value, expand.java)

    override fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Long, expand: ExpandMark) =
        transaction.mark(obj.java, start.toLong(), end.toLong(), name, NewValue.integer(value), expand.java)

    override fun unmark(obj: ObjId, start: Int, end: Int, name: String, expand: ExpandMark) =
        transaction.unmark(obj.java, name, start.toLong(), end.toLong(), expand.java)
}

// The constructor of ChangeHash is protected, so a hash from hex needs a subclass.
private class Hash(bytes: ByteArray) : ChangeHash(bytes)

private val ROOT = ObjId(ObjectId.ROOT)

private val ObjId.java: ObjectId get() = platform as ObjectId

private val ExpandMark.java: org.automerge.ExpandMark get() = org.automerge.ExpandMark.valueOf(name)

private fun AmValue.toCommon(): AutomergeValue = when (this) {
    is AmValue.Str -> AutomergeValue.Str(value)
    is AmValue.Int -> AutomergeValue.Integer(value)
    is AmValue.UInt -> AutomergeValue.Integer(value)
    is AmValue.Bool -> AutomergeValue.Bool(value)
    is AmValue.Null -> AutomergeValue.Null
    is AmValue.Map -> AutomergeValue.Obj(ObjId(id), ObjType.MAP)
    is AmValue.List -> AutomergeValue.Obj(ObjId(id), ObjType.LIST)
    is AmValue.Text -> AutomergeValue.Obj(ObjId(id), ObjType.TEXT)
    else -> AutomergeValue.Other
}

// automerge-java's own error class is package-private, so it is matched by name. Other runtime
// exceptions (misuse, a missing native library) are bugs, not document errors, and propagate unchanged.
internal const val JAVA_AUTOMERGE_EXCEPTION = "org.automerge.AutomergeException"

@Suppress("TooGenericExceptionCaught") // automerge-java's error class is package-private; see above.
private inline fun <T> wrapAutomergeErrors(message: String, block: () -> T): T = try {
    block()
} catch (e: RuntimeException) {
    if (e.javaClass.name != JAVA_AUTOMERGE_EXCEPTION) throw e
    throw AutomergeException(message, e)
}
