package app.droidster.notesp2p.core.internal.automerge

import app.droidster.notesp2p.core.note.Note
import app.droidster.notesp2p.core.note.NoteStatus
import kotlin.time.Instant

/**
 * A [note] as its document stores it, with the trash times that a restore undid.
 *
 * @property note the note.
 * @property restored the [NoteStatus.Trashed.since] values that a restore undid. A purge of only these
 * values does not apply.
 */
internal data class NoteWithRestores(val note: Note, val restored: Set<Instant> = emptySet())
