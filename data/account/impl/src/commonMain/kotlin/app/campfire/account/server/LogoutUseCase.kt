package app.campfire.account.server

import app.campfire.account.api.ServerRepository
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

interface LogoutUseCase {

  suspend fun execute(server: Server)
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultLogoutUseCase(
  private val serverRepository: ServerRepository,
) : LogoutUseCase {

  override suspend fun execute(server: Server) {
    // Delete the core server db models and relations
    serverRepository.remove(server)
  }
}
