package app.campfire.widgets

import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.model.Metadata
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItem
import app.campfire.home.api.HomeRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.widgets.composables.ChapterListContent
import app.campfire.widgets.composables.ConstrainedPlaybackContent
import app.campfire.widgets.composables.FullPlaybackContent
import app.campfire.widgets.composables.PlaybackInfo
import app.campfire.widgets.composables.WidgetHeightClass
import app.campfire.widgets.composables.WidgetScaffold
import app.campfire.widgets.composables.WidgetSizeClass
import app.campfire.widgets.composables.WidgetWidthClass
import app.campfire.widgets.theme.CampfireGlanceColorScheme
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

@ContributesTo(UserScope::class)
interface PlayerWidgetComponent {
  val sessionsRepository: SessionsRepository
  val audioPlayerHolder: AudioPlayerHolder
  val activityIntentProgression: ActivityIntentProvider
  val homeRepository: HomeRepository
  val settings: CampfireSettings
}

class PlayerWidget : GlanceAppWidget() {

  companion object {
    val KEY_CURRENT_TIME get() = floatPreferencesKey("current-time")
    val KEY_CURRENT_DURATION get() = floatPreferencesKey("current-duration")
    val KEY_PLAYBACK_SPEED get() = floatPreferencesKey("playback-speed")
  }

  override val sizeMode: SizeMode = SizeMode.Exact
  override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          GlanceTheme.colors
        } else {
          CampfireGlanceColorScheme.colors
        },
      ) {
        val size = LocalSize.current
        val sizeClass = WidgetSizeClass.from(size)
        PlayerWidgetContent(sizeClass)
      }
    }
  }

  @Immutable
  data class SessionLite(
    val id: String,
    val title: String,
    val libraryItem: LibraryItem,
  )

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
            SessionLite(
              id = s.id.toHexDashString(),
              title = s.title,
              libraryItem = s.libraryItem,
            )
          }
        }
        ?.distinctUntilChanged()
        ?: emptyFlow()
    }.collectAsState(null)

    val audioPlayer by remember(component) {
      component?.audioPlayerHolder?.currentPlayer ?: MutableStateFlow(null)
    }.collectAsState()

    if (currentSession != null) {
      val currentMetadata = remember(audioPlayer) {
        audioPlayer?.currentMetadata ?: MutableStateFlow(Metadata())
      }.collectAsState()

      val state = remember(audioPlayer) {
        audioPlayer?.state ?: MutableStateFlow(AudioPlayer.State.Disabled)
      }.collectAsState()

      val showTimeInBook = remember(component) {
        component?.settings?.observeShowTimeInBook() ?: emptyFlow()
      }.collectAsState(true)

      val currentTime = currentState(KEY_CURRENT_TIME)?.seconds ?: Duration.ZERO
      val currentDuration = currentState(KEY_CURRENT_DURATION)?.seconds ?: Duration.ZERO
      val playbackSpeed = currentState(KEY_PLAYBACK_SPEED) ?: 1f

      ActiveWidgetContent(
        title = currentMetadata.value.title ?: currentSession!!.title,
        subtitle = currentSession!!.libraryItem.media.metadata.title ?: "",
        artworkUrl = currentMetadata.value.artworkUri ?: currentSession!!.libraryItem.media.coverImageUrl,
        playbackState = state.value,
        currentTime = currentTime,
        currentDuration = currentDuration,
        currentPlayingChapterId = -1,
        playbackSpeed = playbackSpeed,
        libraryItem = currentSession?.libraryItem,
        showTimeInBook = showTimeInBook.value,
        onClick = mainActivityAction,
        widgetSizeClass = widgetSizeClass,
        modifier = modifier,
      )
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
    currentPlayingChapterId: Int,
    playbackSpeed: Float,
    libraryItem: LibraryItem?,
    showTimeInBook: Boolean,
    onClick: Action,
    widgetSizeClass: WidgetSizeClass,
    modifier: GlanceModifier = GlanceModifier,
  ) {
    WidgetScaffold(
      sizeClass = widgetSizeClass,
      artworkUrl = artworkUrl,
      onClick = onClick,
      modifier = modifier,
      playbackContent = {
        if (
          widgetSizeClass.heightSizeClass == WidgetHeightClass.Single ||
          widgetSizeClass.widthSizeClass != WidgetWidthClass.Expanded
        ) {
          FullPlaybackContent(
            title = title,
            subtitle = subtitle,
            playbackState = playbackState,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            widthSizeClass = widgetSizeClass.widthSizeClass,
          )
        } else {
          ConstrainedPlaybackContent(
            title = title,
            subtitle = subtitle,
            playbackState = playbackState,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            widthSizeClass = widgetSizeClass.widthSizeClass,
          )
        }
      },
      content = {
        if (libraryItem != null) {
          ChapterListContent(
            item = libraryItem,
            currentPlayingChapterId = currentPlayingChapterId,
            showTimeInBook = showTimeInBook,
          )
        } else {
          Box(
            modifier = GlanceModifier
              .fillMaxWidth()
              .defaultWeight(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
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
      artworkUrl = null,
      defaultBackground = if (widgetSizeClass.heightSizeClass != WidgetHeightClass.Single) {
        ImageProvider(R.drawable.default_background_expanded)
      } else {
        ImageProvider(R.drawable.default_background)
      },
      onClick = onClick,
      modifier = modifier,
      playbackContent = {
        if (widgetSizeClass.widthSizeClass == WidgetWidthClass.Expanded) {
          ConstrainedPlaybackContent(
            title = title,
            subtitle = subtitle,
            playbackState = AudioPlayer.State.Disabled,
            currentTime = 0.seconds,
            currentDuration = 0.seconds,
            playbackSpeed = 1f,
            widthSizeClass = widgetSizeClass.widthSizeClass,
            backgroundColor = null,
            contentColor = GlanceTheme.colors.onSecondary,
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
