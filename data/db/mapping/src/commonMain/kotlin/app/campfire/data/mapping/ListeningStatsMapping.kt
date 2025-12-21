package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.extensions.seconds
import app.campfire.core.model.ItemListenedTo
import app.campfire.core.model.ListeningStats
import app.campfire.network.models.ItemsListenedTo
import app.campfire.network.models.ListeningStats as NetworkListeningStats
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

suspend fun NetworkListeningStats.asDomainModel(
  urlHydrator: UrlHydrator,
): ListeningStats {
  return ListeningStats(
    totalTime = totalTime.seconds,
    today = today.seconds,
    days = days.map { (date, duration) ->
      LocalDate.parse(date) to duration.seconds
    }.toMap(),
    dayOfWeek = dayOfWeek.mapKeys { it.key.asDayOfWeek() }.mapValues { it.value.seconds },
    items = items.values.map { it.asDomainModel(urlHydrator) },
    recentSessions = recentSessions.map { it.asDomainModel(urlHydrator) },
  )
}

suspend fun ItemsListenedTo.asDomainModel(
  urlHydrator: UrlHydrator,
): ItemListenedTo {
  return ItemListenedTo(
    id = id,
    timeListening = timeListening.seconds,
    coverImageUrl = urlHydrator.hydrateLibraryItem(id),
    mediaMetadata = mediaMetadata.asDomainModel(),
  )
}

private fun String.asDayOfWeek(): DayOfWeek = when (this.lowercase()) {
  "sunday" -> DayOfWeek.SUNDAY
  "monday" -> DayOfWeek.MONDAY
  "tuesday" -> DayOfWeek.TUESDAY
  "wednesday" -> DayOfWeek.WEDNESDAY
  "thursday" -> DayOfWeek.THURSDAY
  "friday" -> DayOfWeek.FRIDAY
  "saturday" -> DayOfWeek.SATURDAY
  else -> error("Unrecognized day of week")
}
