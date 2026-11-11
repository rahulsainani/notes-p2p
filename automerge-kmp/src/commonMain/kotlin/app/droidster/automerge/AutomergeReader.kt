package app.droidster.automerge

/**
 * Reads of one document state. Text positions count Unicode code points, not UTF-16 units.
 */
public interface AutomergeReader {
    /**
     * The root map of the document.
     */
    public val root: ObjId

    /**
     * The value at [key] in the map [obj], or null if the key is missing.
     */
    public fun get(obj: ObjId, key: String): AutomergeValue?

    /**
     * Every concurrent value at [key]: one entry after a clean merge, several after a conflict.
     */
    public fun getAll(obj: ObjId, key: String): List<AutomergeValue>

    /**
     * The keys of the map [obj].
     */
    public fun keys(obj: ObjId): List<String>

    /**
     * The content of the text object [obj].
     */
    public fun text(obj: ObjId): String

    /**
     * The marks on the text object [obj].
     */
    public fun marks(obj: ObjId): List<Mark>

    /**
     * Returns the string at [key] in the map [obj], or `null` when the key is missing or holds another type.
     */
    public fun getString(obj: ObjId, key: String): String? = (get(obj, key) as? AutomergeValue.Str)?.value

    /**
     * Returns the integer at [key] in the map [obj], or `null` when the key is missing or holds another type.
     */
    public fun getLong(obj: ObjId, key: String): Long? = (get(obj, key) as? AutomergeValue.Integer)?.value

    /**
     * Returns the Boolean at [key] in the map [obj], or `null` when the key is missing or holds another type.
     */
    public fun getBoolean(obj: ObjId, key: String): Boolean? = (get(obj, key) as? AutomergeValue.Bool)?.value

    /**
     * Returns the ID of the object of [type] at [key] in the map [obj], or `null` when the key is
     * missing or holds another value.
     */
    public fun getObject(obj: ObjId, key: String, type: ObjType): ObjId? =
        (get(obj, key) as? AutomergeValue.Obj)?.takeIf { it.type == type }?.id
}
