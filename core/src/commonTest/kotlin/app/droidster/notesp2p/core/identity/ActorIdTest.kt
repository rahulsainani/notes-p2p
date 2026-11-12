package app.droidster.notesp2p.core.identity

import kotlin.test.Test
import kotlin.test.assertEquals

class ActorIdTest {

    @Test
    fun onlySixteenBytesOfLowercaseHexAreAnActorId() {
        assertEquals(
            listOf(true, false, false),
            listOf(
                runCatching { ActorId("ab".repeat(16)) },
                runCatching { ActorId("ab".repeat(32)) },
                runCatching { ActorId("AB".repeat(16)) },
            ).map { it.isSuccess },
        )
    }
}
