package app.droidster.notesp2p.core.order

import kotlin.random.Random

/**
 * One entry of an ordered set: an [id] and its order [key], both as plain text.
 */
internal data class Ordered(val id: String, val key: String)

/**
 * The display order: by key, then by ID, so equal keys sort the same on every device.
 */
internal val orderComparator: Comparator<Ordered> = compareBy({ it.key }, { it.id })

internal object OrderKeys {
    /**
     * The key changes that put [id] directly after [afterId], or directly before [beforeId] when
     * [afterId] is null, or first when both are null. [all] holds every entry of the set, hidden ones
     * included, so a new key never repeats a hidden entry's key.
     *
     * The result is one key for [id]. It has more entries only when neighbours share one key: no key
     * fits between equal keys, so those neighbours get new keys too.
     */
    fun place(
        all: List<Ordered>,
        id: String,
        afterId: String?,
        beforeId: String?,
        random: Random,
    ): Map<String, String> {
        val others = all.filter { it.id != id }.sortedWith(orderComparator)
        val target = beforeId ?: others.firstOrNull()?.id
        return when {
            afterId != null -> placeAfter(others, id, afterId, random)
            target != null -> placeBefore(others, id, target, random)
            else -> mapOf(id to FractionalIndex.between(after = null, before = null, random))
        }
    }

    private fun placeAfter(others: List<Ordered>, id: String, afterId: String, random: Random): Map<String, String> {
        val position = others.indexOfFirst { it.id == afterId }
        require(position >= 0) { "Unknown entry $afterId" }
        val low = others[position].key
        val following = others.drop(position + 1)
        val high = following.firstOrNull { it.key > low }?.key
        val changes = LinkedHashMap<String, String>()
        var previous = low
        (listOf(id) + following.takeWhile { it.key == low }.map { it.id }).forEach { entry ->
            previous = FractionalIndex.between(previous, high, random)
            changes[entry] = previous
        }
        return changes
    }

    private fun placeBefore(others: List<Ordered>, id: String, beforeId: String, random: Random): Map<String, String> {
        val position = others.indexOfFirst { it.id == beforeId }
        require(position >= 0) { "Unknown entry $beforeId" }
        val high = others[position].key
        val preceding = others.take(position)
        val low = preceding.lastOrNull { it.key < high }?.key
        val changes = LinkedHashMap<String, String>()
        var next = high
        (listOf(id) + preceding.takeLastWhile { it.key == high }.reversed().map { it.id }).forEach { entry ->
            next = FractionalIndex.between(low, next, random)
            changes[entry] = next
        }
        return changes
    }
}
