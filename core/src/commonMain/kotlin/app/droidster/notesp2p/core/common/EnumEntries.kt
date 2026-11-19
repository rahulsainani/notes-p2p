package app.droidster.notesp2p.core.common

import kotlin.enums.EnumEntries

/**
 * Returns the first entry that matches [predicate], or [ifNone] when no entry matches.
 */
public fun <E : Enum<E>> EnumEntries<E>.firstOrDefault(ifNone: E, predicate: (E) -> Boolean): E =
    firstOrNull(predicate) ?: ifNone
