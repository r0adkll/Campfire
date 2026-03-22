package app.campfire.audioplayer.impl.history

import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

interface PlaybackHistoryRecorder {

  fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration? = null,
  )
}

@ContributesBinding(UserScope::class)
@Inject
class PlaybackHistoryRecorderImpl(
  private val repository: PlaybackHistoryRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : PlaybackHistoryRecorder {

  override fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration?,
  ) {
    coroutineScopeHolder.get().launch {
      repository.record(libraryItemId, type, fromPosition, toPosition)
    }
  }
}
