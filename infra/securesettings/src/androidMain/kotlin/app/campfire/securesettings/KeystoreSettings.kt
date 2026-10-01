// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.securesettings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * [Settings] backed by the shared preferences file [name], with every value encrypted by a key
 * held in the Android Keystore.
 *
 * When [legacyName] is given, the first access moves the entries of that deprecated
 * `EncryptedSharedPreferences` file into this store and deletes it. A legacy file that can't be
 * opened is deleted too: its entries are unreadable either way.
 */
fun keystoreSettings(
  context: Context,
  name: String,
  legacyName: String? = null,
): Settings {
  val cipher = KeystoreSettingsCipher(alias = "app.campfire.securesettings.$name")
  return EncryptedSettings(cipher) {
    val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    if (legacyName != null) {
      // Commit the copy synchronously so the legacy file is never deleted ahead of it.
      val committing = EncryptedSettings(cipher) { SharedPreferencesSettings(prefs, commit = true) }
      migrateLegacyStore(context, legacyName, committing)
    }
    SharedPreferencesSettings(prefs)
  }
}

private fun migrateLegacyStore(context: Context, legacyName: String, target: Settings) {
  if (!context.legacyStoreExists(legacyName)) return
  val entries = try {
    openLegacyStore(context, legacyName).all
  } catch (e: Exception) {
    bark(TAG, LogPriority.WARN, throwable = e) { "Discarding unreadable legacy store '$legacyName'" }
    emptyMap()
  }
  // A failed import throws before the delete, leaving the legacy store to retry on next launch.
  target.importEntries(entries)
  context.deleteSharedPreferences(legacyName)
  bark(TAG) { "Migrated ${entries.size} entries from '$legacyName'" }
}

private const val TAG = "KeystoreSettings"

private fun Context.legacyStoreExists(name: String): Boolean =
  dataDir.resolve("shared_prefs/$name.xml").exists()

@Suppress("DEPRECATION")
private fun openLegacyStore(context: Context, name: String) = EncryptedSharedPreferences.create(
  context,
  name,
  MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build(),
  EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
  EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)
