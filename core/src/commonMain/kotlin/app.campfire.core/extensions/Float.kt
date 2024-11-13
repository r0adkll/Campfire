package app.campfire.core.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val Float.seconds: Duration
  get() = toDouble().seconds
