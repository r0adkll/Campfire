package app.campfire.ui.settings.auto

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidAutoWrapper(
  private val application: Application,
) : AndroidAuto {

  private val androidAutoPackageName = "com.google.android.projection.gearhead"
  private val playStorePackageName = "com.android.vending"

  /**
   * Checks if the Android Auto application is installed on the phone.
   *
   * @return True if Android Auto is installed, false otherwise.
   */
  override fun isAvailable(): Boolean {
    return isAppInstalledFromPlayStore(application, androidAutoPackageName)
  }

  private fun isAppInstalledFromPlayStore(context: Context, packageName: String): Boolean {
    // We should just ASSUME Android Auto is available and instead check if application
    // has been installed from the PlayStore. If not the user will need special instruction to
    // enable Android Auto in the app from un-official stores.
    val packageManager: PackageManager = context.packageManager
    return try {
      val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
      val installerPackageName = installSourceInfo.installingPackageName
      playStorePackageName == installerPackageName
    } catch (e: PackageManager.NameNotFoundException) {
      // The package is not installed at all
      false
    } catch (e: SecurityException) {
      // May occur if the caller doesn't have permission to query install source,
      // though less common for checking one's own app or common apps if <queries> is set up.
      // Log this or handle as needed; for simplicity, returning false.
      // Consider logging e.printStackTrace() for debugging.
      false
    }
  }

  override fun openSettings() {
    // Intent to open Android Auto specific settings (most reliable for settings)
    // This intent action is commonly used to go directly to Android Auto's settings page.
    val settingsIntent = Intent("com.google.android.projection.gearhead.SETTINGS")
    settingsIntent.setPackage(androidAutoPackageName) // Target Android Auto specifically
    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    application.startActivity(settingsIntent)
  }
}
