package app.campfire.network.models

import app.campfire.network.RequestOrigin

abstract class NetworkModel {

  var origin: RequestOrigin = RequestOrigin.None
}
