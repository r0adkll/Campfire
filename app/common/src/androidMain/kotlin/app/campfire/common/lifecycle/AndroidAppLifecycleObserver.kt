package app.campfire.common.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidAppLifecycleObserver : AppLifecycleObserver {

  private val _state = MutableStateFlow(AppLifecycleState.Foreground)
  override val state: StateFlow<AppLifecycleState> = _state.asStateFlow()

  private val lifecycleObserver = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
      _state.value = AppLifecycleState.Foreground
    }
    override fun onStop(owner: LifecycleOwner) {
      _state.value = AppLifecycleState.Background
    }
  }

  init {
    val register = Runnable {
      ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      register.run()
    } else {
      Handler(Looper.getMainLooper()).post(register)
    }
  }
}
