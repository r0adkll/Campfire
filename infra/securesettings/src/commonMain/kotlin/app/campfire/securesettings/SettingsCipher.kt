// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.securesettings

/**
 * Authenticated encryption for [EncryptedSettings] values. [associatedData] binds a ciphertext to
 * the key it was stored under, so an entry can't be decrypted after being copied to another key.
 */
interface SettingsCipher {

  fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray

  /** Returns null when [ciphertext] can't be authenticated, e.g. the key behind it is gone. */
  fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray?
}
