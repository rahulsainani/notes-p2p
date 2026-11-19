package app.droidster.notesp2p.core.internal.automerge

import app.droidster.automerge.AutomergeDocument
import app.droidster.automerge.ExpandMark
import app.droidster.automerge.ObjType

// Format fixtures: one stored note document per format version, as Base64. A
// fixture proves that later app versions still read the documents that older ones wrote; the rules
// are in docs/SchemaChanges.md, section "Format fixtures".
//
// Never change or remove an entry after the first release. A new format version gets a new entry:
// FormatFixtureTest fails while DocAccess.SUPPORTED_VERSION has none, and prints the line to add.

/**
 * The note fixture of each format version (key: the format version).
 */
internal val noteFixtures: Map<Long, String> = mapOf(
    1L to base64(
        """
        hW9Kg6Qs0I0AigUBEAoLDA0ODwABAgMEBQYHCAkBb2C1cx1OPOMLK0n7n6JDWtrQKuRcHVjkX84x
        KA1NEMkGAQIDAhMCIwJAAlYCDgEEAhMREhMhFfYBIQIjMDQHQiFWLFdFgAEClAEDpQEMfwB/AX84
        fwB/AH8HABAoAAAQCQMGDQIVAxYKFwMkBSV+MTcAEQgAAAEFAAAHCAAABAQAAAIAEH4ABAcBfnUO
        BAEABX1uABgCAX8ABAEAA35iJgMBAAJwCGFyY2hpdmVkBGJvZHkSY2hlY2tlZEl0ZW1zSGlkZGVu
        BmNvbG91cgljcmVhdGVkQXQJZGVsZXRlZEF0BWl0ZW1zBGtpbmQGbGFiZWxzEG1pblJlYWRlclZl
        cnNpb24QbWluV3JpdGVyVmVyc2lvbgVvcmRlcgZwaW5uZWQIcmVzdG9yZWQFdGl0bGUJdXBkYXRl
        ZEF0AA97Cml0ZW0tYnJlYWQJaXRlbS1taWxrB2NoZWNrZWQFb3JkZXIEdGV4dAAKfQdjaGVja2Vk
        BW9yZGVyBHRleHQABX4MbGFiZWwtdHJhdmVsDTE3NjI2NTAwMDAwMDA4AG8vXiZ6BwJffx1QAS5+
        CUwyTwgBfwIFAXkRcgwBdAl4AgF+B3oEAX0MAXkFAX4IBhAPBQoDBQJ+AQQEAX0AAQAEAX4ABBAB
        AgACAX4EBwMBfwcHAX8EBwF8AgACZgJjfQCWAQACFH4mAgIAf2MPFgIAfAImAAIDFn8ABRZ9ASYA
        BRYCAmJ1dHRlcoCoifujM4D27cimM2NoZWNrbGlzdAEBYTWAtJaZpjNHcm9jZXJpZXNIaWRkZW5h
        MU9hdCBtaWxrYTBCcmVhZDgAKAEPACR/Bml0YWxpYwATAA==
        """,
    ),
)

/**
 * Returns the Base64 text of a fixture written over several lines, without the line breaks.
 */
internal fun base64(lines: String): String = lines.filterNot(Char::isWhitespace)

internal const val FIXTURE_ACTOR = "0a0b0c0d0e0f00010203040506070809"

/**
 * Writes a note document of the current format version with every key set, by hand: the fixture
 * must not depend on [NoteDocument], the code it checks.
 */
internal fun createNoteFixture(): ByteArray = AutomergeDocument(FIXTURE_ACTOR).use { document ->
    document.transact { tx ->
        val root = tx.root
        tx.put(root, "minReaderVersion", DocAccess.SUPPORTED_VERSION)
        tx.put(root, "minWriterVersion", DocAccess.SUPPORTED_VERSION)
        tx.spliceText(tx.putObject(root, "title", ObjType.TEXT), 0, 0, "Groceries")
        tx.spliceText(tx.putObject(root, "body", ObjType.TEXT), 0, 0, "Hidden")
        tx.put(root, "kind", "checklist")
        val items = tx.putObject(root, "items", ObjType.MAP)
        val milk = tx.putObject(items, "item-milk", ObjType.MAP)
        val milkText = tx.putObject(milk, "text", ObjType.TEXT)
        tx.spliceText(milkText, 0, 0, "Oat milk")
        tx.mark(milkText, 0, 3, "italic", true, ExpandMark.AFTER)
        tx.put(milk, "checked", true)
        tx.put(milk, "order", "a1")
        val bread = tx.putObject(items, "item-bread", ObjType.MAP)
        tx.spliceText(tx.putObject(bread, "text", ObjType.TEXT), 0, 0, "Bread")
        tx.put(bread, "checked", false)
        tx.put(bread, "order", "a0")
        tx.put(root, "colour", "butter")
        tx.put(root, "pinned", true)
        tx.put(root, "archived", true)
        tx.put(root, "order", "a5")
        tx.put(tx.putObject(root, "labels", ObjType.MAP), "label-travel", true)
        tx.put(root, "checkedItemsHidden", true)
        tx.put(root, "createdAt", 1_762_000_000_000L)
        tx.put(root, "updatedAt", 1_762_600_000_000L)
        tx.put(root, "deletedAt", 1_762_700_000_000L)
        tx.put(tx.putObject(root, "restored", ObjType.MAP), "1762650000000", true)
    }
    document.save()
}
