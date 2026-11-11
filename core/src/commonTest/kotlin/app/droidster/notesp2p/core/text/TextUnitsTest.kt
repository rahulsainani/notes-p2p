package app.droidster.notesp2p.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class TextUnitsTest {

    @Test
    fun diffKeepsTheCommonStartAndEnd() {
        val splices = listOf(
            diffSplice("Milk", "Milk"),
            diffSplice("Milk", "Oat milk"),
            diffSplice("Buy milk today", "Buy bread today"),
            diffSplice("Milk and bread", "Milk"),
            diffSplice("", "Milk"),
            diffSplice("aaa", "aa"),
        )

        assertEquals(
            listOf(
                null,
                Splice(0, 1, "Oat m"),
                Splice(4, 4, "bread"),
                Splice(4, 10, ""),
                Splice(0, 0, "Milk"),
                Splice(2, 1, ""),
            ),
            splices,
        )
    }

    @Test
    fun diffCountsAnEmojiAsOnePosition() {
        val splices = listOf(
            diffSplice("$GRIN milk", "$GRIN oat milk"),
            // Both emoji start with the same UTF-16 unit; the splice must replace the whole emoji.
            diffSplice("a$GRIN", "a$WINK"),
            diffSplice("$GRIN$GRIN", GRIN),
        )

        assertEquals(listOf(Splice(2, 0, "oat "), Splice(1, 1, WINK), Splice(1, 1, "")), splices)
    }

    @Test
    fun positionsConvertBetweenCodePointsAndUtf16() {
        val text = "a${GRIN}b"

        val positions = listOf(text.codePointCount(), text.codePointCount(3), text.utf16Index(2), text.utf16Index(9))

        assertEquals(listOf(3, 2, 3, 4), positions)
    }

    private companion object {
        const val GRIN = "😀"
        const val WINK = "😉"
    }
}
