package app.campfire.crashreporting.impl

import app.campfire.crashreporting.CrashReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FirebaseCrashReporter : CrashReporter {

  override fun tag(key: String, tag: String) {
    FirebaseCrashlytics.getInstance().setCustomKey(key, tag)
  }

  override fun record(t: Throwable) {
    FirebaseCrashlytics.getInstance().recordException(t)
  }
}
