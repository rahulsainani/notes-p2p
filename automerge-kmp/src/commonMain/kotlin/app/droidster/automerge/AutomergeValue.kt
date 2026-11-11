package app.droidster.automerge

/**
 * The ID of a map, list or text object inside one document.
 */
public class ObjId internal constructor(internal val platform: Any)

/**
 * The type of object in a document.
 */
public enum class ObjType { MAP, LIST, TEXT }

/**
 * Whether a mark grows when text is inserted at its start or end.
 */
public enum class ExpandMark { BEFORE, AFTER, BOTH, NONE }

/**
 * A value on the code points [start] until [end] (exclusive) of a text object.
 */
public data class Mark(val start: Int, val end: Int, val name: String, val value: AutomergeValue)

/**
 * A value in a map of a document.
 */
public sealed interface AutomergeValue {
    /**
     * A string.
     */
    public data class Str(val value: String) : AutomergeValue

    /**
     * A signed 64-bit integer.
     */
    public data class Integer(val value: Long) : AutomergeValue

    /**
     * A Boolean.
     */
    public data class Bool(val value: Boolean) : AutomergeValue

    /**
     * A `null` value.
     */
    public data object Null : AutomergeValue

    /**
     * A map, list or text object, by its [id].
     */
    public data class Obj(val id: ObjId, val type: ObjType) : AutomergeValue

    /**
     * A type this API does not expose, e.g. a counter or bytes.
     */
    public data object Other : AutomergeValue
}
