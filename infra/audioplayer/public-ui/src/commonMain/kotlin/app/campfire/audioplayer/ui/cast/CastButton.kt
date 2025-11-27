package app.campfire.audioplayer.ui.cast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Cast
import app.campfire.common.compose.icons.rounded.CastConnected
import app.campfire.common.compose.icons.rounded.CastConnecting
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.util.withDensity
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.fluentIf
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.label_connecting
import campfire.infra.audioplayer.public_ui.generated.resources.media_route_dialog_title
import coil3.compose.AsyncImage
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(AppScope::class)
interface CastButtonComponent {
  val castController: CastController
}

@Composable
fun CastButton(
  modifier: Modifier = Modifier,
  component: CastButtonComponent = rememberComponent(),
) {
  val state by remember(component) {
    component.castController.state
  }.collectAsState()

  val devices by remember(component) {
    component.castController.availableDevices
  }.collectAsState()

  val currentDevice = devices.find { it.isSelected }

  var showDevices by remember { mutableStateOf(false) }

  if (currentDevice != null) {
    CurrentDeviceButton(
      state = state,
      device = currentDevice,
      onClick = { showDevices = !showDevices },
      modifier = modifier,
    )
  } else {
    CastButton(
      state = state,
      onClick = { showDevices = !showDevices },
      modifier = modifier,
    )
  }

  if (showDevices) {
    CastDevices(
      devices = devices,
      onDeviceClick = { device ->
        component.castController.connect(device)
        showDevices = false
      },
      onDismissRequest = { showDevices = false },
    )
  }
}

@Composable
private fun CastButton(
  state: CastState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (state == CastState.Unavailable) return

  IconButton(
    onClick = onClick,
    modifier = modifier,
  ) {
    val iconPainter = when (state) {
      CastState.Unavailable -> error("Invalid state for cast button")

      CastState.NoDevicesAvailable,
      CastState.NotConnected,
      -> rememberVectorPainter(CampfireIcons.Rounded.Cast)

      CastState.Connecting -> CampfireIcons.Rounded.CastConnecting
      CastState.Connected -> rememberVectorPainter(CampfireIcons.Rounded.CastConnected)
    }

    Icon(iconPainter, contentDescription = null)
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrentDeviceButton(
  state: CastState,
  device: CastDevice,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val buttonHeight = ButtonDefaults.ExtraSmallContainerHeight
  Button(
    onClick = onClick,
    modifier = modifier
      .padding(horizontal = 8.dp)
      .heightIn(buttonHeight),
    colors = ButtonDefaults.buttonColors(
//      containerColor = MaterialTheme.colorScheme.primaryContainer,
//      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    shapes = ButtonDefaults.shapes(
      shape = ButtonDefaults.squareShape,
      pressedShape = ButtonDefaults.shape,
    ),
    contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
  ) {
    AnimatedContent(
      targetState = state == CastState.Connecting,
    ) { isConnecting ->
      if (isConnecting) {
        CircularProgressIndicator(
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
          color = LocalContentColor.current,
          strokeWidth = 3.dp,
        )
      } else {
        Icon(
          device.asIcon(),
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
        )
      }
    }
    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
    AnimatedContent(
      targetState = state == CastState.Connecting,
      transitionSpec = {
        val enter = fadeIn(animationSpec = tween(220, delayMillis = 90)) +
          slideInVertically(animationSpec = tween(220, delayMillis = 90)) {
            -it / 2
          }
        val exit = fadeOut(animationSpec = tween(90)) +
          slideOutVertically(animationSpec = tween(90)) {
            it / 2
          }
        enter togetherWith exit
      },
    ) { isConnecting ->
      if (isConnecting) {
        Text(
          text = stringResource(Res.string.label_connecting),
          style = ButtonDefaults.textStyleFor(buttonHeight),
        )
      } else {
        Text(
          text = device.displayName,
          style = ButtonDefaults.textStyleFor(buttonHeight),
        )
      }
    }
  }
}

@Composable
private fun CastDevices(
  devices: List<CastDevice>,
  onDeviceClick: (CastDevice) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var visible by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    visible = true
  }

  Impression {
    ScreenViewEvent("CastDevicesPopup", ScreenType.Dialog)
  }

  val dismissPopup: () -> Unit = {
    scope.launch {
      visible = false
      delay(200)
      onDismissRequest()
    }
  }

  Popup(
    alignment = Alignment.TopEnd,
    offset = IntOffset(
      x = 0,
      y = withDensity { (-16).dp.roundToPx() },
    ),
    onDismissRequest = dismissPopup,
  ) {
    Box(
      modifier = Modifier
        .padding(end = 12.dp)
        .fillMaxSize()
        .clickable(
          onClick = dismissPopup,
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
        ),
      contentAlignment = Alignment.TopEnd,
    ) {
      AnimatedVisibility(
        visible = visible,
        enter = expandIn(
          expandFrom = Alignment.TopEnd,
        ) + fadeIn(),
        exit = shrinkOut(
          shrinkTowards = Alignment.TopEnd,
        ) + fadeOut(),
      ) {
        CastDevicesCard(
          devices = devices,
          onDeviceClick = onDeviceClick,
          onDismissRequest = onDismissRequest,
        )
      }
    }
  }
}

@Composable
private fun CastDevicesCard(
  devices: List<CastDevice>,
  onDeviceClick: (CastDevice) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier
      .fillMaxWidth(0.75f),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = stringResource(Res.string.media_route_dialog_title),
        style = MaterialTheme.typography.titleLarge,
        fontFamily = PaytoneOneFontFamily,
      )
    }
    LazyColumn(
      modifier = Modifier
        .heightIn(max = 500.dp),
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        bottom = 16.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(
        items = devices,
        key = { it.id },
      ) { device ->
        CastDeviceListItem(
          device = device,
          onClick = {
            onDeviceClick(device)
          },
        )
      }
    }
  }
}

@Composable
private fun CastDeviceListItem(
  device: CastDevice,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = if (device.isSelected) {
    CircleShape
  } else {
    RoundedCornerShape(16.dp)
  }
  val containerColor = if (device.isSelected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.secondaryContainer
  }
  val contentColor = if (device.isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSecondaryContainer
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        color = containerColor,
        shape = shape,
      )
      .fluentIf(device.isSelected) {
        border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = shape,
        )
      }
      .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides contentColor,
    ) {
      Box(
        modifier = Modifier
          .padding(16.dp),
      ) {
        device.iconUri?.let { uri ->
          AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
              .size(24.dp),
          )
        } ?: run {
          Icon(
            device.asIcon(),
            contentDescription = null,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = device.displayName,
          style = MaterialTheme.typography.titleSmall,
        )

        device.description?.let { desc ->
          Text(
            text = desc,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}
