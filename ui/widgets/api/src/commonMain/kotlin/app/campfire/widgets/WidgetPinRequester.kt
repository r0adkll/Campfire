package app.campfire.widgets

interface WidgetPinRequester {

  /**
   * Request the system UI to show the playback control widget to the user
   * to be pinned on the homescreen. This is currently only working in Android.
   */
  fun requestPinWidget()
}
