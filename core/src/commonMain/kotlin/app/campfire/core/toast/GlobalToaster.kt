package app.campfire.core.toast

/**
 * An easy to use global singleton
 */
object GlobalToaster : Toast {

  /*
   * 🐲 WARNING!
   *
   * Using a singleton like this can be prone to memory leaks if you are not careful to unregister this delegate
   * when its underlying reference (i.e. Activities, etc) are destroyed.
   */
  private var delegate: Toast? = null

  override fun show(
    message: String,
    duration: Toast.Duration,
  ): ToastHandle {
    return delegate?.show(message, duration) ?: ToastHandle { }
  }

  fun register(delegate: Toast) {
    this.delegate = delegate
  }

  fun unregister() {
    this.delegate = null
  }
}
