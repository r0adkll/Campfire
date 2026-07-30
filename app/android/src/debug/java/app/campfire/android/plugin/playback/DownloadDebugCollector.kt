// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Records media3 [DownloadManager.Listener] callbacks for the Downloads tab's event
 * log. Pure media3 — attached to whatever [DownloadManager] the wiring hands it
 * (see LivewireInitializer), with no knowledge of the app's download layer.
 */
object DownloadDebugCollector : DownloadManager.Listener {

  data class DownloadEvent(
    val timeMs: Long,
    val type: String,
    val details: String,
  )

  private val _events = MutableStateFlow<List<DownloadEvent>>(emptyList())
  val events: StateFlow<List<DownloadEvent>> = _events.asStateFlow()

  private var attached: DownloadManager? = null

  fun attach(downloadManager: DownloadManager) {
    if (attached === downloadManager) return
    attached?.removeListener(this)
    attached = downloadManager
    downloadManager.addListener(this)
  }

  fun clear() {
    _events.value = emptyList()
  }

  override fun onInitialized(downloadManager: DownloadManager) {
    record("initialized", "downloads=${downloadManager.currentDownloads.size}")
  }

  override fun onDownloadsPausedChanged(downloadManager: DownloadManager, downloadsPaused: Boolean) {
    record("pausedChanged", "downloadsPaused=$downloadsPaused")
  }

  override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
    val failure = finalException?.let { " (${it::class.simpleName}: ${it.message})" }.orEmpty()
    record("downloadChanged", "${download.request.id} -> ${download.state.asDownloadStateName()}$failure")
  }

  override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
    record("downloadRemoved", download.request.id)
  }

  override fun onIdle(downloadManager: DownloadManager) {
    record("idle", "all downloads finished or paused")
  }

  override fun onRequirementsStateChanged(
    downloadManager: DownloadManager,
    requirements: Requirements,
    notMetRequirements: Int,
  ) {
    record("requirements", "notMetRequirements=$notMetRequirements")
  }

  override fun onWaitingForRequirementsChanged(downloadManager: DownloadManager, waitingForRequirements: Boolean) {
    record("waitingForRequirements", waitingForRequirements.toString())
  }

  private fun record(type: String, details: String) {
    _events.update { current ->
      val event = DownloadEvent(System.currentTimeMillis(), type, details)
      (listOf(event) + current).take(MAX_EVENTS)
    }
  }

  private const val MAX_EVENTS = 200
}

internal fun Int.asDownloadStateName(): String = when (this) {
  Download.STATE_QUEUED -> "QUEUED"
  Download.STATE_STOPPED -> "STOPPED"
  Download.STATE_DOWNLOADING -> "DOWNLOADING"
  Download.STATE_COMPLETED -> "COMPLETED"
  Download.STATE_FAILED -> "FAILED"
  Download.STATE_REMOVING -> "REMOVING"
  Download.STATE_RESTARTING -> "RESTARTING"
  else -> "UNKNOWN($this)"
}
