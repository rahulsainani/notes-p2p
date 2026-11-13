package app.droidster.notesp2p.core.internal.order

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderKeysTest {
    private val random = Random(3)

    /**
     * The IDs in display order after [changes] apply to [entries], with [moved] added if it is new.
     */
    private fun orderAfter(entries: List<Ordered>, moved: String, changes: Map<String, String>): List<String> =
        (entries.filter { it.id != moved } + Ordered(moved, ""))
            .map { Ordered(it.id, changes[it.id] ?: it.key) }
            .sortedWith(orderComparator)
            .map { it.id }

    private val entries = listOf(Ordered("a", "a0"), Ordered("b", "a1"), Ordered("c", "a2"))

    @Test
    fun entryMovesDirectlyAfterAnother() {
        val changes = OrderKeys.place(entries, "c", afterId = "a", beforeId = null, random)

        assertEquals(listOf("a", "c", "b") to setOf("c"), orderAfter(entries, "c", changes) to changes.keys)
    }

    @Test
    fun entryMovesDirectlyBeforeAnother() {
        val changes = OrderKeys.place(entries, "c", afterId = null, beforeId = "b", random)

        assertEquals(listOf("a", "c", "b") to setOf("c"), orderAfter(entries, "c", changes) to changes.keys)
    }

    @Test
    fun newEntryWithNoNeighbourGoesFirst() {
        val changes = OrderKeys.place(entries, "new", afterId = null, beforeId = null, random)

        assertEquals(listOf("new", "a", "b", "c"), orderAfter(entries, "new", changes))
    }

    @Test
    fun firstEntryOfAnEmptySetGetsAKey() {
        val changes = OrderKeys.place(emptyList(), "new", afterId = null, beforeId = null, random)

        assertEquals(setOf("new"), changes.keys)
    }

    @Test
    fun moveBetweenEqualKeysGivesTheNeighbourANewKeyToo() {
        // Two devices made "a" and "b" offline and got the same key; "c" now moves between them.
        val clash = listOf(Ordered("a", "a0"), Ordered("b", "a0"), Ordered("c", "a1"), Ordered("d", "a2"))

        val changes = OrderKeys.place(clash, "c", afterId = "a", beforeId = "b", random)

        assertEquals(listOf("a", "c", "b", "d") to setOf("c", "b"), orderAfter(clash, "c", changes) to changes.keys)
    }

    @Test
    fun moveBeforeEqualKeysGivesTheNeighbourANewKeyToo() {
        val clash = listOf(Ordered("a", "a0"), Ordered("b", "a0"), Ordered("c", "a1"))

        val changes = OrderKeys.place(clash, "c", afterId = null, beforeId = "b", random)

        assertEquals(listOf("a", "c", "b") to setOf("c", "a"), orderAfter(clash, "c", changes) to changes.keys)
    }

    // The worst case for key length: every move goes into the gap that the move before left.
    @Test
    fun manyMovesIntoOneGapKeepTheKeyShort() {
        var all = listOf(Ordered("first", "a0"), Ordered("last", "a1"))
        var previous = "first"

        repeat(600) { index ->
            val id = "moved$index"
            val key = OrderKeys.place(all, id, afterId = previous, beforeId = null, random).getValue(id)
            all = all + Ordered(id, key)
            previous = id
        }

        val longest = all.maxOf { it.key.length }
        assertEquals(true, longest < 200, "Longest key has $longest characters")
    }
}
