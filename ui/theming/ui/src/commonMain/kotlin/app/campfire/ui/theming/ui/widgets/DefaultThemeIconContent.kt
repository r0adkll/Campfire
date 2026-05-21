package app.campfire.ui.theming.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.di.AppScope
import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultThemeIconContent(
  private val themeRepository: AppThemeRepository,
  private val socketManager: SocketManager,
) : ThemeIconContent {

  @Composable
  override fun Content(onClick: () -> Unit, modifier: Modifier) {
    val currentAppTheme by remember {
      themeRepository.observeCurrentAppTheme()
    }.collectAsState()

    val socketState by socketManager.state.collectAsState()

    Box(
      modifier = modifier,
    ) {
      AppThemeImage(
        appTheme = currentAppTheme,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onClick)
          .fillMaxSize(),
      )

      SocketIndicator(
        state = socketState,
        modifier = Modifier
          .align(Alignment.TopEnd)
      )
    }
  }
}

@Composable
private fun SocketIndicator(
  state: SocketState,
  modifier: Modifier = Modifier,
) {
  val indicatorColor by animateColorAsState(
    when (state) {
      is SocketState.Authenticated -> CampfireTheme.colorScheme.success
      SocketState.Authenticating -> CampfireTheme.colorScheme.loading
      SocketState.Connecting -> CampfireTheme.colorScheme.loading
      SocketState.Disconnected -> MaterialTheme.colorScheme.error
      is SocketState.Failed -> MaterialTheme.colorScheme.error
    },
  )

  val infiniteTransition = rememberInfiniteTransition("socket_indicator_alpha")

  val indicatorAlpha = when (state) {
    is SocketState.Authenticated,
    is SocketState.Failed,
    SocketState.Disconnected,
      -> 1f

    SocketState.Authenticating,
    SocketState.Connecting,
      -> infiniteTransition.animateFloat(
      0.3f, 0.8f,
      animationSpec = infiniteRepeatable(
        tween(500, easing = EaseInOutCubic),
        RepeatMode.Reverse,
      ),
    ).value
  }

  Canvas(
    modifier = modifier
      .size(8.dp),
  ) {
    drawCircle(
      color = indicatorColor,
      alpha = indicatorAlpha,
    )
  }
}
