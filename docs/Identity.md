# Identity

No login exists. The app makes its own identity on its first run on a device (`IdentityLoader.loadOrCreate`
in the package `core.identity`). In these docs a device is one installation of the app: after a
reinstall, the phone is a new device.

| Value       | What it is                               | Who sees it                                                        |
|-------------|------------------------------------------|--------------------------------------------------------------------|
| Device ID   | An Ed25519 public key, 64 hex characters | Other devices, e.g. as the file name `<device-id>.automerge`       |
| Private key | The other half of the key pair           | Nobody. No code signs with it                                      |
| Actor ID    | A random UUID as 32 hex characters       | Inside Automerge only: it is recorded as the author of each change |

## Why two IDs

- The device ID is public. It is a full public key, not a short form: a short form could collide,
  and a key can later prove which device wrote a file. The ID cannot change later, so the key pair
  is made and stored now.
- The actor ID must be unique per device. Automerge numbers the changes of each actor. Two devices
  with one actor ID give one number to two different changes, and a merge of their documents then
  fails ("duplicate seq").

## The identity file

One file of 80 bytes holds the identity: the private key (32 bytes), the public key (32 bytes), then
the actor ID (16 bytes). The public key is stored, not derived from the private key: the desktop JDK
cannot derive an Ed25519 public key without BouncyCastle. Example paths:

| Caller      | File                                 |
|-------------|--------------------------------------|
| CLI         | `~/.notes-p2p/identity/identity.bin` |
| Android app | `<filesDir>/identity/identity.bin`   |

- The first run writes the file to a temp file, then moves it into place in one step. A crash
  leaves no file or a whole one, never a part of one.
- A file of another size is damaged. Loading it fails with `DamagedIdentityException`, and the file
  stays as it is: a new identity would make the device a stranger to every other device.
- One `IdentityLoader` lets only one call at a time read or make the file. When two processes make
  an identity at the same time, the last move wins, and both read that file back.

## Never in a backup

The identity file belongs in app-private storage that no backup and no device transfer includes.

- A backup restored onto a new phone, while the old phone still runs the app, gives two devices with
  one device ID and one actor ID.
- A reinstall therefore makes a new identity. The notes come back from the other devices. The old
  device's files stay in the sync folder and are read on every first pass of a new device; nothing
  removes them.

## Key library

`cryptography-kotlin` gives one API with a provider per platform:

| Platform    | Provider                       | Note                                                        |
|-------------|--------------------------------|-------------------------------------------------------------|
| Desktop JVM | JDK                            | Ed25519 needs JDK 15 or later                               |
| Android     | JDK provider with BouncyCastle | The library's docs name BouncyCastle for Ed25519 on Android |
| iOS         | CryptoKit                      |                                                             |

The private key is a plain file in that app-private storage.
