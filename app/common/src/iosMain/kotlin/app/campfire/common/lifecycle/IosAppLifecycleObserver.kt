package app.campfire.common.lifecycle

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosAppLifecycleObserver : AppLifecycleObserver {

  private val _state = MutableStateFlow(AppLifecycleState.Foreground)
  override val state: StateFlow<AppLifecycleState> = _state.asStateFlow()

  init {
    val center = NSNotificationCenter.defaultCenter
    center.addObserverForName(
      name = UIApplicationDidBecomeActiveNotification,
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { _ ->
      _state.value = AppLifecycleState.Foreground
    }
    center.addObserverForName(
      name = UIApplicationWillResignActiveNotification,
      `object` = null,
      queue = NSOperationQueue.mainQueue,
    ) { _ ->
      _state.value = AppLifecycleState.Background
    }
  }
}
