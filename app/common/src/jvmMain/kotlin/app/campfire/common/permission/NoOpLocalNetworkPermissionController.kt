package app.campfire.common.permission

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.permission.LocalNetworkPermissionController
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/** Desktop has no local-network permission gate, so access is always available. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpLocalNetworkPermissionController : LocalNetworkPermissionController {
  override suspend fun requestIfNeeded(serverUrl: String): Boolean = true
}
