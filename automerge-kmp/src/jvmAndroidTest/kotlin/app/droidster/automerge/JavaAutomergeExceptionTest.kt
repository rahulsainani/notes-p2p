package app.droidster.automerge

import kotlin.test.Test
import kotlin.test.assertTrue

class JavaAutomergeExceptionTest {

    @Test
    fun wrappedExceptionClassStillExists() {
        val exists = runCatching { Class.forName(JAVA_AUTOMERGE_EXCEPTION) }.isSuccess

        assertTrue(exists, "automerge-java no longer has $JAVA_AUTOMERGE_EXCEPTION. Update wrapAutomergeErrors.")
    }
}
