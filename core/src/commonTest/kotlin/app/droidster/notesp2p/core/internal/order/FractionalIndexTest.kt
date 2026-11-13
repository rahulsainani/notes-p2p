package app.droidster.notesp2p.core.internal.order

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FractionalIndexTest {

    @Test
    fun keysCountUpAndDownAtTheEnds() {
        val keys = listOf(
            FractionalIndex.between(null, null),
            FractionalIndex.between(null, "a0"),
            FractionalIndex.between(null, "Zz"),
            FractionalIndex.between("a0", null),
            FractionalIndex.between("az", null),
            FractionalIndex.between(null, "Z0"),
        )

        assertEquals(listOf("a0", "Zz", "Zy", "a1", "b00", "Yzz"), keys)
    }

    @Test
    fun keyBetweenTwoKeysAddsAFraction() {
        val keys = listOf(
            FractionalIndex.between("Zz", "a0"),
            FractionalIndex.between("Zz", "ZzV"),
            FractionalIndex.between("a0", "a2"),
            FractionalIndex.between("a0V", "a1"),
        )

        assertEquals(listOf("ZzV", "ZzG", "a1", "a0l"), keys)
    }

    // The base-62 cases of rocicorp/fractional-indexing, the reference that the JS, Go, Python and
    // Kotlin implementations share. Equal keys mean another device that uses one of them can share a list.
    @Test
    fun keysMatchTheReferenceImplementation() {
        val smallest = "A" + "0".repeat(26)
        val largest = "z".repeat(27)
        val cases = listOf(
            Triple(null, null, "a0"),
            Triple(null, "a0", "Zz"),
            Triple(null, "Zz", "Zy"),
            Triple("a0", null, "a1"),
            Triple("a1", null, "a2"),
            Triple("a0", "a1", "a0V"),
            Triple("a1", "a2", "a1V"),
            Triple("a0V", "a1", "a0l"),
            Triple("Zz", "a0", "ZzV"),
            Triple("Zz", "a1", "a0"),
            Triple(null, "Y00", "Xzzz"),
            Triple("bzz", null, "c000"),
            Triple("a0", "a0V", "a0G"),
            Triple("a0", "a0G", "a08"),
            Triple("b125", "b129", "b127"),
            Triple("a0", "a1V", "a1"),
            Triple("Zz", "a01", "a0"),
            Triple(null, "a0V", "a0"),
            Triple(null, "b999", "b99"),
            Triple(null, smallest + "1", smallest + "0V"),
            Triple(largest.dropLast(1) + "y", null, largest),
            Triple(largest, null, largest + "V"),
        )

        assertEquals(cases.map { it.third }, cases.map { (after, before) -> FractionalIndex.between(after, before) })
    }

    @Test
    fun invalidKeysOfTheReferenceImplementationFail() {
        val cases = listOf(
            null to "A" + "0".repeat(26),
            "a00" to null,
            "a00" to "a1",
            "0" to "1",
            "a1" to "a0",
        )

        assertEquals(
            List(cases.size) { true },
            cases.map { (after, before) -> runCatching { FractionalIndex.between(after, before) }.isFailure },
        )
    }

    @Test
    fun randomInsertsKeepEveryKeyInItsGap() {
        val random = Random(7)
        val keys = mutableListOf<String>()

        repeat(2_000) {
            val position = random.nextInt(keys.size + 1)
            keys.add(position, FractionalIndex.between(keys.getOrNull(position - 1), keys.getOrNull(position), random))
        }

        assertEquals(keys.sorted().distinct(), keys)
    }

    @Test
    fun manyInsertsIntoOneGapStayShort() {
        var low = "a0"
        repeat(600) { low = FractionalIndex.between(low, "a1") }

        assertTrue(low.length < 130, "Key has ${low.length} characters")
    }

    @Test
    fun randomKeysForOneGapDiffer() {
        val first = FractionalIndex.between("a0", "a1", Random(1))
        val second = FractionalIndex.between("a0", "a1", Random(2))

        assertNotEquals(first, second)
    }

    @Test
    fun keysInTheWrongOrderFail() {
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between("a1", "a0") }
    }

    @Test
    fun brokenKeysAreNotValid() {
        val valid = listOf("a0", "Zz", "a0V", "b00", "", "a", "a00", "!0", "b0").map(FractionalIndex::isValid)

        assertEquals(listOf(true, true, true, true, false, false, false, false, false), valid)
    }
}
