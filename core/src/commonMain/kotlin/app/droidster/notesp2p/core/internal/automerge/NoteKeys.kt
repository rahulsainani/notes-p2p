package app.droidster.notesp2p.core.internal.automerge

import app.droidster.notesp2p.core.common.firstOrDefault
import app.droidster.notesp2p.core.note.NoteColour
import app.droidster.notesp2p.core.note.NoteKind
import app.droidster.notesp2p.core.note.TextFormat

/**
 * The keys and values that a note document stores in Automerge: the file format of a note. The
 * schema is in `docs/NoteModel.md`, section "The document".
 *
 * Every device and every older file uses these names, so a stored key or value is an identifier that
 * never changes. A code name follows the domain words of `docs/Glossary.md`; the stored name stays,
 * e.g. [TRASHED_AT] is stored as `deletedAt`. The rules are in `docs/SchemaChanges.md`.
 * The format versions of the document are in [DocAccess].
 */
internal object NoteKeys {
    const val TITLE = "title"
    const val BODY = "body"
    const val KIND = "kind"
    const val ITEMS = "items"
    const val COLOUR = "colour"
    const val PINNED = "pinned"
    const val ARCHIVED = "archived"
    const val ORDER = "order"
    const val LABELS = "labels"
    const val CHECKED_ITEMS_HIDDEN = "checkedItemsHidden"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val TRASHED_AT = "deletedAt"
    const val RESTORED = "restored"
    const val ITEM_TEXT = "text"
    const val ITEM_CHECKED = "checked"
    const val ITEM_ORDER = "order"

    const val MARK_HEADING = "heading"
    val headingLevels = mapOf(TextFormat.HEADING_1 to 1L, TextFormat.HEADING_2 to 2L)
    val inlineMarks = mapOf(
        TextFormat.BOLD to "bold",
        TextFormat.ITALIC to "italic",
        TextFormat.UNDERLINE to "underline",
        TextFormat.STRIKETHROUGH to "strikethrough",
    )

    // Each enum value has a stored ID of its own, never derived from the Kotlin name, so a rename in code
    // does not change the format (docs/SchemaChanges.md). A stored ID that this app version does not
    // know reads as the default; the value in the document stays until someone sets a new one.

    /**
     * Returns the stored ID of [kind]. Stored IDs never change.
     */
    fun stored(kind: NoteKind): String = when (kind) {
        NoteKind.TEXT -> "text"
        NoteKind.CHECKLIST -> "checklist"
    }

    /**
     * Returns the stored ID of [colour]. Stored IDs never change; a new colour gets a new ID. Ten IDs
     * come from the first colour set and keep their meaning under the new names, e.g. "butter" is
     * [NoteColour.LEMON].
     */
    @Suppress("CyclomaticComplexMethod") // One branch per colour: an exhaustive list, not logic.
    fun stored(colour: NoteColour): String = when (colour) {
        NoteColour.OAT -> "oat"
        NoteColour.LEMON -> "butter"
        NoteColour.TANGERINE -> "apricot"
        NoteColour.TOMATO -> "tomato"
        NoteColour.PINK -> "rose"
        NoteColour.GRAPE -> "plum"
        NoteColour.IRIS -> "iris"
        NoteColour.SKY -> "sky"
        NoteColour.TEAL -> "moss"
        NoteColour.MINT -> "mint"
        NoteColour.LIME -> "lime"
        NoteColour.COCOA -> "cocoa"
        NoteColour.SLATE -> "stone"
        NoteColour.SAGE -> "sage"
        NoteColour.BRICK -> "clay"
    }

    /**
     * Returns the kind with the stored ID [stored], or [NoteKind.TEXT] when this app version does not
     * know it.
     */
    fun kindOf(stored: String?): NoteKind = NoteKind.entries.firstOrDefault(NoteKind.TEXT) { stored(it) == stored }

    /**
     * Returns the colour with the stored ID [stored], or [NoteColour.OAT] when this app version does not
     * know it. The fallback is a fixed colour, not [NoteColour.DEFAULT]: a later change of the default
     * must not change how a stored note looks.
     */
    fun colourOf(stored: String?): NoteColour =
        NoteColour.entries.firstOrDefault(NoteColour.OAT) { stored(it) == stored }
}
