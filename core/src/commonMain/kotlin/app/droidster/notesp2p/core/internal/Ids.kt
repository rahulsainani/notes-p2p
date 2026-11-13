package app.droidster.notesp2p.core.internal

// One type per kind of ID, so a label ID cannot go where a note ID belongs. An ID is random hex,
// except the ID of the default vault. Every ID is safe as a file name, as IDs name files and come
// from other devices.

private val idPattern = Regex("[A-Za-z0-9_-]{1,128}")

/**
 * Returns `true` when [value] can be an ID: 1 to 128 of `A-Z`, `a-z`, `0-9`, `_`, `-`.
 */
internal fun isValidId(value: String): Boolean = idPattern.matches(value)

/**
 * @throws IllegalArgumentException when [value] cannot be an ID ([isValidId]).
 */
internal fun requireId(value: String) = require(isValidId(value)) { "Invalid ID: $value" }
