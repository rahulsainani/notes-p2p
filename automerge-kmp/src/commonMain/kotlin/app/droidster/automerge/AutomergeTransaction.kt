package app.droidster.automerge

/**
 * Writes inside [AutomergeDocument.transact]. Reads see the writes made so far.
 */
public interface AutomergeTransaction : AutomergeReader {
    /**
     * Puts [value] at [key] in the map [obj].
     */
    public fun put(obj: ObjId, key: String, value: String)

    /**
     * Puts [value] at [key] in the map [obj]. On JVM and Android, [value] cannot be below
     * [Int.MIN_VALUE] (a limit of automerge-java 0.0.9).
     */
    public fun put(obj: ObjId, key: String, value: Long)

    /**
     * Puts [value] at [key] in the map [obj].
     */
    public fun put(obj: ObjId, key: String, value: Boolean)

    /**
     * Puts `null` at [key] in the map [obj].
     */
    public fun putNull(obj: ObjId, key: String)

    /**
     * Puts a new, empty object at [key] and returns its ID.
     */
    public fun putObject(obj: ObjId, key: String, type: ObjType): ObjId

    /**
     * Removes [key] from the map [obj].
     */
    public fun delete(obj: ObjId, key: String)

    /**
     * Removes [deleteCount] code points at [start] in the text [obj], then inserts [text] there.
     */
    public fun spliceText(obj: ObjId, start: Int, deleteCount: Int, text: String)

    /**
     * Marks the range [start] until [end] (exclusive) of the text [obj] with [name] = [value].
     */
    @Suppress("LongParameterList") // The arguments of Automerge's own mark call, in its order.
    public fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Boolean, expand: ExpandMark)

    /**
     * Marks the range [start] until [end] (exclusive) of the text [obj] with [name] = [value].
     */
    @Suppress("LongParameterList") // The arguments of Automerge's own mark call, in its order.
    public fun mark(obj: ObjId, start: Int, end: Int, name: String, value: Long, expand: ExpandMark)

    /**
     * Removes the mark [name] from the range [start] until [end] (exclusive).
     */
    public fun unmark(obj: ObjId, start: Int, end: Int, name: String, expand: ExpandMark)
}
