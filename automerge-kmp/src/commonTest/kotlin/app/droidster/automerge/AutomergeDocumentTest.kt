package app.droidster.automerge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AutomergeDocumentTest {

    @Test
    fun savedDocumentLoadsWithItsValues() {
        val saved = AutomergeDocument().use { document ->
            document.transact { tx ->
                tx.put(tx.root, "colour", "yellow")
                tx.put(tx.root, "createdAt", 1_762_600_000_000)
                tx.put(tx.root, "pinned", true)
                tx.putNull(tx.root, "deletedAt")
                tx.putText(tx.root, "title", "Groceries")
                val item = tx.putObject(tx.root, "item", ObjType.MAP)
                tx.putText(item, "text", "Milk")
                tx.put(item, "done", false)
            }
            document.save()
        }

        val loaded = AutomergeDocument(saved).use { it.snapshot() }

        assertEquals(
            mapOf(
                "colour" to "yellow",
                "createdAt" to 1_762_600_000_000,
                "pinned" to true,
                "deletedAt" to null,
                "title" to TextSnapshot("Groceries"),
                "item" to mapOf("text" to TextSnapshot("Milk"), "done" to false),
            ),
            loaded,
        )
    }

    @Test
    fun integersKeepTheirValue() {
        val values = listOf(0, 5, -5, Int.MAX_VALUE + 1L, 1_762_600_000_000, Long.MAX_VALUE)

        val loaded = AutomergeDocument().use { document ->
            document.transact { tx -> values.forEachIndexed { index, value -> tx.put(tx.root, "k$index", value) } }
            values.indices.map { document.get(document.root, "k$it") }
        }

        assertEquals(values.map { AutomergeValue.Integer(it) }, loaded)
    }

    @Test
    fun missingKeyReadsAsNull() {
        val value = AutomergeDocument().use { it.get(it.root, "title") }

        assertNull(value)
    }

    @Test
    fun typedReadsReturnOnlyTheirOwnType() {
        val reads = AutomergeDocument().use { document ->
            document.transact { tx ->
                tx.put(tx.root, "colour", "yellow")
                tx.put(tx.root, "createdAt", 5)
                tx.put(tx.root, "pinned", true)
                tx.putText(tx.root, "title", "Groceries")
            }
            val root = document.root
            listOf(
                document.getString(root, "colour"),
                document.getLong(root, "createdAt"),
                document.getBoolean(root, "pinned"),
                document.getObject(root, "title", ObjType.TEXT)?.let(document::text),
                document.getString(root, "createdAt"),
                document.getLong(root, "pinned"),
                document.getBoolean(root, "colour"),
                document.getObject(root, "title", ObjType.MAP),
                document.getString(root, "missing"),
            )
        }

        assertEquals(listOf("yellow", 5L, true, "Groceries", null, null, null, null, null), reads)
    }

    @Test
    fun deletedKeyIsGone() {
        val keys = AutomergeDocument().use { document ->
            document.transact { tx ->
                tx.put(tx.root, "colour", "yellow")
                tx.put(tx.root, "pinned", true)
            }
            document.transact { tx -> tx.delete(tx.root, "colour") }
            document.keys(document.root)
        }

        assertEquals(listOf("pinned"), keys)
    }

    @Test
    fun concurrentEditsToDifferentKeysBothSurviveMerge() {
        val merged = AutomergeDocument().use { phone ->
            phone.transact { it.put(it.root, "colour", "yellow") }
            AutomergeDocument(phone.save()).use { laptop ->
                phone.transact { it.put(it.root, "pinned", true) }
                laptop.transact { it.put(it.root, "order", "a0") }

                phone.merge(laptop)
                phone.snapshot()
            }
        }

        assertEquals(mapOf("colour" to "yellow", "pinned" to true, "order" to "a0"), merged)
    }

    @Test
    fun concurrentEditsToOneKeyKeepBothValuesAndPickOneWinner() {
        val result = AutomergeDocument().use { phone ->
            phone.transact { it.put(it.root, "colour", "yellow") }
            AutomergeDocument(phone.save()).use { laptop ->
                phone.transact { it.put(it.root, "colour", "sage") }
                laptop.transact { it.put(it.root, "colour", "rose") }

                phone.merge(laptop)
                laptop.merge(phone)
                listOf(phone, laptop).map { document ->
                    document.get(document.root, "colour") to document.getAll(document.root, "colour").toSet()
                }
            }
        }

        val conflict = setOf<AutomergeValue>(AutomergeValue.Str("sage"), AutomergeValue.Str("rose"))
        val winner = result.first().first
        assertEquals(listOf(winner to conflict, winner to conflict), result)
    }

    @Test
    fun concurrentTextEditsMergePerCharacter() {
        val merged = AutomergeDocument().use { phone ->
            phone.transact { it.putText(it.root, "body", "Milk") }
            AutomergeDocument(phone.save()).use { laptop ->
                phone.transact { it.spliceText(it.textId("body"), 0, 0, "Oat ") }
                laptop.transact { it.spliceText(it.textId("body"), 4, 0, " and bread") }

                phone.merge(laptop)
                phone.text(phone.textId("body"))
            }
        }

        assertEquals("Oat Milk and bread", merged)
    }

    @Test
    fun textPositionsCountCodePoints() {
        val text = AutomergeDocument().use { document ->
            document.transact { tx ->
                // The emoji is one code point and two UTF-16 units.
                val body = tx.putText(tx.root, "body", "a😀b")
                tx.spliceText(body, 2, 0, "X")
                tx.spliceText(body, 0, 1, "")
            }
            document.text(document.textId("body"))
        }

        assertEquals("😀Xb", text)
    }

    @Test
    fun marksCoverTheirRange() {
        val marks = AutomergeDocument().use { document ->
            document.transact { tx ->
                val body = tx.putText(tx.root, "body", "😀 Buy milk today")
                tx.mark(body, 2, 10, "bold", true, ExpandMark.AFTER)
                tx.mark(body, 0, 15, "heading", 1, ExpandMark.NONE)
                tx.mark(body, 2, 5, "italic", true, ExpandMark.AFTER)
                tx.unmark(body, 2, 5, "italic", ExpandMark.AFTER)
            }
            AutomergeDocument(document.save()).use { it.marks(it.textId("body")).sortedBy(Mark::name) }
        }

        assertEquals(
            listOf(
                Mark(2, 10, "bold", AutomergeValue.Bool(true)),
                Mark(0, 15, "heading", AutomergeValue.Integer(1)),
            ),
            marks,
        )
    }

    @Test
    fun headsAreEqualAfterMergeInBothDirections() {
        val heads = AutomergeDocument().use { phone ->
            phone.transact { it.put(it.root, "colour", "yellow") }
            AutomergeDocument(phone.save()).use { laptop ->
                val before = phone.heads()
                phone.transact { it.put(it.root, "pinned", true) }
                laptop.transact { it.put(it.root, "order", "a0") }

                phone.merge(laptop)
                laptop.merge(phone)
                Triple(phone.heads() == laptop.heads(), phone.heads().size, phone.heads() != before)
            }
        }

        assertEquals(Triple(true, 2, true), heads)
    }

    @Test
    fun documentKeepsTheGivenActor() {
        val actors = AutomergeDocument(actorId = ACTOR).use { created ->
            AutomergeDocument(created.save(), actorId = OTHER_ACTOR).use { loaded -> created.actorId to loaded.actorId }
        }

        assertEquals(ACTOR to OTHER_ACTOR, actors)
    }

    @Test
    fun documentsWithoutAnActorGetDifferentActors() {
        val first = AutomergeDocument().use { it.actorId }
        val second = AutomergeDocument().use { it.actorId }

        assertNotEquals(first, second)
    }

    @Test
    fun changeAtPastHeadsReadsTheOldTextAndKeepsTheLaterChange() {
        val result = AutomergeDocument(actorId = ACTOR).use { phone ->
            phone.transact { it.putText(it.root, "body", "abcdef") }
            val base = phone.heads()
            AutomergeDocument(phone.save(), actorId = OTHER_ACTOR).use { laptop ->
                laptop.transact { it.spliceText(it.textId("body"), 3, 0, "X") }
                phone.merge(laptop)
            }

            var read = ""
            val version = phone.transactAt(base) { tx ->
                val body = tx.textId("body")
                read = tx.text(body)
                tx.spliceText(body, 6, 0, "g")
            }
            Triple(read, phone.text(phone.textId("body")), version.size == 1 && version != base)
        }

        assertEquals(Triple("abcdef", "abcXdefg", true), result)
    }

    @Test
    fun changeAtPastHeadsFollowsALaterChangeOfTheSameActor() {
        val result = AutomergeDocument(actorId = ACTOR).use { phone ->
            phone.transact { it.putText(it.root, "body", "abcdef") }
            val base = phone.heads()
            phone.transact { it.put(it.root, "pinned", true) }

            val version = phone.transactAt(base) { it.spliceText(it.textId("body"), 6, 0, "g") }
            phone.transactAt(version) { it.spliceText(it.textId("body"), 7, 0, "h") }
            // A copy that holds every change loads, changes and merges in both directions.
            AutomergeDocument(phone.save(), actorId = OTHER_ACTOR).use { copy ->
                copy.transact { it.put(it.root, "colour", "yellow") }
                phone.merge(copy)
                copy.merge(phone)
                Triple(phone.snapshot(), copy.snapshot(), phone.heads() == copy.heads())
            }
        }

        val content = mapOf("body" to TextSnapshot("abcdefgh"), "pinned" to true, "colour" to "yellow")
        assertEquals(Triple(content, content, true), result)
    }

    @Test
    fun manyChangesAtPastHeadsAllStay() {
        val saved = AutomergeDocument(actorId = ACTOR).use { phone ->
            phone.transact { it.putText(it.root, "body", "a") }
            var version = phone.heads()
            AutomergeDocument(phone.save(), actorId = OTHER_ACTOR).use { laptop ->
                laptop.transact { it.spliceText(it.textId("body"), 0, 0, "X") }
                phone.merge(laptop)
            }

            // Each change follows the one before, and none of them holds the change of the laptop.
            repeat(10) { index ->
                version = phone.transactAt(version) { it.spliceText(it.textId("body"), index + 1, 0, index.toString()) }
            }
            phone.save()
        }

        assertEquals(mapOf("body" to TextSnapshot("Xa0123456789")), AutomergeDocument(saved).use { it.snapshot() })
    }

    @Test
    fun unknownHeadsFail() {
        AutomergeDocument().use { document ->
            assertFailsWith<AutomergeException> {
                document.transactAt(listOf("00".repeat(32))) { it.put(it.root, "pinned", true) }
            }
        }
    }

    // Two copies of one document must never write with one actor: both changes get the same
    // sequence number. This test records what Automerge then does.
    @Test
    fun twoChangesWithOneActorAndOneSequenceNumber() {
        val bytes = AutomergeDocument(actorId = ACTOR).use { document ->
            document.transact { it.put(it.root, "colour", "yellow") }
            document.save()
        }
        val kept = AutomergeDocument(bytes, actorId = ACTOR).use { first ->
            AutomergeDocument(bytes, actorId = ACTOR).use { second ->
                first.transact { it.put(it.root, "title", "First") }
                second.transact { it.put(it.root, "pinned", true) }

                // Both bindings report "duplicate seq 2 found for actor ..." and merge nothing.
                assertFailsWith<AutomergeException> { first.merge(second) }
                first.snapshot()
            }
        }

        assertEquals(mapOf("colour" to "yellow", "title" to "First"), kept)
    }

    @Test
    fun invalidBytesFailToLoad() {
        assertFailsWith<AutomergeException> { AutomergeDocument(byteArrayOf(1, 2, 3)).use { } }
    }

    @Test
    fun invalidActorFails() {
        assertFailsWith<AutomergeException> { AutomergeDocument(actorId = "not hex").use { } }
    }

    private companion object {
        const val ACTOR = "0123456789abcdef0123456789abcdef"
        const val OTHER_ACTOR = "fedcba9876543210fedcba9876543210"
    }
}
