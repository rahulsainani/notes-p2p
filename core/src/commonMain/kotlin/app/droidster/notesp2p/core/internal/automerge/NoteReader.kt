package app.droidster.notesp2p.core.internal.automerge

import app.droidster.automerge.AutomergeReader
import app.droidster.automerge.AutomergeValue
import app.droidster.automerge.Mark
import app.droidster.automerge.ObjId
import app.droidster.automerge.ObjType
import app.droidster.notesp2p.core.document.DocVersion
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ARCHIVED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.BODY
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.CHECKED_ITEMS_HIDDEN
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.COLOUR
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.CREATED_AT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ITEMS
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ITEM_CHECKED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ITEM_ORDER
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ITEM_TEXT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.KIND
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.LABELS
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.MARK_HEADING
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ORDER
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.PINNED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.RESTORED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.TITLE
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.TRASHED_AT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.UPDATED_AT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.headingLevels
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.inlineMarks
import app.droidster.notesp2p.core.internal.isValidId
import app.droidster.notesp2p.core.internal.order.FractionalIndex
import app.droidster.notesp2p.core.internal.order.Ordered
import app.droidster.notesp2p.core.internal.order.orderComparator
import app.droidster.notesp2p.core.note.ChecklistItem
import app.droidster.notesp2p.core.note.FormatSpan
import app.droidster.notesp2p.core.note.FormattedText
import app.droidster.notesp2p.core.note.ItemId
import app.droidster.notesp2p.core.note.Note
import app.droidster.notesp2p.core.note.NoteContent
import app.droidster.notesp2p.core.note.NoteId
import app.droidster.notesp2p.core.note.NoteKind
import app.droidster.notesp2p.core.note.NoteStatus
import app.droidster.notesp2p.core.note.OrderKey
import app.droidster.notesp2p.core.text.utf16Index
import app.droidster.notesp2p.core.vault.LabelId
import kotlin.time.Instant

/**
 * Reads a [Note] from a note document, with the fallbacks of `docs/NoteModel.md` for missing values.
 */
internal class NoteReader(private val reader: AutomergeReader) {

    /**
     * Returns the note [id] as it is at [version], the version of the document that this reader reads.
     */
    fun note(id: NoteId, version: DocVersion): Note {
        val root = reader.root
        // Two devices that edit at the same time both set updatedAt; the newest time is the true one.
        val updatedAt = reader.getAll(root, UPDATED_AT).filterIsInstance<AutomergeValue.Integer>()
            .maxOfOrNull { it.value }
        // A switch of kind moves the content, so the other kind is empty. It holds content only when
        // another device added some at the same time as the switch; that content shows after a switch back.
        val content = when (NoteKeys.kindOf(reader.getString(root, KIND))) {
            NoteKind.TEXT -> NoteContent.Text(formattedText(root, BODY))

            NoteKind.CHECKLIST -> NoteContent.Checklist(
                items(),
                reader.getBoolean(root, CHECKED_ITEMS_HIDDEN) ?: false,
            )
        }
        val isArchived = reader.getBoolean(root, ARCHIVED) ?: false
        // The Trash wins: a trashed note keeps its archive state for a restore.
        val status = instant(root, TRASHED_AT)?.let { NoteStatus.Trashed(since = it, fromArchive = isArchived) }
            ?: if (isArchived) NoteStatus.Archived else NoteStatus.Active
        return Note(
            id = id,
            title = text(root, TITLE),
            content = content,
            colour = NoteKeys.colourOf(reader.getString(root, COLOUR)),
            isPinned = reader.getBoolean(root, PINNED) ?: false,
            status = status,
            order = order(root, ORDER),
            labelIds = keys(LABELS).filter(::isValidId).map(::LabelId).toSet(),
            createdAt = instant(root, CREATED_AT) ?: Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt ?: 0),
            isReadOnly = DocAccess.of(reader) != DocAccess.READ_WRITE,
            version = version,
        )
    }

    /**
     * Returns the trash times that a restore undid.
     */
    fun restored(): Set<Instant> =
        keys(RESTORED).mapNotNull(String::toLongOrNull).map(Instant::fromEpochMilliseconds).toSet()

    /**
     * Returns every item of the document in display order, also the items that a text note hides.
     */
    fun items(): List<ChecklistItem> {
        val items = reader.getObject(reader.root, ITEMS, ObjType.MAP) ?: return emptyList()
        // A key that is not an ID comes from another program; the item stays out of the model.
        return reader.keys(items).filter(::isValidId)
            .mapNotNull { id ->
                val item = reader.getObject(items, id, ObjType.MAP) ?: return@mapNotNull null
                ChecklistItem(
                    ItemId(id),
                    formattedText(item, ITEM_TEXT),
                    reader.getBoolean(item, ITEM_CHECKED) ?: false,
                    order(item, ITEM_ORDER),
                )
            }
            .sortedWith(compareBy(orderComparator) { Ordered.of(it.id.value, it.order) })
    }

    /**
     * Returns the keys of the map at the root key [key]; none when it is missing.
     */
    fun keys(key: String): List<String> = reader.getObject(reader.root, key, ObjType.MAP)?.let(reader::keys).orEmpty()

    private fun formattedText(obj: ObjId, key: String): FormattedText {
        val textObject = reader.getObject(obj, key, ObjType.TEXT) ?: return FormattedText()
        val text = reader.text(textObject)
        val spans = reader.marks(textObject).mapNotNull { it.toSpan(text) }
        return FormattedText(text, spans.sortedWith(compareBy({ it.start }, { it.format })))
    }

    private fun instant(obj: ObjId, key: String): Instant? =
        reader.getLong(obj, key)?.let(Instant::fromEpochMilliseconds)

    private fun text(obj: ObjId, key: String): String =
        reader.getObject(obj, key, ObjType.TEXT)?.let(reader::text).orEmpty()

    private fun order(obj: ObjId, key: String): OrderKey =
        OrderKey(reader.getString(obj, key)?.takeIf(FractionalIndex::isValid) ?: FractionalIndex.FIRST)

    private fun Mark.toSpan(text: String): FormatSpan? {
        val format = when (name) {
            MARK_HEADING -> headingLevels.entries.firstOrNull { value == AutomergeValue.Integer(it.value) }?.key
            else -> inlineMarks.entries.firstOrNull { it.value == name && value == AutomergeValue.Bool(true) }?.key
        } ?: return null
        val from = text.utf16Index(start)
        val until = text.utf16Index(end)
        // An empty mark formats no text, so the model has no span for it.
        return if (from < until) FormatSpan(from, until, format) else null
    }
}
