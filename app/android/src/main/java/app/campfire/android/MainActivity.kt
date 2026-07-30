// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import app.campfire.android.di.ActivityComponent
import app.campfire.android.toast.AndroidToast
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLink
import app.campfire.core.navigation.DeepLinkKeys
import app.campfire.core.session.serverUrl
import app.campfire.core.toast.GlobalToaster
import app.campfire.tracing.Trace
import app.campfire.tracing.trace
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

class MainActivity : ComponentActivity() {

  private lateinit var component: ActivityComponent

  private val deepLinkFlow = MutableStateFlow<DeepLink>(DeepLink.None)

  override fun onCreate(savedInstanceState: Bundle?) = Trace.trace("MainActivity.onCreate") {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    bark { "MainActivity::onCreate()" }

    component = Trace.trace("MainActivity.inject") {
      ComponentHolder.component<ActivityComponent.Factory>()
        .create(this)
        .also {
          ComponentHolder.updateComponent(lifecycleScope, it)
        }
    }

    // Initialize the CastContext used for Google Cast
    // https://developers.google.com/cast/docs/android_sender/integrate#kotlin
    component.castController.initialize()

    // Register all flow launchers
    component.componentActivityPlugins.forEach { launcher ->
      launcher.register(this)
    }

    // Upgrade path: a user already signed in to a LAN server before the app began targeting
    // Android 16+ won't pass through the login screen, so prompt for local-network access here
    // if their active server is private and the permission is missing. Waits for the session to
    // restore (it may still be Loading at onCreate), then requests once. No-ops otherwise.
    lifecycleScope.launch {
      val serverUrl = component.userSessionManager.observe()
        .mapNotNull { it.serverUrl }
        .first()
      component.localNetworkPermission.requestIfNeeded(serverUrl)
    }

    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Parse Deeplink
    if (intent != null) {
      val libraryItemId = intent.extras?.getString(DeepLinkKeys.LibraryItemId)
      if (libraryItemId != null) {
        deepLinkFlow.value = DeepLink.ItemDetail(libraryItemId)
      }
    }

    // Configure toaster
    val toaster = AndroidToast(this)
    GlobalToaster.register(toaster)

    setContent {
      val deepLink by remember {
        deepLinkFlow
      }.collectAsState()

      CompositionLocalProvider(
        LocalToast provides toaster,
      ) {
        component.campfireContent(
          ::finish,
          { url: String ->
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@MainActivity, url.toUri())
          },
          deepLink,
          Modifier,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    // Parse DeepLink parameters
    val libraryItemId = intent.extras?.getString(DeepLinkKeys.LibraryItemId)
    if (libraryItemId != null) {
      deepLinkFlow.value = DeepLink.ItemDetail(libraryItemId)
    }
  }

  override fun onStart() {
    super.onStart()
    bark { "MainActivity::onStart()" }
    with(component) {
      mediaControllerConnector.connect()
      castController.scanForDevices()
      offlineDownloadManager.resumeDownloads()
    }
  }

  override fun onStop() {
    super.onStop()
    bark { "MainActivity::onStop()" }
    with(component) {
      mediaControllerConnector.disconnect()
      castController.stopScanningForDevices()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    bark { "MainActivity::onDestroy()" }
    GlobalToaster.unregister()
    component.castController.destroy()
    component.componentActivityPlugins.forEach { launcher ->
      launcher.unregister()
    }
    ComponentHolder.removeComponent(component)
  }
}

@ContributesBinding(AppScope::class)
@Inject
class MainActivityIntentProvider(
  private val application: Application,
) : ActivityIntentProvider {

  override fun provide(): Intent {
    return Intent(application, MainActivity::class.java)
  }
}
