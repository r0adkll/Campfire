// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.securesettings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a non-exportable key held by the Android Keystore under [alias]. Each
 * ciphertext is the Keystore-generated 12-byte IV followed by the sealed value and its tag.
 *
 * The key never leaves the device, so anything it encrypted is unreadable after a restore to
 * another one — keep these stores out of backups.
 */
internal class KeystoreSettingsCipher(
  private val alias: String,
) : SettingsCipher {

  private val key: SecretKey by lazy { loadOrCreateKey() }

  override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    cipher.updateAAD(associatedData)
    return cipher.iv + cipher.doFinal(plaintext)
  }

  override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray? {
    if (ciphertext.size < IV_SIZE) return null
    return try {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, ciphertext, 0, IV_SIZE))
      cipher.updateAAD(associatedData)
      cipher.doFinal(ciphertext, IV_SIZE, ciphertext.size - IV_SIZE)
    } catch (_: GeneralSecurityException) {
      null
    }
  }

  private fun loadOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

    val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(KEY_SIZE_BITS)
      .build()
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      .apply { init(spec) }
      .generateKey()
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_SIZE_BITS = 256
    const val TAG_SIZE_BITS = 128
    const val IV_SIZE = 12
  }
}
