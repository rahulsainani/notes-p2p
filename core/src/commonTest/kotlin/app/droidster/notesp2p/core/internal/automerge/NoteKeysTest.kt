package app.droidster.notesp2p.core.internal.automerge

import app.droidster.notesp2p.core.note.NoteColour
import app.droidster.notesp2p.core.note.NoteKind
import kotlin.test.Test
import kotlin.test.assertEquals

// Stored IDs are the file format: a change here breaks the notes of every other device
// (docs/SchemaChanges.md). Add new IDs; never change or reuse one.
class NoteKeysTest {

    @Test
    fun kindsKeepTheirStoredIds() {
        assertEquals(listOf("text", "checklist"), NoteKind.entries.map(NoteKeys::stored))
    }

    @Test
    fun coloursKeepTheirStoredIds() {
        assertEquals(
            listOf(
                "oat", "butter", "apricot", "tomato", "rose", "plum", "iris", "sky",
                "moss", "mint", "lime", "cocoa", "stone", "sage", "clay",
            ),
            NoteColour.entries.map(NoteKeys::stored),
        )
    }

    @Test
    fun unknownStoredIdReadsAsAFixedValue() {
        assertEquals(
            listOf(NoteKind.TEXT, NoteColour.OAT),
            listOf(NoteKeys.kindOf("table"), NoteKeys.colourOf("magenta")),
        )
    }
}
