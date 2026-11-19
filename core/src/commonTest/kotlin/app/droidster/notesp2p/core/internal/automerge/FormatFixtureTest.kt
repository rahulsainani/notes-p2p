package app.droidster.notesp2p.core.internal.automerge

import app.droidster.notesp2p.core.createChecklistItem
import app.droidster.notesp2p.core.createNote
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
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The format fixtures (`FormatFixtures.kt`): every stored version still reads the same way.
 */
class FormatFixtureTest {
    @Test
    fun currentVersionHasANoteFixture() {
        assertTrue(DocAccess.SUPPORTED_VERSION in noteFixtures, missing("noteFixtures", createNoteFixture()))
    }

    @Test
    fun noteOfFormatVersion1KeepsItsMeaning() {
        NoteDocument.load(Base64.decode(noteFixtures.getValue(1)), actorId = null).use { document ->
            assertEquals(
                NoteWithRestores(
                    createNote(
                        id = NOTE,
                        title = "Groceries",
                        kind = NoteKind.CHECKLIST,
                        items = listOf(
                            createChecklistItem(ItemId("item-bread"), "Bread", order = OrderKey("a0")),
                            createChecklistItem(
                                ItemId("item-milk"),
                                "Oat milk",
                                spans = listOf(FormatSpan(0, 3, TextFormat.ITALIC)),
                                checked = true,
                                order = OrderKey("a1"),
                            ),
                        ),
                        hidden = true,
                        colour = NoteColour.LEMON,
                        isPinned = true,
                        status = NoteStatus.Trashed(
                            Instant.fromEpochMilliseconds(1_762_700_000_000),
                            fromArchive = true,
                        ),
                        order = OrderKey("a5"),
                        labelIds = setOf(LabelId("label-travel")),
                        createdAt = Instant.fromEpochMilliseconds(1_762_000_000_000),
                        updatedAt = Instant.fromEpochMilliseconds(1_762_600_000_000),
                        version = document.version(),
                    ),
                    restored = setOf(Instant.fromEpochMilliseconds(1_762_650_000_000)),
                ),
                document.read(NOTE),
            )
        }
    }

    // The message is the entry to add to FormatFixtures.kt.
    private fun missing(map: String, bytes: ByteArray): String {
        val rows = Base64.encode(bytes).chunked(BASE64_ROW).joinToString("\n") { "        $it" }
        return "$map has no fixture for format version ${DocAccess.SUPPORTED_VERSION}. Add this entry:\n" +
            "    ${DocAccess.SUPPORTED_VERSION}L to base64(\n        \"\"\"\n$rows\n        \"\"\",\n    ),"
    }

    private companion object {
        val NOTE = NoteId("note")
        const val BASE64_ROW = 76
    }
}
