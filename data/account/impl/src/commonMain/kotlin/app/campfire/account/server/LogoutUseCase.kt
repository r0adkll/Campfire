package app.campfire.account.server

import app.campfire.account.api.ServerRepository
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

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
