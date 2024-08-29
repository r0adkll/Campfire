package app.campfire.account.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import me.tatarka.inject.annotations.Provides

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @TokenSettings
  fun provideTokenSettings(
    application: Application,
  ): Settings = SharedPreferencesSettings(
    delegate = createEncryptedSharedPreferences(application),
  )
}

internal fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
  val masterKey: MasterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

  return EncryptedSharedPreferences.create(
    context,
    "token_shared_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )
}
