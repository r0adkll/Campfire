package app.campfire.widgets

interface WidgetUpdater {

  /**
   * Update any homescreen widgets with the lastest information from the app.
   */
  suspend fun updatePlayerWidget()
}
