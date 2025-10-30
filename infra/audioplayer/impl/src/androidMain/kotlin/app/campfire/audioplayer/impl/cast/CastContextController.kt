package app.campfire.audioplayer.impl.cast

import android.app.Application
import androidx.annotation.MainThread
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.google.android.gms.cast.framework.CastContext
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class CastContextController(
  private val application: Application,
) {

  var castContext: CastContextState = CastContextState.Loading
    private set

  @MainThread
  fun initialize() {
    try {
      val context = CastContext.getSharedInstance(application)
      castContext = CastContextState.Ready(context)
    } catch (e: IllegalStateException) {
      e.printStackTrace()
      castContext = CastContextState.Error(e)
    }
  }
}

sealed interface CastContextState {
  val contextOrNull: CastContext?
    get() = (this as? CastContextState.Ready)?.castContext

  data object Loading : CastContextState
  data class Ready(val castContext: CastContext) : CastContextState
  data class Error(val error: Throwable) : CastContextState
}
