package app.droidster.notesp2p.core.document

import kotlin.jvm.JvmInline

/**
 * A state of a document. Two documents with equal versions hold the same content.
 *
 * A version stays valid after the document gets newer changes. Example: an editor opens a note at
 * version A, and a sync then merges a change from another device. `NoteStore.editNote(id, base = A)`
 * runs the edit on the note as it was at A, and Automerge merges it with the newer change. Without
 * the base, a text edit could remove what the other device typed.
 *
 * @property heads the hashes of the newest changes in the document, sorted. Automerge names a state
 * by these hashes: every older change is reachable from them.
 */
@JvmInline
public value class DocVersion private constructor(public val heads: List<String>) {
    public companion object {
        /**
         * Returns the version with the [heads], in any order.
         */
        public fun of(heads: List<String>): DocVersion = DocVersion(heads.distinct().sorted())
    }
}
