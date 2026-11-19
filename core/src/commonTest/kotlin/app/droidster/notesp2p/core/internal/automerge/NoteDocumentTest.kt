package app.droidster.notesp2p.core.internal.automerge

import app.droidster.automerge.AutomergeDocument
import app.droidster.automerge.AutomergeTransaction
import app.droidster.notesp2p.core.NOW
import app.droidster.notesp2p.core.createChecklistItem
import app.droidster.notesp2p.core.createNote
import app.droidster.notesp2p.core.identity.ActorId
import app.droidster.notesp2p.core.note.FormatSpan
import app.droidster.notesp2p.core.note.ItemId
import app.droidster.notesp2p.core.note.NoteColour
import app.droidster.notesp2p.core.note.NoteId
import app.droidster.notesp2p.core.note.NoteKind
import app.droidster.notesp2p.core.note.NoteStatus
import app.droidster.notesp2p.core.note.OrderKey
import app.droidster.notesp2p.core.note.TextFormat
import app.droidster.notesp2p.core.vault.LabelId
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

/**
 * Reads of a note document: the stored format, its fallbacks and its format versions.
 */
class NoteDocumentTest {

    @Test
    fun newDocumentReadsAsAnEmptyNote() {
        NoteDocument.create(ACTOR, NOW, OrderKey("a0"), NoteKind.CHECKLIST, NoteColour.SKY).use { document ->
            assertEquals(
                NoteWithRestores(
                    createNote(
                        id = ID,
                        kind = NoteKind.CHECKLIST,
                        colour = NoteColour.SKY,
                        version = document.version(),
                    ),
                ),
                document.read(ID),
            )
        }
    }

    @Test
    fun missingValuesReadAsFallbacks() {
        document { it.writeSupportedVersion() }.use { document ->
            assertEquals(
                NoteWithRestores(
                    createNote(
                        id = ID,
                        order = OrderKey("a0"),
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0),
                        version = document.version(),
                    ),
                ),
                document.read(ID),
            )
        }
    }

    @Test
    fun formatVersionsDecideTheAccess() {
        val cases = listOf<(AutomergeTransaction) -> Unit>(
            { versions(it, reader = 1, writer = 1) },
            { versions(it, reader = 1, writer = 2) },
            { versions(it, reader = 2, writer = 2) },
            { it.put(it.root, DocAccess.MIN_READER_VERSION, 1L) },
            { },
        )

        assertEquals(
            listOf(
                DocAccess.READ_WRITE,
                DocAccess.READ_ONLY,
                DocAccess.UNREADABLE,
                DocAccess.READ_ONLY,
                DocAccess.READ_ONLY,
            ),
            cases.map { block -> document(block).use { it.access() } },
        )
    }

    @Test
    fun conflictingFormatVersionsTakeTheHighest() {
        // Device 1 starts the note; device 2 raises the writer version while device 1 writes version 1 again.
        val merged = AutomergeDocument("01").use { first ->
            first.transact { versions(it, reader = 1, writer = 1) }
            AutomergeDocument(first.save(), "02").use { second ->
                second.transact { versions(it, reader = 1, writer = 2) }
                first.transact { versions(it, reader = 1, writer = 1) }
                first.merge(second)
            }
            first.save()
        }

        NoteDocument.load(merged, actorId = null).use { assertEquals(DocAccess.READ_ONLY, it.access()) }
    }

    @Test
    fun readOnlyDocumentReadsAsAReadOnlyNote() {
        document { versions(it, reader = 1, writer = 2) }.use { assertEquals(true, it.read(ID).note.isReadOnly) }
    }

    @Test
    fun unreadableDocumentHasNoNote() {
        document { versions(it, reader = 2, writer = 2) }.use { document ->
            assertFailsWith<IllegalStateException> { document.read(ID) }
        }
    }

    private fun versions(tx: AutomergeTransaction, reader: Long, writer: Long) {
        tx.put(tx.root, DocAccess.MIN_READER_VERSION, reader)
        tx.put(tx.root, DocAccess.MIN_WRITER_VERSION, writer)
    }

    // A note document with only what [block] writes, as another app version or program could make it.
    @Suppress("MissingUseCall") // The caller closes the document in `use`.
    private fun document(block: (AutomergeTransaction) -> Unit): NoteDocument {
        val bytes = AutomergeDocument().use { document ->
            document.transact(block)
            document.save()
        }
        return NoteDocument.load(bytes, actorId = null)
    }

    private companion object {
        val ID = NoteId("note")
        val ACTOR = ActorId("0a0b0c0d0e0f00010203040506070809")
    }
}
