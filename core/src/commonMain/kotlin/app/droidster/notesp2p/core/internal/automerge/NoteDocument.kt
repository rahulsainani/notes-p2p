package app.droidster.notesp2p.core.internal.automerge

import app.droidster.automerge.AutomergeDocument
import app.droidster.automerge.ObjType
import app.droidster.notesp2p.core.document.DocVersion
import app.droidster.notesp2p.core.identity.ActorId
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ARCHIVED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.BODY
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.CHECKED_ITEMS_HIDDEN
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.COLOUR
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.CREATED_AT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ITEMS
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.KIND
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.LABELS
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.ORDER
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.PINNED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.RESTORED
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.TITLE
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.TRASHED_AT
import app.droidster.notesp2p.core.internal.automerge.NoteKeys.UPDATED_AT
import app.droidster.notesp2p.core.note.NoteColour
import app.droidster.notesp2p.core.note.NoteId
import app.droidster.notesp2p.core.note.NoteKind
import app.droidster.notesp2p.core.note.OrderKey
import kotlin.time.Instant

/**
 * A note as an Automerge document. The format is in `docs/NoteModel.md`.
 */
internal class NoteDocument private constructor(private val document: AutomergeDocument) : AutoCloseable {

    fun version(): DocVersion = DocVersion.of(document.heads())

    /**
     * Returns what this app version may do with the document.
     */
    fun access(): DocAccess = DocAccess.of(document)

    fun save(): ByteArray = document.save()

    fun merge(other: NoteDocument) = document.merge(other.document)

    /**
     * Returns the note [id]. Call it only when [access] is not [DocAccess.UNREADABLE].
     */
    fun read(id: NoteId): NoteWithRestores {
        check(access() != DocAccess.UNREADABLE) { "Note ${id.value} needs a newer app version to read" }
        val reader = NoteReader(document)
        return NoteWithRestores(reader.note(id, version()), reader.restored())
    }

    override fun close() = document.close()

    companion object {
        fun create(actorId: ActorId, now: Instant, order: OrderKey, kind: NoteKind, colour: NoteColour): NoteDocument {
            val document = AutomergeDocument(actorId.value)
            document.transact { tx ->
                tx.writeSupportedVersion()
                tx.putObject(tx.root, TITLE, ObjType.TEXT)
                tx.putObject(tx.root, BODY, ObjType.TEXT)
                tx.put(tx.root, KIND, NoteKeys.stored(kind))
                tx.putObject(tx.root, ITEMS, ObjType.MAP)
                tx.put(tx.root, COLOUR, NoteKeys.stored(colour))
                tx.put(tx.root, PINNED, false)
                tx.put(tx.root, ARCHIVED, false)
                tx.put(tx.root, ORDER, order.value)
                tx.putObject(tx.root, LABELS, ObjType.MAP)
                tx.put(tx.root, CHECKED_ITEMS_HIDDEN, false)
                tx.put(tx.root, CREATED_AT, now.toEpochMilliseconds())
                tx.put(tx.root, UPDATED_AT, now.toEpochMilliseconds())
                tx.putNull(tx.root, TRASHED_AT)
                tx.putObject(tx.root, RESTORED, ObjType.MAP)
            }
            return NoteDocument(document)
        }

        /**
         * [actorId] is null for a document that is only read or merged; it then gets no new change.
         *
         * @throws app.droidster.automerge.AutomergeException when [bytes] is not a document.
         */
        fun load(bytes: ByteArray, actorId: ActorId?): NoteDocument =
            NoteDocument(AutomergeDocument(bytes, actorId?.value))
    }
}
