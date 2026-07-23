package app.campfire.android.updates

import app.campfire.core.di.AppScope
import app.campfire.updates.source.AppUpdateSource
import app.campfire.updates.source.FakeAppUpdateSource
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/**
 * Debug builds use the developer-settings driven [FakeAppUpdateSource] so the app
 * update widget and flows can be tested from the Developer settings pane.
 */
@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])
@Inject
class DebugAppUpdateSource(
  private val fake: FakeAppUpdateSource,
) : AppUpdateSource by fake
