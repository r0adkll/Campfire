// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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

  // Start in Background: a process launched for playback alone (media resumption, Android Auto,
  // a notification action) never starts an activity, so ProcessLifecycleOwner never emits and a
  // Foreground default would keep foreground-only work (the socket) running indefinitely.
  // Registering the observer replays ON_START when an activity is already visible.
  private val _state = MutableStateFlow(AppLifecycleState.Background)
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
