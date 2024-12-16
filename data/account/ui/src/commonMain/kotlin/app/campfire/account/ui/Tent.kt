package app.campfire.account.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.campfire.account.api.ServerRepository
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.model.Tent
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.flow.map

@ContributesTo(UserScope::class)
interface ServerProviderComponent {
  val serverRepository: ServerRepository
}

@Composable
private fun rememberServerRepository(key: Any): ServerRepository {
  return remember(key) {
    ComponentHolder.component<ServerProviderComponent>()
      .serverRepository
  }
}

@Composable
fun rememberCurrentTent(
  key: Any,
  serverRepository: ServerRepository = rememberServerRepository(key),
): Tent {
  val tent = remember(serverRepository) {
    serverRepository.observeCurrentServer().map { it.tent }
  }.collectAsState(initial = Tent.Default)

  return tent.value
}
