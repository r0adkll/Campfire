// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.SeriesAdded
import app.campfire.socket.events.SeriesRemoved
import app.campfire.socket.events.SeriesUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class SeriesCacheInvalidationListener(
  private val handler: SeriesEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is SeriesAdded -> handler.onSeriesAdded(event.series)
      is SeriesUpdated -> handler.onSeriesUpdated(event.series)
      is SeriesRemoved -> handler.onSeriesRemoved(
        seriesId = event.payload.id,
        libraryId = event.payload.libraryId,
      )
      else -> Unit
    }
  }
}
