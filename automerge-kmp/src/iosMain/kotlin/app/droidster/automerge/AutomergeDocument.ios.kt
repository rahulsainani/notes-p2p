@file:OptIn(ExperimentalForeignApi::class)

package app.droidster.automerge

import AutomergeBridge.AMBDocument
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

actual fun AutomergeDocument(): AutomergeDocument = SwiftAutomergeDocument(AMBDocument())

actual fun AutomergeDocument(bytes: ByteArray): AutomergeDocument {
    val document = wrapAutomergeErrors("Cannot load document") { error ->
        AMBDocument.fromBytes(bytes.toNSData(), error = error)
    }
    return SwiftAutomergeDocument(document ?: throw AutomergeException("Cannot load document"))
}

internal actual val bindingId: String = "automerge-swift-${BindingVersions.SWIFT}"

private class SwiftAutomergeDocument(val document: AMBDocument) : AutomergeDocument {
    override fun putString(key: String, value: String) {
        wrapAutomergeErrors("Cannot put $key") { error -> document.putString(key, value = value, error = error) }
    }

    override fun getStringOrNull(key: String): String? = document.getString(key)

    override fun save(): ByteArray = document.save().toByteArray()

    override fun merge(other: AutomergeDocument) {
        wrapAutomergeErrors("Cannot merge") { error ->
            document.merge((other as SwiftAutomergeDocument).document, error = error)
        }
    }

    // Swift frees the document through ARC when the last reference goes.
    override fun close() = Unit
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
