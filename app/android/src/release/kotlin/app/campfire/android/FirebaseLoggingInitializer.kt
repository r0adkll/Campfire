package app.campfire.android

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.LogPriority.DEBUG
import app.campfire.core.logging.LogPriority.ERROR
import app.campfire.core.logging.LogPriority.INFO
import app.campfire.core.logging.LogPriority.VERBOSE
import app.campfire.core.logging.LogPriority.WARN
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class FirebaseLoggingInitializer : AppInitializer {

  override val priority: Int = AppInitializer.HIGHEST_PRIORITY

  override suspend fun onInitialize() {
    Heartwood.grow(FirebaseBark)
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
    message: String,
  ) {
    crashlytics.log(
      buildString {
        // Tag
        if (tag != null) {
          append("[$tag] ")
        }

        // Priority
        append(
          when (priority) {
            VERBOSE -> "V: "
            DEBUG -> "D: "
            INFO -> "I: "
            WARN -> "W: "
            ERROR -> "E: "
          },
        )

        append(message)
      },
    )
  }
}
