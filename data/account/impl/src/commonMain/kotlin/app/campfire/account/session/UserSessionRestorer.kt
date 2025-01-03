package app.campfire.account.session

import app.campfire.CampfireDatabase
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDomainModel
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.measureTimedValue
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface UserSessionRestorer {

  suspend fun restore(): UserSession
}

@ContributesBinding(AppScope::class)
@Inject
class DatabaseUserSessionRestorer(
  private val settings: CampfireSettings,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserSessionRestorer {

  override suspend fun restore(): UserSession = measureTimedValue {
    val currentUserId = settings.currentUserId ?: return@measureTimedValue UserSession.LoggedOut

    val user = withContext(dispatcherProvider.databaseRead) {
      db.usersQueries.selectById(currentUserId)
        .awaitAsOne()
        .asDomainModel()
    }

    return@measureTimedValue UserSession.LoggedIn(user)
  }.let { timedValue ->
    bark("UserSessionRestorer") { "Restored ${timedValue.value} in ${timedValue.duration}" }
    timedValue.value
  }
}
