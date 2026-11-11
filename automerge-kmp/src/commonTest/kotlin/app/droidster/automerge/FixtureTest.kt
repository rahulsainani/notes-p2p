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
        val id = "$bindingId/$FIXTURE_CONTENT"

        assertTrue(
            id in fixtures,
            "No fixture for $id. Add this line to Fixtures.kt:\n" +
                "    \"$id\" to \"${Base64.encode(createFixtureDocument())}\",",
        )
    }

    @Test
    fun everyFixtureLoadsOnThisPlatform() {
        val loaded = fixtures.mapValues { (_, encoded) ->
            AutomergeDocument(Base64.decode(encoded)).use { it.snapshot() }
        }

        assertEquals(fixtures.mapValues { (id, _) -> fixtureContents.getValue(id.substringAfter('/')) }, loaded)
    }

    private fun createFixtureDocument(): ByteArray = AutomergeDocument().use { document ->
        document.transact { tx ->
            tx.put(tx.root, "string", FIXTURE_VALUE)
            tx.put(tx.root, "integer", 1_762_600_000_000)
            tx.put(tx.root, "boolean", true)
            tx.putNull(tx.root, "null")
            val map = tx.putObject(tx.root, "map", ObjType.MAP)
            val text = tx.putText(map, "text", FIXTURE_TEXT)
            tx.mark(text, 0, 2, "bold", true, ExpandMark.AFTER)
            tx.mark(text, 0, FIXTURE_TEXT_LENGTH, "heading", 2, ExpandMark.NONE)
        }
        document.save()
    }
}
