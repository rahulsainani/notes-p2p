package app.droidster.notesp2p.core.note

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattedTextTest {

    @Test
    fun spanMustFormatSomeOfTheText() {
        val spans =
            listOf(
                FormatSpan(0, 4, TextFormat.BOLD),
                FormatSpan(2, 2, TextFormat.BOLD),
                FormatSpan(-1, 2, TextFormat.BOLD),
                FormatSpan(2, 5, TextFormat.BOLD),
            )

        assertEquals(
            listOf(true, false, false, false),
            spans.map { span -> runCatching { FormattedText("Milk", listOf(span)) }.isSuccess },
        )
    }
}
