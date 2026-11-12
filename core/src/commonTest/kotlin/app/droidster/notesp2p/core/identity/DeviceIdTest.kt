package app.droidster.notesp2p.core.identity

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceIdTest {

    @Test
    fun onlyThirtyTwoBytesOfLowercaseHexAreADeviceId() {
        assertEquals(
            listOf(true, false, false),
            listOf(
                runCatching { DeviceId("ab".repeat(32)) },
                runCatching { DeviceId("ab".repeat(16)) },
                runCatching { DeviceId("AB".repeat(32)) },
            ).map { it.isSuccess },
        )
    }
}
