package app.droidster.notesp2p.core.identity

import kotlin.jvm.JvmInline

/**
 * The identity of this device. The app makes it on its first run, and it never leaves the device.
 * After a reinstallation, the app makes a new identity.
 *
 * The identity must be left out of every backup. A backup restored onto a second device would give
 * both devices the same identity, and with it the same [actorId].
 *
 * @property deviceId the public key that other devices see.
 * @property actorId the Automerge actor of every change that this device makes. Each device needs its
 * own actor ID. Automerge keeps the changes of each actor as one numbered chain (1, 2, 3, ...), and
 * each change builds on the one before it. Two devices with one actor ID both make a change 5 with
 * different content; a merge then has two changes for one place in the chain, and Automerge refuses
 * it ("duplicate seq") instead of dropping one of the edits.
 */
public data class Identity(val deviceId: DeviceId, val actorId: ActorId)

/**
 * The Ed25519 public key of a device, as lowercase hex.
 *
 * @throws IllegalArgumentException when [value] is not 64 lowercase hex digits (32 bytes).
 */
@JvmInline
public value class DeviceId(public val value: String) {
    init {
        require(isLowercaseHex(value, DEVICE_ID_BYTES)) { "Device ID is not 32 bytes of lowercase hex: $value" }
    }
}

/**
 * The Automerge actor ID of a device, as lowercase hex.
 *
 * @throws IllegalArgumentException when [value] is not 32 lowercase hex digits (16 bytes).
 */
@JvmInline
public value class ActorId(public val value: String) {
    init {
        require(isLowercaseHex(value, ACTOR_ID_BYTES)) { "Actor ID is not 16 bytes of lowercase hex: $value" }
    }
}

private const val DEVICE_ID_BYTES = 32
private const val ACTOR_ID_BYTES = 16

private fun isLowercaseHex(value: String, bytes: Int): Boolean =
    value.length == bytes * 2 && value.all { it in '0'..'9' || it in 'a'..'f' }
