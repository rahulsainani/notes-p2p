package app.droidster.notesp2p.core.identity

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray

class IdentityLoaderTest {
    private val directory = Path(SystemTemporaryDirectory, "notes-p2p-test-${Random.nextLong()}")

    @Test
    fun firstRunMakesAnIdentityAndLaterRunsKeepIt() = runTest {
        val file = Path(directory, "identity")

        val first = IdentityLoader(file, StandardTestDispatcher(testScheduler)).loadOrCreate()
        val second = IdentityLoader(file, StandardTestDispatcher(testScheduler)).loadOrCreate()

        assertEquals(first, second)
    }

    @Test
    fun eachDeviceGetsItsOwnIdentity() = runTest {
        val first = IdentityLoader(Path(directory, "phone"), StandardTestDispatcher(testScheduler)).loadOrCreate()
        val second = IdentityLoader(Path(directory, "laptop"), StandardTestDispatcher(testScheduler)).loadOrCreate()

        assertNotEquals(first, second)
    }

    @Test
    fun twoCallsAtOnceGiveOneIdentity() = runTest {
        val file = Path(directory, "identity")
        val loader = IdentityLoader(file, StandardTestDispatcher(testScheduler))

        val calls = listOf(async { loader.loadOrCreate() }, async { loader.loadOrCreate() })
        val results = calls.map { it.await() }
        val stored = IdentityLoader(file, StandardTestDispatcher(testScheduler)).loadOrCreate()

        assertEquals(listOf(stored, stored), results)
    }

    @Test
    fun damagedFileIsNotReplaced() = runTest {
        val file = Path(directory, "identity")
        SystemFileSystem.createDirectories(directory)
        SystemFileSystem.sink(file).buffered().use { it.write(byteArrayOf(1, 2, 3)) }

        assertFailsWith<DamagedIdentityException> {
            IdentityLoader(file, StandardTestDispatcher(testScheduler)).loadOrCreate()
        }
        assertEquals(
            listOf<Byte>(1, 2, 3),
            SystemFileSystem.source(file).buffered().use {
                it.readByteArray()
            }.toList(),
        )
    }
}
