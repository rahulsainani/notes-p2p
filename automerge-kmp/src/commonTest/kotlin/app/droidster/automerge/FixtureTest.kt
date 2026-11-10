package app.droidster.automerge

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the cross-platform format. Every wrapper version saves one fixture; every platform loads all
 * of them. A wrapper bump has no fixture yet, so [currentBindingHasFixture] fails until one is added,
 * and the new fixture then has to load on every other platform.
 */
class FixtureTest {

    @Test
    fun currentBindingHasFixture() {
        val id = bindingId

        assertTrue(
            id in fixtures,
            "No fixture for $id. Add this line to Fixtures.kt:\n" +
                "    \"$id\" to \"${Base64.encode(createFixtureDocument())}\",",
        )
    }

    @Test
    fun everyFixtureLoadsOnThisPlatform() {
        val loaded = fixtures.mapValues { (_, encoded) ->
            AutomergeDocument(Base64.decode(encoded)).use { it.getStringOrNull(FIXTURE_KEY) }
        }

        assertEquals(fixtures.mapValues { FIXTURE_VALUE }, loaded)
    }

    private fun createFixtureDocument(): ByteArray = AutomergeDocument().use { document ->
        document.putString(FIXTURE_KEY, FIXTURE_VALUE)
        document.save()
    }
}
