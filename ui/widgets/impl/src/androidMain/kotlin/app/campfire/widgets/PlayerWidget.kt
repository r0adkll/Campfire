package app.campfire.widgets

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.model.Metadata
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.sessions.api.SessionsRepository
import app.campfire.widgets.callbacks.ForwardActionCallback
import app.campfire.widgets.callbacks.PlayPauseActionCallback
import app.campfire.widgets.callbacks.RewindActionCallback
import app.campfire.widgets.composables.GlanceIconButton
import app.campfire.widgets.theme.CampfireGlanceColorScheme
import app.campfire.widgets.theme.withAlpha
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

@ContributesTo(UserScope::class)
interface PlayerWidgetComponent {
  val sessionsRepository: SessionsRepository
  val audioPlayerHolder: AudioPlayerHolder
  val activityIntentProgression: ActivityIntentProvider
}

enum class WidgetSize(val breakpoint: Dp) {
  Single(90.dp),
  Compact(240.dp),
  Expanded(290.dp),
  ;

  companion object {
    fun from(size: DpSize): WidgetSize = entries
      .firstOrNull { size.width < it.breakpoint }
      ?: Expanded
  }
}

class PlayerWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Exact

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
        val widgetSize = WidgetSize.from(size)
        PlayerWidgetContent(widgetSize)
      }
    }
  }

  @Composable
  private fun PlayerWidgetContent(
    size: WidgetSize,
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
      component?.sessionsRepository?.observeCurrentSession() ?: emptyFlow()
    }.collectAsState(null)

    val audioPlayer by remember(component) {
      component?.audioPlayerHolder?.currentPlayer ?: MutableStateFlow(null)
    }.collectAsState()

    if (currentSession != null) {
      val currentMetadata by remember(audioPlayer) {
        audioPlayer?.currentMetadata ?: MutableStateFlow(Metadata())
      }.collectAsState()

      val state by remember(audioPlayer) {
        audioPlayer?.state ?: MutableStateFlow(AudioPlayer.State.Disabled)
      }.collectAsState()

      ActiveWidgetContent(
        title = currentMetadata.title ?: currentSession!!.chapter.title,
        subtitle = currentSession!!.libraryItem.media.metadata.title ?: "",
        artworkUrl = currentMetadata.artworkUri ?: currentSession!!.libraryItem.media.coverImageUrl,
        playbackState = state,
        onClick = mainActivityAction,
        size = size,
        modifier = modifier,
      )
    } else {
      val context = LocalContext.current
      ActiveWidgetContent(
        title = context.getString(R.string.player_widget_title_default),
        subtitle = context.getString(R.string.player_widget_subtitle_default),
        artworkUrl = null,
        playbackState = AudioPlayer.State.Disabled,
        onClick = mainActivityAction,
        size = size,
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
    onClick: Action,
    size: WidgetSize,
    modifier: GlanceModifier = GlanceModifier,
  ) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .clickable(onClick)
        .appWidgetBackground()
        .background(GlanceTheme.colors.background),
    ) {
      if (artworkUrl != null) {
        GlanceImage(
          url = artworkUrl,
          modifier = GlanceModifier
            .fillMaxSize(),
        )
      } else {
        Image(
          provider = ImageProvider(R.drawable.default_background),
          contentScale = ContentScale.Crop,
          contentDescription = null,
          colorFilter = ColorFilter.tint(
            GlanceTheme.colors.secondaryContainer.withAlpha(0.5f),
          ),
          modifier = GlanceModifier.fillMaxSize(),
        )
      }

      Row(
        modifier = GlanceModifier
          .fillMaxSize()
          .padding(
            horizontal = if (size == WidgetSize.Expanded) {
              24.dp
            } else {
              8.dp
            },
          ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (size == WidgetSize.Expanded) {
          Alignment.Start
        } else {
          Alignment.CenterHorizontally
        },
      ) {
        if (size == WidgetSize.Expanded) {
          Column(
            modifier = GlanceModifier.defaultWeight(),
          ) {
            Text(
              text = title,
              style = TextStyle(
                color = GlanceTheme.colors.onSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
              ),
            )
            Text(
              text = subtitle,
              style = TextStyle(
                color = GlanceTheme.colors.onSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
              ),
            )
          }
          Spacer(GlanceModifier.width(8.dp))
        }

        if (size != WidgetSize.Single) {
          GlanceIconButton(
            resourceId = R.drawable.ic_media_replay,
            contentDescription = null,
            onClickActionCallback = RewindActionCallback::class,
            colorFilter = ColorFilter.tint(
              GlanceTheme.colors.onSecondary,
            ),
          )

          Spacer(GlanceModifier.width(16.dp))
        }

        if (playbackState == AudioPlayer.State.Initializing || playbackState == AudioPlayer.State.Buffering) {
          CircularProgressIndicator()
        } else {
          CircleIconButton(
            imageProvider = ImageProvider(
              when (playbackState) {
                AudioPlayer.State.Initializing -> error("This state should never be reached")
                AudioPlayer.State.Buffering -> error("This state should never be reached")

                AudioPlayer.State.Playing,
                -> R.drawable.ic_media_pause

                AudioPlayer.State.Paused,
                AudioPlayer.State.Disabled,
                AudioPlayer.State.Finished,
                -> R.drawable.ic_media_play
              },
            ),
            contentDescription = null,
            onClick = actionRunCallback(PlayPauseActionCallback::class.java),
          )
        }

        if (size == WidgetSize.Compact) {
          Spacer(GlanceModifier.width(16.dp))

          GlanceIconButton(
            resourceId = R.drawable.ic_media_forward,
            contentDescription = null,
            onClickActionCallback = ForwardActionCallback::class,
            colorFilter = ColorFilter.tint(
              GlanceTheme.colors.onSecondary,
            ),
          )
        }
      }
    }
  }
}

@Composable
private fun GlanceImage(
  url: Any?,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(url) {
    withContext(Dispatchers.IO) {
      val request = ImageRequest.Builder(context)
        .data(url)
        .build()

      bitmap = when (val result = context.imageLoader.execute(request)) {
        is ErrorResult -> null
        is SuccessResult -> {
          bark { "Successfully loaded: $url from ${result.dataSource}" }
          result.image.toBitmap()
        }
      }
    }
  }

  bitmap.let {
    if (bitmap != null) {
      Image(
        provider = ImageProvider(bitmap!!),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.75f),
        ),
      )
    } else {
      Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
  }
}
