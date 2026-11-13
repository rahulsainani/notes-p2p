package app.droidster.notesp2p.core.internal.order

import kotlin.random.Random

/**
 * Order keys: strings that sort in plain character order, where a new key always fits between two
 * different keys. A key is a whole-number part plus an optional fraction, in base 62.
 *
 * The first character gives the length of the whole-number part: `a` = 1 digit, `b` = 2 digits; `Z` =
 * 1 digit below zero, `Y` = 2 digits. So `Zy` < `Zz` < `a0` < `a1` < `b00`, and `a0V` lies between
 * `a0` and `a1`. New keys at either end count up or down, so they stay short.
 */
@Suppress("TooManyFunctions") // One algorithm in base 62; the private functions are its steps.
internal object FractionalIndex {
    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val BASE = 62
    private const val SMALLEST_INTEGER = "A00000000000000000000000000"
    private const val RANDOM_STEPS = 10

    /**
     * The first key of an empty sequence.
     */
    public const val FIRST: String = "a0"

    /**
     * Returns a key between [after] and [before]; `null` means no neighbour on that side. The result
     * is deterministic: two devices get the same key for the same neighbours.
     */
    public fun between(after: String?, before: String?): String {
        after?.let(::validate)
        before?.let(::validate)
        require(after == null || before == null || after < before) { "$after is not before $before" }
        return when {
            after == null -> before?.let(::keyBefore) ?: FIRST
            before == null -> keyAfter(after)
            else -> keyBetween(after, before)
        }
    }

    private fun keyBefore(before: String): String {
        val integer = integerPart(before)
        return when {
            integer == SMALLEST_INTEGER -> integer + midpoint("", before.substring(integer.length))
            integer < before -> integer
            else -> checkNotNull(decrement(integer)) { "No key fits before $before" }
        }
    }

    private fun keyAfter(after: String): String {
        val integer = integerPart(after)
        return increment(integer) ?: (integer + midpoint(after.substring(integer.length), null))
    }

    private fun keyBetween(after: String, before: String): String {
        val afterInteger = integerPart(after)
        val afterFraction = after.substring(afterInteger.length)
        val beforeInteger = integerPart(before)
        if (afterInteger == beforeInteger) {
            return afterInteger + midpoint(afterFraction, before.substring(beforeInteger.length))
        }
        val next = checkNotNull(increment(afterInteger)) { "No key fits after $after" }
        return if (next < before) next else afterInteger + midpoint(afterFraction, null)
    }

    /**
     * Returns a key at a random point between [after] and [before]. Two devices that fill the same gap
     * at the same time then almost never make the same key.
     */
    public fun between(after: String?, before: String?, random: Random): String {
        var low = after
        var high = before
        repeat(RANDOM_STEPS) {
            val middle = between(low, high)
            if (random.nextBoolean()) low = middle else high = middle
        }
        return between(low, high)
    }

    /**
     * Returns `true` when [key] is an order key.
     */
    public fun isValid(key: String): Boolean = runCatching { validate(key) }.isSuccess

    private fun validate(key: String) {
        require(key != SMALLEST_INTEGER) { "Invalid order key: $key" }
        val integer = integerPart(key)
        require(key.all { it in DIGITS }) { "Invalid order key: $key" }
        require(!key.substring(integer.length).endsWith('0')) { "Invalid order key: $key" }
    }

    private fun integerLength(head: Char): Int = when (head) {
        in 'a'..'z' -> head - 'a' + 2
        in 'A'..'Z' -> 'Z' - head + 2
        else -> throw IllegalArgumentException("Invalid order key head: $head")
    }

    private fun integerPart(key: String): String {
        require(key.isNotEmpty()) { "Invalid order key: empty" }
        val length = integerLength(key[0])
        require(length <= key.length) { "Invalid order key: $key" }
        return key.substring(0, length)
    }

    /**
     * The key in the middle of the fractions [after] and [before]; a null [before] is the upper end.
     */
    private fun midpoint(after: String, before: String?): String {
        if (before != null) {
            var common = 0
            while (common < before.length && (after.getOrNull(common) ?: '0') == before[common]) common++
            if (common > 0) {
                return before.substring(0, common) + midpoint(after.drop(common), before.substring(common))
            }
        }
        val afterDigit = if (after.isEmpty()) 0 else DIGITS.indexOf(after[0])
        val beforeDigit = if (before == null) BASE else DIGITS.indexOf(before[0])
        return when {
            beforeDigit - afterDigit > 1 -> DIGITS[(afterDigit + beforeDigit + 1) / 2].toString()
            before != null && before.length > 1 -> before.substring(0, 1)
            else -> DIGITS[afterDigit] + midpoint(after.drop(1), null)
        }
    }

    private fun increment(integer: String): String? {
        val head = integer[0]
        val digits = integer.substring(1).toCharArray()
        var carry = true
        var index = digits.lastIndex
        while (carry && index >= 0) {
            val digit = DIGITS.indexOf(digits[index]) + 1
            if (digit == BASE) {
                digits[index] = '0'
            } else {
                digits[index] = DIGITS[digit]
                carry = false
            }
            index--
        }
        val rest = digits.concatToString()
        return when {
            !carry -> head + rest
            head == 'Z' -> "a0"
            head == 'z' -> null
            else -> (head + 1).let { nextHead -> nextHead + if (nextHead > 'a') rest + "0" else rest.dropLast(1) }
        }
    }

    private fun decrement(integer: String): String? {
        val head = integer[0]
        val digits = integer.substring(1).toCharArray()
        var borrow = true
        var index = digits.lastIndex
        while (borrow && index >= 0) {
            val digit = DIGITS.indexOf(digits[index]) - 1
            if (digit == -1) {
                digits[index] = DIGITS.last()
            } else {
                digits[index] = DIGITS[digit]
                borrow = false
            }
            index--
        }
        val rest = digits.concatToString()
        return when {
            !borrow -> head + rest

            head == 'a' -> "Z" + DIGITS.last()

            head == 'A' -> null

            else -> (head - 1).let { previousHead ->
                previousHead + if (previousHead < 'Z') rest + DIGITS.last() else rest.dropLast(1)
            }
        }
    }
}
