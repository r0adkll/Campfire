package app.campfire.android.permission

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.core.permission.extractUrlHost
import app.campfire.core.permission.isPrivateNetworkAddress
import com.r0adkll.kimchi.annotations.ContributesBinding
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Android 16+ Local Network Protection gates private IP ranges behind the runtime
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
) : LocalNetworkPermissionController {

  private val mutex = Mutex()
  private var requestedThisSession = false

  override suspend fun requestIfNeeded(serverUrl: String): Boolean {
    // LNP only applies on Android 16+; older versions have no such gate.
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
    // Local Network Protection was introduced in Android 16 (API 36).
    const val LNP_MIN_SDK = 36

    // String literal rather than Manifest.permission.ACCESS_LOCAL_NETWORK so this still compiles
    // against a compileSdk that predates the constant.
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
  }
}
