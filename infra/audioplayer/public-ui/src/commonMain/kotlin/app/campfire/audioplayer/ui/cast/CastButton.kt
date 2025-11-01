package app.campfire.audioplayer.ui.cast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeviceUnknown
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

  var showDevices by remember { mutableStateOf(false) }

  CastButton(
    state = state,
    onClick = { showDevices = !showDevices },
    modifier = modifier,
  )

  if (showDevices) {
    CastDevices(
      devices = devices,
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
//    enabled = state == CastState.Connected || state == CastState.NotConnected,
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

@Composable
private fun CastDevices(
  devices: List<CastDevice>,
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
          onDeviceClick = { device ->

          },
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
        text = "Devices",
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
  val shape = RoundedCornerShape(16.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = shape,
      )
      .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
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
        val icon = if (device.id == CastDevice.DEFAULT_ID) {
          Icons.Rounded.PhoneAndroid
        } else {
          Icons.Rounded.DeviceUnknown
        }
        Icon(
          icon,
          contentDescription = null,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = if (device.id == CastDevice.DEFAULT_ID) {
          "This phone"
        } else {
          device.name
        },
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
