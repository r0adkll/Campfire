package app.campfire.sessions.network

import app.campfire.account.api.UserRepository
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asSeconds
import app.campfire.core.model.Session
import app.campfire.network.models.BookChapter
import app.campfire.network.models.DeviceInfo
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.PlaybackSession
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class DefaultNetworkSessionMapper(
  private val campfireSettings: CampfireSettings,
  private val applicationInfo: ApplicationInfo,
  private val userRepository: UserRepository,
) : NetworkSessionMapper {

  override suspend fun map(session: Session): PlaybackSession {
    val currentUser = userRepository.getCurrentUser()
    return PlaybackSession(
      id = session.id.toString(),
      userId = currentUser.id,
      libraryId = session.libraryItem.libraryId,
      libraryItemId = session.libraryItem.id,
      episodeId = null,
      mediaType = session.libraryItem.mediaType.name.lowercase(),
      mediaPlayer = "campfire",
      mediaMetadata = with(session.libraryItem.media.metadata) {
        MinifiedBookMetadata(
          title = title,
          subtitle = subtitle,
          genres = genres,
          publishedYear = publishedYear,
          publishedDate = publishedDate,
          publisher = publisher,
          description = description,
          isbn = ISBN,
          asin = ASIN,
          language = language,
          explicit = isExplicit,
          abridged = isAbridged,
          titleIgnorePrefix = titleIgnorePrefix,
          authorName = authorName,
          authorNameLF = authorNameLastFirst,
          narratorName = narratorName,
          seriesName = seriesName,
        )
      },
      chapters = session.libraryItem.media.chapters.map { c ->
        BookChapter(
          id = c.id,
          title = c.title,
          start = c.start,
          end = c.end,
        )
      },
      displayTitle = session.libraryItem.media.metadata.title!!,
      displayAuthor = session.libraryItem.media.metadata.authorName!!,
      coverPath = session.libraryItem.media.coverPath,
      duration = session.libraryItem.media.durationInMillis.milliseconds.toDouble(DurationUnit.SECONDS),
      playMethod = 0, // Direct Play - TODO: Figure out these different methods
      deviceInfo = DeviceInfo(
        id = campfireSettings.deviceId,
        userId = currentUser.id,
        deviceId = campfireSettings.deviceId,
        osName = applicationInfo.osName,
        osVersion = applicationInfo.osVersion,
        clientName = "Campfire",
        clientVersion = applicationInfo.versionName,
        manufacturer = applicationInfo.manufacturer,
        model = applicationInfo.model,
        sdkVersion = applicationInfo.sdkVersion,
      ),
      serverVersion = "", // This value is dumb, the server knows what version it is
      date = "", // This value is dumb, why send this AND startedAt
      dayOfWeek = "", // This value is dumb, why send this AND startedAt
      timeListening = session.timeListening.asSeconds(),
      startTime = session.startTime.asSeconds(),
      currentTime = session.currentTime.asSeconds(),
      startedAt = session.startedAt.toInstant(TimeZone.UTC).toEpochMilliseconds(),
      updatedAt = session.updatedAt.toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )
  }
}
