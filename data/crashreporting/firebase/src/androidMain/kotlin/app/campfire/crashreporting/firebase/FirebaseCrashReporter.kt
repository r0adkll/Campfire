// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.crashreporting.firebase

import app.campfire.crashreporting.CrashReporter
import app.campfire.crashreporting.impl.redactedCopyOrSelf
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FirebaseCrashReporter : CrashReporter {

  override fun tag(key: String, tag: String) {
    FirebaseCrashlytics.getInstance().setCustomKey(key, tag)
  }

  override fun record(t: Throwable) {
    FirebaseCrashlytics.getInstance().recordException(t.redactedCopyOrSelf())
  }
}
