package app.droidster.notesp2p.core.document

import kotlin.test.Test
import kotlin.test.assertEquals

class DocVersionTest {

    @Test
    fun versionIsTheSameForAnyOrderOfItsHeads() {
        assertEquals(DocVersion.of(listOf("a", "b")), DocVersion.of(listOf("b", "a", "b")))
    }
}
