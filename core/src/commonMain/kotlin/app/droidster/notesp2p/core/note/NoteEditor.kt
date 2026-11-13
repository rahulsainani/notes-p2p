package app.droidster.notesp2p.core.note

import app.droidster.notesp2p.core.vault.LabelId

/**
 * The changes that one edit can make to a note. Positions count UTF-16 units, as [String] does.
 *
 * A text setter takes the whole new text and stores only the changed range, so a merge keeps what
 * another device typed elsewhere in the same text.
 */
@Suppress("TooManyFunctions") // One function per kind of change, so one edit can make any of them.
public interface NoteEditor {
    /**
     * The note with the changes of this edit so far. Its [Note.version] is the version that the edit runs on.
     */
    public val note: Note

    /**
     * Sets the title to [text].
     */
    public fun setTitle(text: String)

    /**
     * Sets the body to [text]. Formats on the unchanged parts stay.
     */
    public fun setBody(text: String)

    /**
     * Sets [format] on the body from [start] until [end] (exclusive) when [on] is `true`, or clears it
     * when [on] is `false`. A heading replaces the other heading level; clearing one level leaves the
     * other level as it is.
     *
     * A format is a range, a heading too: it can cover several lines. Text typed at its end gets the
     * format, a new line included, until the format is cleared there.
     */
    public fun setFormat(start: Int, end: Int, format: TextFormat, on: Boolean)

    /**
     * Switches between a text note and a checklist. To a checklist: each body line that is not blank
     * becomes an item, with its formats except headings. To text: the items join into lines, with
     * their formats; the checked state is lost.
     */
    public fun setKind(kind: NoteKind)

    /**
     * Adds an item after [afterId], or at the end when [afterId] is `null`, and returns its ID.
     */
    public fun addItem(text: String = "", afterId: ItemId? = null, checked: Boolean = false): ItemId

    /**
     * Sets the text of the item [id] to [text]. Formats on the unchanged parts stay.
     */
    public fun setItemText(id: ItemId, text: String)

    /**
     * Sets or clears [format] on the text of the item [id], as [setFormat] does on the body. A heading
     * is not a format of an item: [TextFormat.HEADING_1] and [TextFormat.HEADING_2] change nothing.
     */
    public fun setItemFormat(id: ItemId, start: Int, end: Int, format: TextFormat, on: Boolean)

    /**
     * Checks or unchecks the item [id].
     */
    public fun setItemChecked(id: ItemId, checked: Boolean)

    /**
     * Moves the item [id] directly after [afterId], or to the start when [afterId] is `null`.
     */
    public fun moveItem(id: ItemId, afterId: ItemId?)

    /**
     * Deletes the item [id].
     */
    public fun deleteItem(id: ItemId)

    /**
     * Unchecks every checked item.
     */
    public fun uncheckAll()

    /**
     * Deletes every checked item.
     */
    public fun deleteCheckedItems()

    /**
     * Sets the colour.
     */
    public fun setColour(colour: NoteColour)

    /**
     * Pins or unpins the note.
     */
    public fun setPinned(isPinned: Boolean)

    /**
     * Moves the note to the Archive or back. In the Trash, it sets where a [restore] puts the note.
     */
    public fun setArchived(isArchived: Boolean)

    /**
     * Sets the position among the notes of the vault.
     */
    public fun setOrder(order: OrderKey)

    /**
     * Adds the label [labelId].
     */
    public fun addLabel(labelId: LabelId)

    /**
     * Removes the label [labelId].
     */
    public fun removeLabel(labelId: LabelId)

    /**
     * Hides or shows the checked items of a checklist.
     */
    public fun setCheckedItemsHidden(hidden: Boolean)

    /**
     * Moves the note to the Trash. Colour, pin, labels and position stay for a later [restore].
     */
    public fun trash()

    /**
     * Takes the note out of the Trash. A purge made on another device at the same time does not apply.
     */
    public fun restore()
}
