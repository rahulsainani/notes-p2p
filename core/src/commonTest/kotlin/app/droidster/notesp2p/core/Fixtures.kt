package app.droidster.notesp2p.core

import app.droidster.notesp2p.core.document.DocVersion
import app.droidster.notesp2p.core.note.ChecklistItem
import app.droidster.notesp2p.core.note.FormatSpan
import app.droidster.notesp2p.core.note.FormattedText
import app.droidster.notesp2p.core.note.ItemId
import app.droidster.notesp2p.core.note.Note
import app.droidster.notesp2p.core.note.NoteColour
import app.droidster.notesp2p.core.note.NoteContent
import app.droidster.notesp2p.core.note.NoteId
import app.droidster.notesp2p.core.note.NoteKind
import app.droidster.notesp2p.core.note.NoteStatus
import app.droidster.notesp2p.core.note.OrderKey
import app.droidster.notesp2p.core.vault.LabelId
import kotlin.time.Instant

// 2019-06-01 20:52 UTC
internal val NOW: Instant = Instant.fromEpochMilliseconds(1_559_422_320_000)

internal fun createNote(
    id: NoteId = NoteId("note"),
    title: String = "",
    body: String = "",
    spans: List<FormatSpan> = emptyList(),
    kind: NoteKind = NoteKind.TEXT,
    items: List<ChecklistItem> = emptyList(),
    colour: NoteColour = NoteColour.DEFAULT,
    isPinned: Boolean = false,
    status: NoteStatus = NoteStatus.Active,
    order: OrderKey = OrderKey("a0"),
    labelIds: Set<LabelId> = emptySet(),
    hidden: Boolean = false,
    createdAt: Instant = NOW,
    updatedAt: Instant = NOW,
    isReadOnly: Boolean = false,
    version: DocVersion = DocVersion.of(emptyList()),
) = Note(
    id = id,
    title = title,
    content = when (kind) {
        NoteKind.TEXT -> NoteContent.Text(FormattedText(body, spans))
        NoteKind.CHECKLIST -> NoteContent.Checklist(items, hidden)
    },
    colour = colour,
    isPinned = isPinned,
    status = status,
    order = order,
    labelIds = labelIds,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isReadOnly = isReadOnly,
    version = version,
)

/**
 * The body text of a text note; empty for a checklist.
 */
internal val Note.body: String get() = (content as? NoteContent.Text)?.body?.text.orEmpty()

/**
 * The body formats of a text note; none for a checklist.
 */
internal val Note.spans: List<FormatSpan> get() = (content as? NoteContent.Text)?.body?.spans.orEmpty()

/**
 * The items of a checklist; none for a text note.
 */
internal val Note.items: List<ChecklistItem> get() = (content as? NoteContent.Checklist)?.items.orEmpty()

internal fun createChecklistItem(
    id: ItemId = ItemId("item"),
    text: String = "",
    spans: List<FormatSpan> = emptyList(),
    checked: Boolean = false,
    order: OrderKey = OrderKey("a0"),
) = ChecklistItem(id, FormattedText(text, spans), checked, order)

/**
 * Replaces what is random in a note, its IDs, order keys and version, by the fixture defaults.
 */
internal fun Note.withFixtureIds() = createNote().let { defaults ->
    copy(
        id = defaults.id,
        order = defaults.order,
        version = defaults.version,
        content = when (content) {
            is NoteContent.Text -> content

            is NoteContent.Checklist -> content.copy(
                items = content.items.map {
                    it.copy(id = createChecklistItem().id, order = createChecklistItem().order)
                },
            )
        },
    )
}
