package app.campfire.android.toast

import android.content.Context
import android.widget.Toast as SystemToast
import app.campfire.common.compose.toast.Toast
import app.campfire.common.compose.toast.ToastHandle

class AndroidToast(
  private val context: Context,
) : Toast {

  override fun show(message: String, duration: Toast.Duration): ToastHandle {
    val systemToast = SystemToast.makeText(
      context,
      message,
      when (duration) {
        Toast.Duration.SHORT -> SystemToast.LENGTH_SHORT
        Toast.Duration.LONG -> SystemToast.LENGTH_LONG
      },
    )
    systemToast.show()

    return AndroidToastHandle(systemToast)
  }
}

class AndroidToastHandle(
  val systemToast: SystemToast,
) : ToastHandle {
  override fun cancel() {
    systemToast.cancel()
  }
}
