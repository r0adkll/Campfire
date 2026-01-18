package app.campfire.android.logging

import android.util.Log
import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority

class AndroidBark : Heartwood.Bark {

  override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String) {
    var msg = message()
    if (extras != null) {
      msg += "\nExtras[${extras.entries.joinToString { "${it.key}:${it.value}" }}]"
    }

    Log.println(priority.priority, tag, msg)
  }
}
