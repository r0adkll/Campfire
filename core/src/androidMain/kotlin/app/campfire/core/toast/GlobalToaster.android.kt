package app.campfire.core.toast

import android.os.Handler
import android.os.Looper

actual inline fun runInMainThread(crossinline block: () -> Unit) {
  Handler(Looper.getMainLooper()).post { block() }
}
