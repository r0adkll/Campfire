// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.securesettings

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test

class EncryptedSettingsTest {

  private val backing = MapSettings()
  private val cipher = XorCipher()
  private val settings = EncryptedSettings(cipher) { backing }

  @Test
  fun `round trips every type`() {
    settings.putString("string", "token")
    settings.putBoolean("boolean", true)
    settings.putInt("int", 42)
    settings.putLong("long", Long.MAX_VALUE)
    settings.putFloat("float", 1.5f)
    settings.putDouble("double", 2.25)

    assertThat(settings.getStringOrNull("string")).isEqualTo("token")
    assertThat(settings.getBooleanOrNull("boolean")).isEqualTo(true)
    assertThat(settings.getIntOrNull("int")).isEqualTo(42)
    assertThat(settings.getLongOrNull("long")).isEqualTo(Long.MAX_VALUE)
    assertThat(settings.getFloatOrNull("float")).isEqualTo(1.5f)
    assertThat(settings.getDoubleOrNull("double")).isEqualTo(2.25)
  }

  @Test
  fun `stores ciphertext rather than the value`() {
    settings.putString("accessToken", "secret")

    assertThat(backing.getStringOrNull("accessToken")).isNotEqualTo("secret")
  }

  @Test
  fun `missing keys fall back to defaults`() {
    assertThat(settings.getStringOrNull("missing")).isNull()
    assertThat(settings.getString("missing", "default")).isEqualTo("default")
    assertThat(settings.getBoolean("missing", true)).isTrue()
    assertThat(settings.getInt("missing", 7)).isEqualTo(7)
    assertThat(settings.getLong("missing", 8L)).isEqualTo(8L)
    assertThat(settings.getFloat("missing", 9f)).isEqualTo(9f)
    assertThat(settings.getDouble("missing", 10.0)).isEqualTo(10.0)
  }

  @Test
  fun `value that no longer decrypts reads as absent`() {
    settings.putString("accessToken", "secret")
    cipher.failDecryption = true

    assertThat(settings.getStringOrNull("accessToken")).isNull()
    assertThat(settings.getString("accessToken", "default")).isEqualTo("default")
  }

  @Test
  fun `value moved to another key reads as absent`() {
    settings.putString("accessToken_a", "secret")
    backing.putString("accessToken_b", backing.getString("accessToken_a", ""))

    assertThat(settings.getStringOrNull("accessToken_b")).isNull()
  }

  @Test
  fun `value that is not base64 reads as absent`() {
    backing.putString("accessToken", "not base64!")

    assertThat(settings.getStringOrNull("accessToken")).isNull()
  }

  @Test
  fun `wrong type reads as absent`() {
    settings.putString("flag", "not a boolean")

    assertThat(settings.getBooleanOrNull("flag")).isNull()
    assertThat(settings.getIntOrNull("flag")).isNull()
  }

  @Test
  fun `key operations pass through`() {
    settings.putString("a", "1")
    settings.putString("b", "2")

    assertThat(settings.keys).containsOnly("a", "b")
    assertThat(settings.size).isEqualTo(2)
    assertThat(settings.hasKey("a")).isTrue()

    settings.remove("a")
    assertThat(settings.hasKey("a")).isFalse()

    settings.clear()
    assertThat(settings.size).isEqualTo(0)
  }

  @Test
  fun `delegate is resolved on first use`() {
    var resolved = 0
    val lazySettings = EncryptedSettings(cipher) {
      resolved++
      backing
    }
    assertThat(resolved).isEqualTo(0)

    lazySettings.putString("a", "1")
    lazySettings.getStringOrNull("a")
    assertThat(resolved).isEqualTo(1)
  }

  @Test
  fun `imports legacy entries with their types`() {
    settings.importEntries(
      mapOf(
        "accessToken_user" to "access",
        "hardcoverTokenInvalid_user" to true,
        "int" to 1,
        "long" to 2L,
        "float" to 3f,
        "double" to 4.0,
        "set" to setOf("ignored"),
        "null" to null,
      ),
    )

    assertThat(settings.getStringOrNull("accessToken_user")).isEqualTo("access")
    assertThat(settings.getBooleanOrNull("hardcoverTokenInvalid_user")).isEqualTo(true)
    assertThat(settings.getIntOrNull("int")).isEqualTo(1)
    assertThat(settings.getLongOrNull("long")).isEqualTo(2L)
    assertThat(settings.getFloatOrNull("float")).isEqualTo(3f)
    assertThat(settings.getDoubleOrNull("double")).isEqualTo(4.0)
    assertThat(settings.keys).containsOnly(
      "accessToken_user",
      "hardcoverTokenInvalid_user",
      "int",
      "long",
      "float",
      "double",
    )
  }
}

/** Reversible stand-in for an AEAD: XORs with the associated data and prefixes it as the "tag". */
private class XorCipher : SettingsCipher {

  var failDecryption = false

  override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
    byteArrayOf(associatedData.size.toByte()) + associatedData + xor(plaintext, associatedData)

  override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray? {
    if (failDecryption) return null
    val tagSize = ciphertext.first().toInt()
    val tag = ciphertext.copyOfRange(1, 1 + tagSize)
    if (!tag.contentEquals(associatedData)) return null
    return xor(ciphertext.copyOfRange(1 + tagSize, ciphertext.size), associatedData)
  }

  private fun xor(data: ByteArray, key: ByteArray): ByteArray =
    ByteArray(data.size) { i -> (data[i].toInt() xor (key[i % key.size].toInt() + 1)).toByte() }
}
