package app.droidster.automerge

import org.automerge.AmValue
import org.automerge.Document
import org.automerge.ObjectId

actual fun AutomergeDocument(): AutomergeDocument = JavaAutomergeDocument(Document())

actual fun AutomergeDocument(bytes: ByteArray): AutomergeDocument =
    JavaAutomergeDocument(wrapAutomergeErrors("Cannot load document") { Document.load(bytes) })

internal actual val bindingId: String = "automerge-java-${BindingVersions.JAVA}"

private class JavaAutomergeDocument(val document: Document) : AutomergeDocument {
    override fun putString(key: String, value: String) {
        wrapAutomergeErrors("Cannot put $key") {
            document.startTransaction().use { transaction ->
                transaction.set(ObjectId.ROOT, key, value)
                transaction.commit()
            }
        }
    }

    override fun getStringOrNull(key: String): String? =
        (document.get(ObjectId.ROOT, key).orElse(null) as? AmValue.Str)?.value

    override fun save(): ByteArray = document.save()

    override fun merge(other: AutomergeDocument) {
        val otherDocument = (other as JavaAutomergeDocument).document
        wrapAutomergeErrors("Cannot merge") { document.merge(otherDocument) }
    }

    override fun close() {
        document.free()
    }
}

// automerge-java's own error class is package-private, so it is matched by name. Other runtime
// exceptions (misuse, a missing native library) are bugs, not document errors, and propagate unchanged.
internal const val JAVA_AUTOMERGE_EXCEPTION = "org.automerge.AutomergeException"

private inline fun <T> wrapAutomergeErrors(message: String, block: () -> T): T = try {
    block()
} catch (e: RuntimeException) {
    if (e.javaClass.name != JAVA_AUTOMERGE_EXCEPTION) throw e
    throw AutomergeException(message, e)
}
