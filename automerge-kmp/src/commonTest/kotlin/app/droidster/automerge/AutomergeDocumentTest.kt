package app.droidster.automerge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AutomergeDocumentTest {

    @Test
    fun savedDocumentLoadsWithItsValues() {
        val saved = AutomergeDocument().use { document ->
            document.putString("title", "Groceries")
            document.save()
        }

        val title = AutomergeDocument(saved).use { it.getStringOrNull("title") }

        assertEquals("Groceries", title)
    }

    @Test
    fun missingKeyReadsAsNull() {
        val value = AutomergeDocument().use { it.getStringOrNull("title") }

        assertNull(value)
    }

    @Test
    fun concurrentEditsToDifferentKeysBothSurviveMerge() {
        val phone = AutomergeDocument().apply { putString("title", "Groceries") }
        val laptop = AutomergeDocument(phone.save())
        phone.putString("colour", "yellow")
        laptop.putString("pinned", "true")

        phone.merge(laptop)
        val merged = listOf("title", "colour", "pinned").associateWith { phone.getStringOrNull(it) }
        phone.close()
        laptop.close()

        assertEquals(mapOf("title" to "Groceries", "colour" to "yellow", "pinned" to "true"), merged)
    }

    @Test
    fun invalidBytesFailToLoad() {
        assertFailsWith<AutomergeException> { AutomergeDocument(byteArrayOf(1, 2, 3)) }
    }
}
