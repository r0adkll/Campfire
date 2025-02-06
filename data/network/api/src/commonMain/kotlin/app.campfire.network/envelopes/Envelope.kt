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
  abstract fun applyPostage()
}
