package app.droidster.notesp2p.core.text

// Automerge counts text positions in Unicode code points; Kotlin strings count UTF-16 units. An emoji
// is one code point and two units, so every position crosses this file on its way in or out.

/**
 * The number of code points in the first [endIndex] UTF-16 units.
 */
internal fun String.codePointCount(endIndex: Int = length): Int {
    var count = 0
    var index = 0
    while (index < endIndex) {
        index += if (isSurrogatePairAt(index)) 2 else 1
        count++
    }
    return count
}

/**
 * The UTF-16 index of the code point at [codePointIndex].
 */
internal fun String.utf16Index(codePointIndex: Int): Int {
    var index = 0
    repeat(codePointIndex) {
        if (index >= length) return length
        index += if (isSurrogatePairAt(index)) 2 else 1
    }
    return index
}

private fun String.isSurrogatePairAt(index: Int): Boolean =
    this[index].isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()

/**
 * One text change, in code points: remove [deleteCount] at [start], then insert [insert] there.
 */
internal data class Splice(val start: Int, val deleteCount: Int, val insert: String)

/**
 * The single splice that turns [old] into [new], or null when they are equal. It keeps the longest
 * common start and end, so a merge keeps the other device's edits outside the changed range.
 */
internal fun diffSplice(old: String, new: String): Splice? {
    if (old == new) return null
    var prefix = 0
    val maxPrefix = minOf(old.length, new.length)
    while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++
    // Never cut a surrogate pair in half.
    if (prefix > 0 && old[prefix - 1].isHighSurrogate()) prefix--
    var suffix = 0
    val maxSuffix = minOf(old.length, new.length) - prefix
    while (suffix < maxSuffix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    if (suffix > 0 && old[old.length - suffix].isLowSurrogate()) suffix--
    val removed = old.substring(prefix, old.length - suffix)
    return Splice(
        start = old.codePointCount(prefix),
        deleteCount = removed.codePointCount(),
        insert = new.substring(prefix, new.length - suffix),
    )
}
