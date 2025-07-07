package app.campfire.widgets

import kotlin.time.Duration

interface WidgetUpdater {

  /**
   * Update any homescreen widgets with the lastest information from the app.
   */
  suspend fun updatePlayerWidget(
    currentTime: Duration? = null,
    currentDuration: Duration? = null,
    playbackSpeed: Float? = null,
  )
}
