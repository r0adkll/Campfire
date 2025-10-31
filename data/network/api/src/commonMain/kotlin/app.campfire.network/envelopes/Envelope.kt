package app.campfire.network.envelopes

import app.campfire.network.RequestOrigin
import app.campfire.network.models.NetworkModel

/**
 * Base model for all API response envelopes
 */
abstract class Envelope : NetworkModel() {

  /**
   * This must be overridden to populate the [RequestOrigin] throughout the containing [NetworkModel]
   * in this envelope. This is necessary for parts of our setup that use this field to relate what [serverUrl]
   * the request came from.
   */
  protected abstract fun applyPostage()

  /**
   * Set the [origin] for this [NetworkModel] and then call [applyPostage] so that the envelope model
   * call apply it's [origin] to its children
   */
  override fun applyOrigin(origin: RequestOrigin) {
    // Must be called to apply the [origin] var
    super.applyOrigin(origin)
    applyPostage()
  }
}
