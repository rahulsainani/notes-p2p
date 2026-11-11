package app.droidster.automerge

/**
 * A failure of Automerge, e.g. bytes that are not a document.
 */
public class AutomergeException(message: String, cause: Throwable? = null) : Exception(message, cause)
