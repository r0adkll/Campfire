package app.campfire.audioplayer.impl.cast

import android.content.Context
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.ModuleUnavailableException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
object SafeCastContext {

  private var isUnavailable = AtomicBoolean(false)

  /**
   * Attempt to get the current [CastContext], and returning null
   * if it fails
   */
  fun getContext(context: Context): CastContext? {
    // If we've already determined that this device doesn't have the
    // Cast module (i.e. PlayServices) installed then short circuit.
    if (isUnavailable.load()) return null

    return try {
      CastContext.getSharedInstance(context)
    } catch (e: ModuleUnavailableException) {
      isUnavailable.store(true)
      bark(
        LogPriority.WARN,
        throwable = e,
      ) { "PlayServices Cast module is unavailable, locking the feature out for the session" }
      null
    } catch (e: Exception) {
      bark(
        LogPriority.ERROR,
        throwable = e,
      ) { "Unable to get CastContext" }
      null
    }
  }
}
