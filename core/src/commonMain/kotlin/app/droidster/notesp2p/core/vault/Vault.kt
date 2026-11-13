package app.droidster.notesp2p.core.vault

import app.droidster.notesp2p.core.internal.requireId
import app.droidster.notesp2p.core.note.Note
import kotlin.jvm.JvmInline

/**
 * A vault: a closed set of notes with its own labels.
 *
 * @property labels the labels, sorted by name.
 * @property isReadOnly `true` when a newer app version wrote the vault. This version then changes no
 * name or label.
 */
public data class Vault(
    val id: VaultId,
    val name: String = "",
    val labels: List<Label> = emptyList(),
    val isReadOnly: Boolean = false,
)

/**
 * @throws IllegalArgumentException when [value] is not 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`.
 */
@JvmInline
public value class VaultId(public val value: String) {
    init {
        requireId(value)
    }

    public companion object {
        /**
         * The vault that the app has on every device from the start. Its ID is the same everywhere,
         * so devices that sync share one default vault.
         */
        public val DEFAULT: VaultId = VaultId("default")
    }
}

/**
 * @throws IllegalArgumentException when [value] is not 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`.
 */
@JvmInline
public value class LabelId(public val value: String) {
    init {
        requireId(value)
    }
}

/**
 * A label of a vault. A note refers to it by [id].
 */
public data class Label(val id: LabelId, val name: String)

/**
 * A [vault] and its [notes] in display order, Archive and Trash included.
 */
public data class VaultSnapshot(val vault: Vault, val notes: List<Note> = emptyList())
