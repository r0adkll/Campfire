package app.campfire.whatsnew.api

import kotlinx.coroutines.flow.Flow

interface WhatsNewRepository {

  suspend fun getChangelog(): Changelog

  fun observeShouldShowWhatsNew(): Flow<Boolean>
  suspend fun dismissWhatsNew()
}
