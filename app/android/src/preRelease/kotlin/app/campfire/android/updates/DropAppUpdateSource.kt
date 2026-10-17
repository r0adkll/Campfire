// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import android.app.Application
import androidx.compose.runtime.Composable
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSheet
import app.campfire.updates.source.AppUpdateSource
import com.r0adkll.drop.Drop
import com.r0adkll.drop.DropManifest
import com.r0adkll.drop.DropState
import com.r0adkll.drop.Update
import com.r0adkll.drop.UpdateCheckResult
import com.r0adkll.drop.ui.DropUpdateSheet
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/**
 * [AppUpdateSource] for the Drop-distributed alpha and beta builds: checks the build's Drop
 * Channel for a newer build and hands the download and install to Drop's own Update sheet,
 * which also covers the install permission, reinstalls and switching to main after a pull
 * request closes.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])
@Inject
class DropAppUpdateSource(
  private val application: Application,
) : AppUpdateSource {

  // One per process, as the SDK requires; it reads its config from the manifest.
  private val drop by lazy { Drop(application) }

  override val isSupported: Boolean = true

  // Pull request builds are installed to test a change and replaced often, so they check on every
  // foreground; main moves with every merge; beta only moves with a release.
  override val checkInterval: Duration by lazy {
    val config = DropManifest.read(application)
    val channel = config.stamp?.channel ?: config.channel
    when {
      channel.startsWith("pr/") -> Duration.ZERO
      channel == "beta" -> 1.hours
      else -> 15.minutes
    }
  }

  override val updateSheet: AppUpdateSheet = object : AppUpdateSheet {
    @Composable
    override fun Content(onDismiss: () -> Unit) {
      DropUpdateSheet(drop = drop, onDismiss = onDismiss)
    }
  }

  // Compare the mapped update, not Drop's state: each check hands out a fresh download URL, so
  // re-checking on every state change would never settle.
  override fun changes(): Flow<Unit> = drop.state
    .map { it.toAppUpdate() }
    .distinctUntilChanged()
    .drop(1)
    .map { }

  override suspend fun getAvailableUpdate(): AppUpdate? {
    val state = drop.state.value
    // A check never interrupts a download or install, so only ask when there's nothing under way.
    if (state is DropState.Idle || state is DropState.UpdateAvailable) {
      val result = drop.check()
      if (result is UpdateCheckResult.Failed) {
        bark(LogPriority.WARN, throwable = result.cause) { "Drop update check failed: ${result.message}" }
      }
    }
    return drop.state.value.toAppUpdate()
  }

  // Drop's Update sheet drives downloads and installs.
  override suspend fun installUpdate(): Flow<AppUpdateProgress> = emptyFlow()
}

private fun DropState.toAppUpdate(): AppUpdate? = when (this) {
  DropState.Idle -> null
  is DropState.UpdateAvailable -> update.toAppUpdate(
    when {
      result is UpdateCheckResult.ChannelEnded -> AppUpdate.Status.ChannelEnded
      update.requiresReinstall != null -> AppUpdate.Status.ReinstallRequired
      else -> AppUpdate.Status.Available
    },
  )
  is DropState.Downloading -> update.toAppUpdate(AppUpdate.Status.Downloading(progress))
  is DropState.ReadyToInstall -> update.toAppUpdate(AppUpdate.Status.ReadyToInstall)
  is DropState.Installing -> update.toAppUpdate(AppUpdate.Status.Installing)
  is DropState.Failed -> update?.toAppUpdate(AppUpdate.Status.Failed)
}

private fun Update.toAppUpdate(status: AppUpdate.Status) = AppUpdate(
  versionName = versionName ?: label,
  versionCode = versionCode,
  headline = headline,
  status = status,
)
