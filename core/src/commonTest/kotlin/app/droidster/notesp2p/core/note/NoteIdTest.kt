package app.droidster.notesp2p.core.note

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteIdTest {

    @Test
    fun idMustBeSafeAsAFileName() {
        val values = listOf("a1_B-", "", "../vault", "a/b", "x".repeat(129))

        assertEquals(
            listOf(true, false, false, false, false),
            values.map { value -> runCatching { NoteId(value) }.isSuccess },
        )
    }
}
