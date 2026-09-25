// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.permission

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.logging.bark
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.core.permission.extractUrlHost
import app.campfire.core.permission.isPrivateNetworkAddress
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Android 17+ Local Network Protection gates private IP ranges behind the runtime
 * `ACCESS_LOCAL_NETWORK` permission. This prompts for it lazily — only once the user enters a
 * LAN address — so people connecting to a remote server are never asked.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class AndroidLocalNetworkPermissionController(
  private val application: Application,
  private val launcher: LocalNetworkPermissionLauncher,
  private val dispatcherProvider: DispatcherProvider,
  appLifecycleObserver: AppLifecycleObserver,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : LocalNetworkPermissionController {

  private val mutex = Mutex()
  private var requestedThisSession = false

  private val missing = MutableStateFlow(isPermissionMissing())

  init {
    // The permission can be granted from system settings while the app is backgrounded
    scope.launch {
      appLifecycleObserver.state
        .filter { it == AppLifecycleState.Foreground }
        .collect { refreshMissing() }
    }
  }

  override fun observePermissionMissing(): Flow<Boolean> = missing.asStateFlow()

  override fun openSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", application.packageName, null))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    application.startActivity(intent)
  }

  private fun refreshMissing() {
    missing.value = isPermissionMissing()
  }

  override suspend fun requestIfNeeded(serverUrl: String): Boolean {
    // LNP only applies on Android 17+; older versions have no such gate.
    if (Build.VERSION.SDK_INT < LNP_MIN_SDK) return true
    if (isGranted()) return true
    if (!pointsAtPrivateNetwork(serverUrl)) return true

    mutex.withLock {
      // Re-check inside the lock in case a concurrent request already resolved it.
      if (isGranted()) return true
      // Only surface the system dialog once per session; after that respect the OS decision
      // (a permanent denial returns immediately without UI anyway).
      if (requestedThisSession) return false
      requestedThisSession = true
    }

    val granted = launcher.launch(ACCESS_LOCAL_NETWORK).getOrDefault(false)
    bark { "ACCESS_LOCAL_NETWORK permission ${if (granted) "granted" else "denied"}" }
    refreshMissing()
    return granted
  }

  override fun isPermissionMissing(): Boolean {
    if (Build.VERSION.SDK_INT < LNP_MIN_SDK) return false
    return !isGranted()
  }

  override suspend fun request(): Boolean {
    if (Build.VERSION.SDK_INT < LNP_MIN_SDK) return true
    if (isGranted()) return true

    // User-initiated, so don't gate on requestedThisSession — but record the attempt so the
    // automatic server-url path doesn't prompt again afterwards.
    mutex.withLock {
      if (isGranted()) return true
      requestedThisSession = true
    }

    val granted = launcher.launch(ACCESS_LOCAL_NETWORK).getOrDefault(false)
    bark { "ACCESS_LOCAL_NETWORK permission ${if (granted) "granted" else "denied"} (explicit request)" }
    refreshMissing()
    return granted
  }

  /**
   * A public-looking hostname can still resolve to a LAN address via split-horizon DNS
   * (e.g. `abs.example.com` overridden to `192.168.x.x` on the local resolver), which LNP
   * gates all the same. DNS resolution itself goes through the system resolver and is exempt,
   * so when the string heuristic says "public" we resolve the host and check where it lands.
   */
  private suspend fun pointsAtPrivateNetwork(serverUrl: String): Boolean {
    if (isPrivateNetworkAddress(serverUrl)) return true
    val host = extractUrlHost(serverUrl) ?: return false
    return withContext(dispatcherProvider.io) {
      try {
        InetAddress.getAllByName(host).any { it.isPrivateAddress() }
      } catch (e: UnknownHostException) {
        false
      }
    }
  }

  private fun InetAddress.isPrivateAddress(): Boolean =
    isSiteLocalAddress || isLinkLocalAddress || isLoopbackAddress ||
      // ULA fc00::/7 isn't covered by isSiteLocalAddress
      (this is Inet6Address && (address[0].toInt() and 0xFE) == 0xFC)

  private fun isGranted(): Boolean =
    application.checkSelfPermission(ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED

  private companion object {
    // Local Network Protection is enforced starting in Android 17 (API 37). Android 16 shipped
    // the permission as opt-in for testing only: checkSelfPermission reports it as denied there,
    // but requesting it silently no-ops, so gating on 36 surfaced a dead permission prompt.
    const val LNP_MIN_SDK = 37

    // String literal rather than Manifest.permission.ACCESS_LOCAL_NETWORK so this still compiles
    // against a compileSdk that predates the constant.
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
  }
}
