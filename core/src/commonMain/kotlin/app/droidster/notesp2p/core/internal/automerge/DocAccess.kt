package app.droidster.notesp2p.core.internal.automerge

import app.droidster.automerge.AutomergeReader
import app.droidster.automerge.AutomergeTransaction
import app.droidster.automerge.AutomergeValue

/**
 * The access that this app version has to a stored document: read and write, read only, or none.
 *
 * Two root keys of each note and vault document decide it. [MIN_READER_VERSION] is the oldest format
 * version that may read the document, and [MIN_WRITER_VERSION] the oldest that may change it.
 * Example: a version 2 app adds a field that version 1 can show but must not write. It stores
 * `minReaderVersion = 1` and `minWriterVersion = 2`, so a version 1 app shows the document read-only.
 * The rules are in `docs/SchemaChanges.md`.
 */
internal enum class DocAccess {
    /**
     * This app version shows and changes the document.
     */
    READ_WRITE,

    /**
     * This app version shows the document but does not change it.
     */
    READ_ONLY,

    /**
     * This app version does not show the document. It keeps the bytes and syncs them, so that a newer
     * app version on this device or another device can read them.
     */
    UNREADABLE,
    ;

    companion object {
        /**
         * The format version that this app version reads and writes.
         */
        const val SUPPORTED_VERSION = 1L

        const val MIN_READER_VERSION = "minReaderVersion"
        const val MIN_WRITER_VERSION = "minWriterVersion"

        /**
         * Returns the access to the document of [reader]. A document without both versions does not
         * come from this app, so this app does not change it.
         */
        fun of(reader: AutomergeReader): DocAccess {
            val minReader = newest(reader, MIN_READER_VERSION)
            val minWriter = newest(reader, MIN_WRITER_VERSION)
            return when {
                minReader != null && minReader > SUPPORTED_VERSION -> UNREADABLE
                minReader == null || minWriter == null || minWriter > SUPPORTED_VERSION -> READ_ONLY
                else -> READ_WRITE
            }
        }

        // Writes at the same time on two devices leave several values. `get` picks one by operation ID,
        // not by value, so take the highest: it is the strictest limit.
        private fun newest(reader: AutomergeReader, key: String): Long? =
            reader.getAll(reader.root, key).filterIsInstance<AutomergeValue.Integer>().maxOfOrNull { it.value }
    }
}

/**
 * Writes the format version of this app version into a new document, as both the reader and the
 * writer version.
 */
internal fun AutomergeTransaction.writeSupportedVersion() {
    put(root, DocAccess.MIN_READER_VERSION, DocAccess.SUPPORTED_VERSION)
    put(root, DocAccess.MIN_WRITER_VERSION, DocAccess.SUPPORTED_VERSION)
}
