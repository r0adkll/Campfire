package app.campfire.android

import android.app.Application
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.LogPriority.DEBUG
import app.campfire.core.logging.LogPriority.ERROR
import app.campfire.core.logging.LogPriority.INFO
import app.campfire.core.logging.LogPriority.VERBOSE
import app.campfire.core.logging.LogPriority.WARN
import app.campfire.core.logging.bark
import app.campfire.crashreporting.CrashReporter
import app.campfire.crashreporting.impl.FirebaseCrashReporter
import app.campfire.settings.api.CampfireSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class FirebaseInitializer(
  private val application: Application,
  private val settings: CampfireSettings,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AppInitializer {

  private val firebaseScope = CoroutineScope(SupervisorJob() + applicationScope.coroutineContext) +
    CoroutineExceptionHandler { _, t ->
      bark("Firebase", ERROR, throwable = t) { "Something when wrong in the Firebase Application Initializer Scope" }
    }

  override val priority: Int = AppInitializer.FIREBASE_INIT_PRIORITY

  override suspend fun onInitialize() {
    // ONLY initialize firebase on release builds
    FirebaseApp.initializeApp(application)

    // Setup logging
    Heartwood.grow(FirebaseBark)

    // Setup Crash Reporting
    CrashReporter.Delegator += FirebaseCrashReporter

    // Start the observer for firebase crash reporting setting to enable/disable
    observeFirebaseSetting()
  }

  private fun observeFirebaseSetting() {
    firebaseScope.launch {
      settings.observeCrashReportingEnabled().collect { enabled ->
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (enabled && !crashlytics.isCrashlyticsCollectionEnabled) {
          crashlytics.isCrashlyticsCollectionEnabled = true
        } else if (!enabled && crashlytics.isCrashlyticsCollectionEnabled) {
          crashlytics.isCrashlyticsCollectionEnabled = false
        }
      }
    }
  }
}

object FirebaseBark : Heartwood.Bark {

  private val crashlytics by lazy {
    FirebaseCrashlytics.getInstance()
  }

  override fun log(
    priority: LogPriority,
    tag: String?,
    extras: Extras?,
    message: () -> String,
  ) {
    if (priority == VERBOSE) return

    crashlytics.log(
      buildString {
        // Tag
        if (tag != null) {
          append("[$tag] ")
        }

        // Priority
        append(
          when (priority) {
            DEBUG -> "D: "
            INFO -> "I: "
            WARN -> "W: "
            ERROR -> "E: "
          },
        )

        append(message())
      },
    )
  }
}
