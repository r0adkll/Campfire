package app.campfire.network.models

import app.campfire.network.RequestOrigin

/**
 * Experimental abstraction to embed network/request information into returned network models so that
 * we can do stuff like trace its origin (serverUrl, etc).
 */
abstract class NetworkModel {

  var origin: RequestOrigin = RequestOrigin.None
}
