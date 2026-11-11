package app.droidster.automerge

/**
 * The text and marks of one text object.
 */
internal data class TextSnapshot(val text: String, val marks: List<Mark> = emptyList())

/**
 * The whole content under [obj] as plain Kotlin values, so a test compares one expected object.
 * Maps become `Map<String, Any?>`, text objects become [TextSnapshot], scalars keep their value.
 */
internal fun AutomergeReader.snapshot(obj: ObjId = root): Map<String, Any?> = keys(obj).associateWith { key ->
    when (val value = get(obj, key)) {
        is AutomergeValue.Str -> value.value

        is AutomergeValue.Integer -> value.value

        is AutomergeValue.Bool -> value.value

        is AutomergeValue.Obj -> when (value.type) {
            ObjType.MAP -> snapshot(value.id)
            ObjType.TEXT -> TextSnapshot(text(value.id), marks(value.id).sortedBy { it.name })
            ObjType.LIST -> error("Lists are not in use")
        }

        AutomergeValue.Null, null -> null

        AutomergeValue.Other -> error("Unexpected value type at $key")
    }
}

/**
 * Puts a text object with [content] at [key] and returns its ID.
 */
internal fun AutomergeTransaction.putText(obj: ObjId, key: String, content: String): ObjId =
    putObject(obj, key, ObjType.TEXT).also { spliceText(it, 0, 0, content) }

internal fun AutomergeReader.textId(key: String): ObjId = (get(root, key) as AutomergeValue.Obj).id
