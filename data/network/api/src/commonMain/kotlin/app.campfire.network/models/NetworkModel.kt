package app.campfire.network.models

import app.campfire.network.RequestOrigin

/**
 * Experimental abstraction to embed network/request information into returned network models so that
 * we can do stuff like trace its origin (serverUrl, etc).
 */
abstract class NetworkModel {

  var origin: RequestOrigin = RequestOrigin.None
    private set

  /**
   * Apply an [origin] to this model.
   * @param origin the [RequestOrigin] for this network model
   */
  open fun applyOrigin(origin: RequestOrigin) {
    this.origin = origin
  }
}
