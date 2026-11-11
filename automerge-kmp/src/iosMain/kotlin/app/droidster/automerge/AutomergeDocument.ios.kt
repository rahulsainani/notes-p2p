@file:OptIn(ExperimentalForeignApi::class)

package app.droidster.automerge

import AutomergeBridge.AMBDocument
import AutomergeBridge.AMBMark
import AutomergeBridge.AMBObjId
import AutomergeBridge.AMBValue
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.posix.memcpy

public actual fun AutomergeDocument(actorId: String?): AutomergeDocument {
    val actor = actorId?.hexToActorBytes()?.toNSData()
    val document = wrapAutomergeErrors("Cannot create document") { error -> AMBDocument.create(actor, error = error) }
    return SwiftAutomergeDocument(document ?: throw AutomergeException("Cannot create document"))
}

public actual fun AutomergeDocument(bytes: ByteArray, actorId: String?): AutomergeDocument {
    val actor = actorId?.hexToActorBytes()?.toNSData()
    val document = wrapAutomergeErrors("Cannot load document") { error ->
        AMBDocument.fromBytes(bytes.toNSData(), actor = actor, error = error)
    }
    return SwiftAutomergeDocument(document ?: throw AutomergeException("Cannot load document"))
}

internal actual val bindingId: String = "automerge-swift-${BindingVersions.SWIFT}"

// automerge-swift commits by itself, so the document is also its own transaction.
@Suppress("TooManyFunctions") // Every member of AutomergeDocument and AutomergeTransaction.
private class SwiftAutomergeDocument(val document: AMBDocument) :
    AutomergeDocument,
    AutomergeTransaction {
    override val root: ObjId = ROOT

    override val actorId: String get() = document.actorId()

    override fun heads(): List<String> = document.heads().map { it as String }

    override fun <T> transact(block: (AutomergeTransaction) -> T): T = block(this)

    /**
     * automerge-swift has no write at past heads. At the current [heads], [block] writes on this
     * document. At past [heads], this function:
     *
     * 1. Forks the document at [heads]: a new document with the history up to that state only. A fork
     *    at heads that the document does not hold fails with [AutomergeException].
     * 2. Runs [block] on the fork, so [block] reads and changes the old state.
     * 3. Merges the fork back into this document, next to the changes that came after [heads].
     * 4. Returns the heads of the fork: the hash of the new change, which follows [heads]; with no
     *    write, [heads].
     *
     * The fork keeps the new random actor that automerge-swift gives it. With this document's actor,
     * the fork would lack the later changes of that actor and give its change a sequence number that
     * one of them already has; the merge then fails.
     */
    override fun transactAt(heads: List<String>, block: (AutomergeTransaction) -> Unit): List<String> {
        if (heads.toSet() == heads().toSet()) {
            block(this)
            return heads()
        }
        val hashes = heads.joinToString("").hexToHashBytes().toNSData()
        val fork = wrapAutomergeErrors("Cannot change document at $heads") { error ->
            document.forkAt(hashes, error = error)
        } ?: throw AutomergeException("Cannot change document at $heads")
        val forked = SwiftAutomergeDocument(fork)
        block(forked)
        merge(forked)
        return forked.heads()
    }

    override fun get(obj: ObjId, key: String): AutomergeValue? =
        wrapAutomergeErrors("Cannot get $key") { error -> document.get(obj.swift, key = key, error = error) }
            ?.takeIf { it.kind != KIND_MISSING }
            ?.toCommon()

    override fun getAll(obj: ObjId, key: String): List<AutomergeValue> =
        wrapAutomergeErrors("Cannot get $key") { error -> document.getAll(obj.swift, key = key, error = error) }
            .orEmpty()
            .map { (it as AMBValue).toCommon() }

    override fun keys(obj: ObjId): List<String> = document.keys(obj.swift).map { it as String }

    override fun text(obj: ObjId): String =
        wrapAutomergeErrors("Cannot read text") { error -> document.text(obj.swift, error = error) }
            ?: throw AutomergeException("Not a text object")

    override fun marks(obj: ObjId): List<Mark> =
        wrapAutomergeErrors("Cannot read marks") { error -> document.marks(obj.swift, error = error) }
            .orEmpty()
            .map { it as AMBMark }
            .map { Mark(it.start.toInt(), it.end.toInt(), it.name, it.value.toCommon()) }

    override fun put(obj: ObjId, key: String, value: String) {
        wrapAutomergeErrors("Cannot put $key") { error ->
            document.putString(obj.swift, key = key, value = value, error = error)
        }
    }

    override fun put(obj: ObjId, key: String, value: Long) {
        wrapAutomergeErrors("Cannot put $key") { error ->
            document.putInteger(obj.swift, key = key, value = value, error = error)
        }
    }

    override fun put(obj: ObjId, key: String, value: Boolean) {
        wrapAutomergeErrors("Cannot put $key") { error ->
            document.putBoolean(obj.swift, key = key, value = value, error = error)
        }
    }

    override fun putNull(obj: ObjId, key: String) {
        wrapAutomergeErrors("Cannot put $key") { error -> document.putNull(obj.swift, key = key, error = error) }
    }

    override fun putObject(obj: ObjId, key: String, type: ObjType): ObjId {
        val kind = when (type) {
            ObjType.MAP -> KIND_MAP
            ObjType.LIST -> KIND_LIST
            ObjType.TEXT -> KIND_TEXT
        }
        val id = wrapAutomergeErrors("Cannot put $key") { error ->
            document.putObject(obj.swift, key = key, kind = kind, error = error)
        }
        return ObjId(id ?: throw AutomergeException("Cannot put $key"))
    }

    override fun delete(obj: ObjId, key: String) {
        wrapAutomergeErrors("Cannot delete $key") { error -> document.delete(obj.swift, key = key, error = error) }
    }

    override fun spliceText(obj: ObjId, start: Int, deleteCount: Int, text: String) {
        wrapAutomergeErrors("Cannot splice text") { error ->
            document.spliceText(
                obj.swift,
                start = start.toLong(),
                delete = deleteCount.toLong(),
                text = text,
                error = error,
            )
        }
    }

    override fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Boolean, expand: ExpandMark) {
        wrapAutomergeErrors("Cannot mark $name") { error ->
            document.markBoolean(
                obj.swift,
                start = start.toLong(),
                end = end.toLong(),
                name = name,
                value = value,
                expand = expand.ordinal.toLong(),
                error = error,
            )
        }
    }

    override fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Long, expand: ExpandMark) {
        wrapAutomergeErrors("Cannot mark $name") { error ->
            document.markInteger(
                obj.swift,
                start = start.toLong(),
                end = end.toLong(),
                name = name,
                value = value,
                expand = expand.ordinal.toLong(),
                error = error,
            )
        }
    }

    override fun unmark(obj: ObjId, start: Int, end: Int, name: String, expand: ExpandMark) {
        wrapAutomergeErrors("Cannot unmark $name") { error ->
            document.unmark(
                obj.swift,
                start = start.toLong(),
                end = end.toLong(),
                name = name,
                expand = expand.ordinal.toLong(),
                error = error,
            )
        }
    }

    override fun save(): ByteArray = document.save().toByteArray()

    override fun merge(other: AutomergeDocument) {
        wrapAutomergeErrors("Cannot merge") { error ->
            document.merge((other as SwiftAutomergeDocument).document, error = error)
        }
    }

    // Swift frees the document through ARC when the last reference goes.
    override fun close() = Unit
}

private val ROOT = ObjId(AMBObjId.root())

private val ObjId.swift: AMBObjId get() = platform as AMBObjId

// The kinds of AMBValue in the Swift bridge.
private const val KIND_MISSING = 0L
private const val KIND_STRING = 1L
private const val KIND_INTEGER = 2L
private const val KIND_BOOLEAN = 3L
private const val KIND_NULL = 4L
private const val KIND_MAP = 5L
private const val KIND_LIST = 6L
private const val KIND_TEXT = 7L

private fun AMBValue.toCommon(): AutomergeValue = when (kind) {
    KIND_STRING -> AutomergeValue.Str(string.orEmpty())
    KIND_INTEGER -> AutomergeValue.Integer(integer)
    KIND_BOOLEAN -> AutomergeValue.Bool(boolean)
    KIND_NULL -> AutomergeValue.Null
    KIND_MAP -> AutomergeValue.Obj(ObjId(objId!!), ObjType.MAP)
    KIND_LIST -> AutomergeValue.Obj(ObjId(objId!!), ObjType.LIST)
    KIND_TEXT -> AutomergeValue.Obj(ObjId(objId!!), ObjType.TEXT)
    else -> AutomergeValue.Other
}

private fun <T> wrapAutomergeErrors(message: String, block: (error: CPointer<ObjCObjectVar<NSError?>>) -> T): T =
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val result = block(error.ptr)
        error.value?.let { throw AutomergeException("$message: ${it.localizedDescription}") }
        result
    }

private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
}

private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
}
