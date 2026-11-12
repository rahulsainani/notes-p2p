package app.droidster.notesp2p.core.identity

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EdDSA
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Loads the [Identity] of this device from [file], or makes it on the first run.
 *
 * The file holds the Ed25519 private key (32 bytes), the public key (32 bytes) and the actor ID
 * (16 bytes). The public key is stored, not derived from the private key: the desktop JDK cannot
 * derive it without BouncyCastle.
 *
 * @param file a file in app-private storage that no backup includes.
 * @param ioDispatcher the dispatcher for the file access.
 */
public class IdentityLoader(
    private val file: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileSystem: FileSystem = SystemFileSystem,
) {

    private val mutex = Mutex()
    private var loaded: Identity? = null

    /**
     * Returns the identity stored in [file]. Makes and stores a new one when [file] does not exist.
     *
     * @throws DamagedIdentityException when [file] exists but does not hold an identity. A new
     * identity would make this device a stranger to every other device, so the app must not make
     * one without asking.
     */
    public suspend fun loadOrCreate(): Identity = mutex.withLock {
        loaded ?: withContext(ioDispatcher) { load() }.also { loaded = it }
    }

    /**
     * Reads the identity from [file], or makes a new one when [file] does not exist.
     */
    private suspend fun load(): Identity {
        val bytes = if (fileSystem.exists(file)) {
            read()
        } else {
            create()
        }
        if (bytes.size != IDENTITY_BYTES) {
            throw DamagedIdentityException(file)
        }

        return Identity(
            deviceId = DeviceId(bytes.copyOfRange(PUBLIC_KEY_START, ACTOR_START).toHexString()),
            actorId = ActorId(bytes.copyOfRange(ACTOR_START, IDENTITY_BYTES).toHexString()),
        )
    }

    /**
     * Returns all bytes of [file].
     */
    private fun read(): ByteArray = fileSystem.source(file).buffered().use { it.readByteArray() }

    /**
     * Makes a new identity, writes it to [file], and returns the bytes that [file] then holds.
     *
     * The file is read back because of a case that [mutex] does not cover: two processes that make
     * an identity at the same time. A mutex works only inside one process.
     *
     * Example: on a new computer, two CLI commands start at the same moment, as two processes. Each
     * one makes its own identity and writes it, and the second write replaces the first. Both
     * commands read the file back, so both use the identity that stays on disk.
     */
    private suspend fun create(): ByteArray {
        write(generateIdentity())
        return read()
    }

    /**
     * Makes a new key pair and a random actor ID, and returns them in the layout of the file.
     */
    private suspend fun generateIdentity(): ByteArray {
        val keyPair = CryptographyProvider.Default.get(EdDSA).keyPairGenerator(EdDSA.Curve.Ed25519).generateKey()
        val privateKey = keyPair.privateKey.encodeToByteArray(EdDSA.PrivateKey.Format.RAW)
        val publicKey = keyPair.publicKey.encodeToByteArray(EdDSA.PublicKey.Format.RAW)
        val actorId = Uuid.random().toByteArray()
        return privateKey + publicKey + actorId
    }

    /**
     * Writes [bytes] to a temp file, then moves the temp file to [file].
     *
     * If the app crashes during the write, only the temp file is left, and the next start is a first
     * run again. The move is one step, so [file] is never half written. Each call uses its own temp
     * name, so two processes never write into the same temp file.
     */
    private fun write(bytes: ByteArray) {
        file.parent?.let(fileSystem::createDirectories)
        val temp = Path("$file.${Uuid.random()}.tmp")
        fileSystem.sink(temp).buffered().use { it.write(bytes) }
        fileSystem.atomicMove(temp, file)
    }

    /**
     * The layout of the file: the private key in bytes 0 to 31, the public key in bytes 32 to 63, and
     * the actor ID in bytes 64 to 79.
     */
    private companion object {
        const val PUBLIC_KEY_START = 32
        const val ACTOR_START = 64
        const val IDENTITY_BYTES = 80
    }
}

/**
 * The identity file exists but does not hold an identity: it has the wrong size.
 */
public class DamagedIdentityException(file: Path) : IllegalStateException("Identity file is damaged: $file")
