package app.droidster.notesp2p.core.note

import app.droidster.notesp2p.core.internal.order.FractionalIndex
import kotlin.jvm.JvmInline

/**
 * A position in an ordered set. A new key always fits between two different keys, so a move changes
 * one key. Keys sort in character order.
 *
 * @see FractionalIndex
 */
@JvmInline
public value class OrderKey(public val value: String) : Comparable<OrderKey> {
    init {
        require(FractionalIndex.isValid(value)) { "Invalid order key: $value" }
    }

    override fun compareTo(other: OrderKey): Int = value.compareTo(other.value)
}
