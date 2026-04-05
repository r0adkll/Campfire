package app.campfire.widgets

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.theme.colorScheme
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.session.UserSession
import app.campfire.home.api.HomeRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.SleepSettings
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.widgets.composables.CompactPlaybackContent
import app.campfire.widgets.composables.ConstrainedPlaybackContent
import app.campfire.widgets.composables.ExpandedPlaybackContent
import app.campfire.widgets.composables.SinglePlaybackContent
import app.campfire.widgets.composables.PlaybackInfo
import app.campfire.widgets.composables.PlayerWidgetScaffold
import app.campfire.widgets.composables.WidgetHeightClass
import app.campfire.widgets.composables.WidgetScaffold
import app.campfire.widgets.composables.WidgetSizeClass
import app.campfire.widgets.composables.WidgetWidthClass
import app.campfire.widgets.theme.asColorProviders
import com.r0adkll.kimchi.annotations.ContributesTo
import com.r0adkll.swatchbuckler.compose.Theme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull

@ContributesTo(UserScope::class)
interface PlayerWidgetComponent {
  val playerWidgetUserSession: UserSession
  val sessionsRepository: SessionsRepository
  val audioPlayerHolder: AudioPlayerHolder
  val activityIntentProgression: ActivityIntentProvider
  val homeRepository: HomeRepository
  val settings: CampfireSettings
  val sleepSettings: SleepSettings
  val themeManager: ThemeManager
}

class PlayerWidget : GlanceAppWidget() {

  companion object {
    val KEY_CURRENT_TIME get() = floatPreferencesKey("current-time")
    val KEY_CURRENT_DURATION get() = floatPreferencesKey("current-duration")
    val KEY_PLAYBACK_SPEED get() = floatPreferencesKey("playback-speed")
  }

  override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSizeClass.ResponsiveSizes)
  override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      GlanceTheme(
        colors = GlanceTheme.colors,
      ) {
        val size = LocalSize.current
        val sizeClass = WidgetSizeClass.from(size)
        bark("PlayerWidget") {
          "Widget Size [$size] ==> $sizeClass"
        }
        PlayerWidgetContent(sizeClass)
      }
    }
  }

  @Immutable
  data class SessionLite(
    val id: String,
    val title: String,
    val libraryItem: LibraryItem,
    val prevChapter: Chapter?,
    val nextChapter: Chapter?,
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun PlayerWidgetContent(
    widgetSizeClass: WidgetSizeClass,
    modifier: GlanceModifier = GlanceModifier,
  ) {
    // Widgets are …weird… so we must subscribe to component / component changes or going from an app state
    // of Force Stopped, or completely dead, to active won't cause existing widgets to update...for some reason.
    val component by remember {
      ComponentHolder.subscribe<PlayerWidgetComponent>()
    }.collectAsState(null)

    val mainActivityAction: Action = if (component != null) {
      actionStartActivity(component!!.activityIntentProgression.provide())
    } else {
      // NOTE: This works since we have a proguard rule to keep the MainActivity name
      //  from being obfuscated
      actionStartActivity(ComponentName("app.campfire.android", "app.campfire.android.MainActivity"))
    }

    val currentSession by remember(component) {
      component?.sessionsRepository?.observeCurrentSession()
        ?.mapNotNull { session ->
          session?.let { s ->
            val currentChapter = s.chapter
            var prevChapter: Chapter? = null
            var nextChapter: Chapter? = null
            s.libraryItem.media.chapters.let { chapters ->
              val currentIndex = chapters.indexOf(currentChapter)
              if (currentIndex > 0) {
                prevChapter = chapters[currentIndex - 1]
              }
              if (currentIndex < chapters.size - 1) {
                nextChapter = chapters[currentIndex + 1]
              }
            }
            SessionLite(
              id = s.id.toHexDashString(),
              title = s.title,
              libraryItem = s.libraryItem,
              prevChapter = prevChapter,
              nextChapter = nextChapter,
            )
          }
        }
        ?.distinctUntilChanged()
        ?: emptyFlow()
    }.collectAsState(null)

    val theme by remember {
      snapshotFlow { currentSession?.libraryItem?.id }
        .filterNotNull()
        .flatMapLatest { itemId ->
          component?.themeManager
            ?.observeThemeFor(itemId)
            ?: emptyFlow()
        }
    }.collectAsState(null)

//    val discoverShelf by remember(component, widgetSizeClass) {
//      val userSession = component?.playerWidgetUserSession
//      component?.homeRepository
//        ?.takeIf {
//          widgetSizeClass.width == WidgetWidthClass.Expanded &&
//            widgetSizeClass.height > WidgetHeightClass.Single
//        }
//        ?.observeShelf("${ShelfIds.Discover}_${userSession?.user?.id}_${userSession?.user?.selectedLibraryId}", ShelfType.BOOK)
//        ?.onEach {
//          bark("WidgetShelf") { "entities: $it" }
//        }
//        ?.map { shelfEntities -> shelfEntities.map { it as LibraryItem } }
//        ?: emptyFlow()
//    }.collectAsState(null)

    val audioPlayer by remember(component) {
      component?.audioPlayerHolder?.currentPlayer ?: MutableStateFlow(null)
    }.collectAsState()

    val lastSetSleepTimer by remember(component) {
      component?.sleepSettings?.observeLastSetSleepTimer() ?: MutableStateFlow(Duration.ZERO)
    }.collectAsState()

    val runningTimer by remember(audioPlayer) {
      audioPlayer?.runningTimer ?: MutableStateFlow(null)
    }.collectAsState()

    if (currentSession != null) {
      val currentMetadata = remember(audioPlayer) {
        audioPlayer?.currentMetadata ?: MutableStateFlow(Metadata())
      }.collectAsState()

      val state = remember(audioPlayer) {
        audioPlayer?.state ?: MutableStateFlow(AudioPlayer.State.Disabled)
      }.collectAsState()

      val currentTime = currentState(KEY_CURRENT_TIME)?.seconds ?: Duration.ZERO
      val currentDuration = currentState(KEY_CURRENT_DURATION)?.seconds ?: Duration.ZERO
      val playbackSpeed = currentState(KEY_PLAYBACK_SPEED) ?: 1f

      GlanceTheme(
        colors = theme?.asColorProviders()
          ?: GlanceTheme.colors
      ) {
        ActiveWidgetContent(
          title = currentMetadata.value.title ?: currentSession!!.title,
          subtitle = currentSession!!.libraryItem.media.metadata.title ?: "",
          artworkUrl = currentMetadata.value.artworkUri ?: currentSession!!.libraryItem.media.coverImageUrl,
          playbackState = state.value,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          sleepTimerDuration = lastSetSleepTimer,
          runningTimer = runningTimer,
          libraryItem = currentSession?.libraryItem,
          prevChapter = currentSession?.prevChapter,
          nextChapter = currentSession?.nextChapter,
          onClick = mainActivityAction,
          widgetSizeClass = widgetSizeClass,
          modifier = modifier,
        )
      }
    } else {
      val context = LocalContext.current
      InActiveWidgetContent(
        title = context.getString(R.string.player_widget_title_default),
        subtitle = context.getString(R.string.player_widget_subtitle_default),
        onClick = mainActivityAction,
        widgetSizeClass = widgetSizeClass,
        modifier = modifier,
      )
    }
  }

  @Composable
  private fun ActiveWidgetContent(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    playbackState: AudioPlayer.State,
    currentTime: Duration,
    currentDuration: Duration,
    playbackSpeed: Float,
    sleepTimerDuration: Duration,
    runningTimer: RunningTimer?,
    libraryItem: LibraryItem?,
    prevChapter: Chapter?,
    nextChapter: Chapter?,
    onClick: Action,
    widgetSizeClass: WidgetSizeClass,
    modifier: GlanceModifier = GlanceModifier,
  ) {
    PlayerWidgetScaffold(
      onClick = onClick,
      modifier = modifier,
      content = {
        when (widgetSizeClass.height) {
          WidgetHeightClass.Single -> SinglePlaybackContent(
            title = title,
            subtitle = subtitle,
            artworkUrl = artworkUrl,
            playbackState = playbackState,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            sizeClass = widgetSizeClass,
          )

          WidgetHeightClass.LargeCompact,
          WidgetHeightClass.Compact -> CompactPlaybackContent(
            title = title,
            subtitle = subtitle,
            artworkUrl = artworkUrl,
            playbackState = playbackState,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            sleepTimerDuration = sleepTimerDuration,
            runningTimer = runningTimer,
            prevChapter = prevChapter.takeIf { widgetSizeClass.height == WidgetHeightClass.LargeCompact },
            nextChapter = nextChapter.takeIf { widgetSizeClass.height == WidgetHeightClass.LargeCompact },
            sizeClass = widgetSizeClass,
          )

          WidgetHeightClass.Tall,
          WidgetHeightClass.Expanded -> ExpandedPlaybackContent(
            title = title,
            subtitle = subtitle,
            artworkUrl = artworkUrl,
            playbackState = playbackState,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            sleepTimerDuration = sleepTimerDuration,
            runningTimer = runningTimer,
            prevChapter = prevChapter,
            nextChapter = nextChapter,
            sizeClass = widgetSizeClass,
          )
        }
      },
    )
  }

  @Composable
  private fun InActiveWidgetContent(
    title: String,
    subtitle: String,
    onClick: Action,
    widgetSizeClass: WidgetSizeClass,
    modifier: GlanceModifier = GlanceModifier,
  ) {
    WidgetScaffold(
      sizeClass = widgetSizeClass,
      defaultBackground = if (widgetSizeClass.height != WidgetHeightClass.Single) {
        ImageProvider(R.drawable.default_background_expanded)
      } else {
        ImageProvider(R.drawable.default_background)
      },
      onClick = onClick,
      modifier = modifier,
      playbackContent = {
        if (widgetSizeClass.width == WidgetWidthClass.Expanded) {
          ConstrainedPlaybackContent(
            title = title,
            subtitle = subtitle,
            artworkUrl = null,
            playbackState = AudioPlayer.State.Disabled,
            currentTime = 0.seconds,
            currentDuration = 0.seconds,
            playbackSpeed = 1f,
            sizeClass = widgetSizeClass,
            backgroundColor = null,
          ) {
            PlaybackInfo(
              title = title,
              subtitle = subtitle,
              modifier = GlanceModifier.defaultWeight(),
            )
          }
        }
      },
      content = {
      },
    )
  }
}
