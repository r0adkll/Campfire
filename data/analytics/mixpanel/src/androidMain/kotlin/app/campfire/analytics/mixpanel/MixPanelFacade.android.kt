package app.campfire.analytics.mixpanel

import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONArray
import org.json.JSONObject

actual class MixPanelFacade(
  /*
   * If we attempt to feed this an api with a null token, the getInstance() returns
   * a null instance. Since this might be the case, let's just no-op here
   */
  private val mixpanel: MixpanelAPI?,
) {
  private var _identity: String? = null
  actual val debugState: String
    get() = "MixPanel[optIn=${!isOptOut}, identity=$_identity]"

  actual val isOptOut: Boolean get() = mixpanel?.hasOptedOutTracking() == true

  actual fun optIn() {
    mixpanel?.optInTracking()
  }

  actual fun optOut() {
    mixpanel?.optOutTracking()
  }

  actual fun identify(distinctId: String, usePeople: Boolean) {
    _identity = distinctId
    mixpanel?.identify(distinctId, usePeople)
  }

  actual fun track(eventName: String, properties: Map<String, Any?>?) {
    properties?.let { props ->
      mixpanel?.track(eventName, props.toJSONObject())
    } ?: mixpanel?.track(eventName)
  }

  private fun Map<*, *>.toJSONObject(): JSONObject {
    return JSONObject().apply {
      entries.forEach { (k, value) ->
        val key = k.toString()
        when (value) {
          is String -> put(key, value)
          is Int -> put(key, value)
          is Long -> put(key, value)
          is Double -> put(key, value)
          is Float -> put(key, value.toDouble())
          is Boolean -> put(key, value)
          is Array<*> -> put(key, JSONArray(value))
          is Collection<*> -> put(key, JSONArray(value))
          is Map<*, *> -> put(key, value.toJSONObject())
          else -> put(key, value)
        }
      }
    }
  }
}
