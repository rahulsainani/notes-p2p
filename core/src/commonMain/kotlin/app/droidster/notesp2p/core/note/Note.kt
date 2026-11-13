package app.droidster.notesp2p.core.note

import app.droidster.notesp2p.core.document.DocVersion
import app.droidster.notesp2p.core.internal.requireId
import app.droidster.notesp2p.core.vault.LabelId
import app.droidster.notesp2p.core.vault.Vault
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/**
 * A note: a title with a text or a checklist.
 *
 * @property id the ID of the note, unique in its vault.
 * @property title the title; empty when the note has none.
 * @property content the text or the checklist below the title.
 * @property colour the colour name; each theme picks its own colour value.
 * @property isPinned `true` when the note shows above the notes that are not pinned.
 * @property status the list that shows the note: the notes, the Archive or the Trash.
 * @property order the position among all notes of the vault. Archive and Trash do not change it.
 * @property labelIds the labels of the note. An ID can name a label that the vault no longer has; a
 * client shows only the labels in [Vault.labels].
 * @property createdAt the time the note was made.
 * @property updatedAt the time of the last change to the title or the content.
 * @property isReadOnly `true` when a newer app version wrote the note. This version shows the note
 * but does not change it.
 * @property version the version of the note that this object shows.
 */
public data class Note(
    val id: NoteId,
    val title: String = "",
    val content: NoteContent = NoteContent.Text(),
    val colour: NoteColour = NoteColour.DEFAULT,
    val isPinned: Boolean = false,
    val status: NoteStatus = NoteStatus.Active,
    val order: OrderKey,
    val labelIds: Set<LabelId> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val isReadOnly: Boolean = false,
    val version: DocVersion,
) {
    /**
     * The kind of [content].
     */
    val kind: NoteKind
        get() = when (content) {
            is NoteContent.Text -> NoteKind.TEXT
            is NoteContent.Checklist -> NoteKind.CHECKLIST
        }
}

/**
 * @throws IllegalArgumentException when [value] is not 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`.
 */
@JvmInline
public value class NoteId(public val value: String) {
    init {
        requireId(value)
    }
}

/**
 * What a [Note] holds below its title.
 */
public sealed interface NoteContent {
    /**
     * A text with formats.
     */
    public data class Text(val body: FormattedText = FormattedText()) : NoteContent

    /**
     * @property items the items in display order.
     * @property checkedItemsHidden `true` when the checked items are not shown.
     */
    public data class Checklist(
        val items: List<ChecklistItem> = emptyList(),
        val checkedItemsHidden: Boolean = false,
    ) : NoteContent
}

/**
 * The kind of content of a [Note].
 */
public enum class NoteKind { TEXT, CHECKLIST }

/**
 * The list that shows a [Note].
 */
public sealed interface NoteStatus {
    /**
     * In the notes list.
     */
    public data object Active : NoteStatus

    /**
     * In the Archive.
     */
    public data object Archived : NoteStatus

    /**
     * In the Trash. Pin, colour, labels and position stay for a restore.
     *
     * @property since the time the note moved to the Trash.
     * @property fromArchive `true` when the note was in the Archive; a restore puts it back there.
     */
    public data class Trashed(val since: Instant, val fromArchive: Boolean) : NoteStatus
}

/**
 * The colour of a [Note], in the order of the colour sheet. It is a name, not a colour value: each
 * theme picks its own values. A new note starts in [DEFAULT].
 */
public enum class NoteColour {
    OAT,
    LEMON,
    TANGERINE,
    TOMATO,
    PINK,
    GRAPE,
    IRIS,
    SKY,
    TEAL,
    MINT,
    LIME,
    COCOA,
    SLATE,
    SAGE,
    BRICK,
    ;

    public companion object {
        /**
         * The colour of a new note, and of a note whose stored colour this app version does not know.
         */
        public val DEFAULT: NoteColour = OAT
    }
}

/**
 * @throws IllegalArgumentException when [value] is not 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`.
 */
@JvmInline
public value class ItemId(public val value: String) {
    init {
        requireId(value)
    }
}

/**
 * One item of a [NoteContent.Checklist].
 */
public data class ChecklistItem(
    val id: ItemId,
    val text: FormattedText = FormattedText(),
    val checked: Boolean = false,
    val order: OrderKey,
)

/**
 * A [text] with formats on ranges of it. Positions count UTF-16 units, as [String] does.
 *
 * @throws IllegalArgumentException when a span is empty or goes past the end of [text].
 */
public data class FormattedText(val text: String = "", val spans: List<FormatSpan> = emptyList()) {
    init {
        spans.forEach { require(it.start in 0..<it.end && it.end <= text.length) { "Span out of the text: $it" } }
    }
}

/**
 * A format on a range of a text.
 */
public enum class TextFormat { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, HEADING_1, HEADING_2 }

/**
 * A [format] on the UTF-16 units from [start] until [end] (exclusive) of a [FormattedText].
 */
public data class FormatSpan(val start: Int, val end: Int, val format: TextFormat)
