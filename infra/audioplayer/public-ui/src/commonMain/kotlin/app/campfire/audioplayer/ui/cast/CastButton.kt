// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastDevice.Type
import app.campfire.audioplayer.cast.CastState
import app.campfire.audioplayer.cast.ConnectionAttempt
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Cast
import app.campfire.common.compose.icons.rounded.CastConnected
import app.campfire.common.compose.icons.rounded.CastConnecting
import app.campfire.common.compose.icons.rounded.CastWarning
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.util.withDensity
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.fluentIf
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.action_allow_local_network
import campfire.infra.audioplayer.public_ui.generated.resources.action_cast
import campfire.infra.audioplayer.public_ui.generated.resources.label_connecting
import campfire.infra.audioplayer.public_ui.generated.resources.label_couldnt_connect
import campfire.infra.audioplayer.public_ui.generated.resources.label_local_network_rationale
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
    // Intensive discovery only while the picker is visible; the controller keeps the
    // active-scan window fresh until this leaves composition.
    DisposableEffect(component) {
      component.castController.startActiveScan()
      onDispose { component.castController.stopActiveScan() }
    }

    val needsLocalNetworkPermission by remember(component) {
      component.castController.needsLocalNetworkPermission
    }.collectAsState()

    val connectionAttempt by remember(component) {
      component.castController.connectionAttempt
    }.collectAsState()

    // A brokered connect keeps the picker open showing progress; close it once the
    // attempt resolves successfully (Connecting -> null).
    var sawConnecting by remember { mutableStateOf(false) }
    LaunchedEffect(connectionAttempt) {
      when (connectionAttempt?.status) {
        ConnectionAttempt.Status.Connecting -> sawConnecting = true
        ConnectionAttempt.Status.Failed -> sawConnecting = false
        null -> if (sawConnecting) {
          sawConnecting = false
          showDevices = false
        }
      }
    }

    CastDevices(
      devices = devices,
      connectionAttempt = connectionAttempt,
      showLocalNetworkPermission = needsLocalNetworkPermission,
      onRequestLocalNetworkPermission = {
        component.castController.requestLocalNetworkPermission()
      },
      onDeviceClick = { device ->
        component.castController.connect(device)
        // Session-based devices (Google Cast) resolve asynchronously and drive the
        // popup through connectionAttempt; instant output switches close it directly.
        if (!device.requiresSession) {
          showDevices = false
        }
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

  val castLabel = stringResource(Res.string.action_cast)
  IconButtonTooltip(
    text = castLabel,
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
    ) {
      val iconPainter = when (state) {
        CastState.Unavailable -> error("Invalid state for cast button")

        CastState.NoDevicesAvailable,
        CastState.NotConnected,
        -> rememberVectorPainter(CampfireIcons.Rounded.Cast)

        CastState.Connecting -> CampfireIcons.Rounded.CastConnecting
        CastState.Connected -> rememberVectorPainter(CampfireIcons.Rounded.CastConnected)
      }

      Icon(iconPainter, contentDescription = castLabel)
    }
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
  connectionAttempt: ConnectionAttempt?,
  showLocalNetworkPermission: Boolean,
  onRequestLocalNetworkPermission: () -> Unit,
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
          connectionAttempt = connectionAttempt,
          showLocalNetworkPermission = showLocalNetworkPermission,
          onRequestLocalNetworkPermission = onRequestLocalNetworkPermission,
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
  connectionAttempt: ConnectionAttempt?,
  showLocalNetworkPermission: Boolean,
  onRequestLocalNetworkPermission: () -> Unit,
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
          attemptStatus = connectionAttempt?.status?.takeIf { connectionAttempt.deviceId == device.id },
          onClick = {
            onDeviceClick(device)
          },
        )
      }

      if (showLocalNetworkPermission) {
        item(key = "local_network_permission") {
          LocalNetworkPermissionListItem(
            onClick = onRequestLocalNetworkPermission,
          )
        }
      }
    }
  }
}

/**
 * Opt-in action shown when finding cast devices may require the platform's local network
 * permission. Never triggered automatically — the user chooses to grant it from here.
 */
@Composable
private fun LocalNetworkPermissionListItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(16.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = shape,
      )
      .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
      Box(
        modifier = Modifier
          .padding(16.dp),
      ) {
        Icon(
          CampfireIcons.Rounded.CastWarning,
          contentDescription = null,
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp,
          ),
      ) {
        Text(
          text = stringResource(Res.string.action_allow_local_network),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = stringResource(Res.string.label_local_network_rationale),
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
  }
}

@Composable
private fun CastDeviceListItem(
  device: CastDevice,
  attemptStatus: ConnectionAttempt.Status?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = if (device.isSelected) {
    CircleShape
  } else {
    RoundedCornerShape(16.dp)
  }
  val containerColor = if (device.isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest
  }
  val contentColor = if (device.isSelected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
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
        if (attemptStatus == ConnectionAttempt.Status.Connecting) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = LocalContentColor.current,
            strokeWidth = 3.dp,
          )
        } else {
          device.iconUri?.let { uri ->
            AsyncImage(
              model = uri,
              contentDescription = null,
              colorFilter = ColorFilter.tint(contentColor),
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
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 8.dp),
      ) {
        Text(
          text = device.displayName,
          style = MaterialTheme.typography.titleMedium,
        )

        if (attemptStatus == ConnectionAttempt.Status.Failed) {
          // errorContainer/onErrorContainer is a guaranteed-contrast pair in every theme,
          // unlike bare `error` over this row's variable container colors
          Text(
            text = stringResource(Res.string.label_couldnt_connect),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
              .padding(top = 2.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer)
              .padding(horizontal = 8.dp, vertical = 2.dp),
          )
        } else {
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
}

/*
 * Previews — the connect failure and progress states are hard to catch on a device
 * (discovery recovers faster than the failure can be observed), so verify them here.
 */

private class PreviewCastDevice(
  id: String,
  name: String,
  type: Type,
  isSelected: Boolean = false,
  requiresSession: Boolean = true,
) : CastDevice(
  id = id,
  name = name,
  description = null,
  iconUri = null,
  type = type,
  isSelected = isSelected,
  requiresSession = requiresSession,
)

private val PreviewDevices = listOf(
  PreviewCastDevice(CastDevice.DEFAULT_ID, "Phone", Type.SMARTPHONE, requiresSession = false),
  PreviewCastDevice("office-speaker", "Office speaker", Type.SPEAKER),
  PreviewCastDevice("kitchen-speaker", "Kitchen speaker", Type.SPEAKER),
  PreviewCastDevice("living-room-tv", "Living room TV", Type.TV, isSelected = true),
)

@Preview
@Composable
private fun CastDevicesCardFailedPreview() {
  CampfireTheme {
    CastDevicesCard(
      devices = PreviewDevices,
      connectionAttempt = ConnectionAttempt("office-speaker", ConnectionAttempt.Status.Failed),
      showLocalNetworkPermission = true,
      onRequestLocalNetworkPermission = {},
      onDeviceClick = {},
      onDismissRequest = {},
    )
  }
}

@Preview
@Composable
private fun CastDevicesCardFailedOnSelectedPreview() {
  CampfireTheme {
    CastDevicesCard(
      devices = PreviewDevices,
      connectionAttempt = ConnectionAttempt("living-room-tv", ConnectionAttempt.Status.Failed),
      showLocalNetworkPermission = false,
      onRequestLocalNetworkPermission = {},
      onDeviceClick = {},
      onDismissRequest = {},
    )
  }
}

@Preview
@Composable
private fun CastDevicesCardConnectingPreview() {
  CampfireTheme {
    CastDevicesCard(
      devices = PreviewDevices,
      connectionAttempt = ConnectionAttempt("office-speaker", ConnectionAttempt.Status.Connecting),
      showLocalNetworkPermission = false,
      onRequestLocalNetworkPermission = {},
      onDeviceClick = {},
      onDismissRequest = {},
    )
  }
}

@Preview
@Composable
private fun CastDevicesCardDarkFailedPreview() {
  CampfireTheme(useDarkColors = true) {
    CastDevicesCard(
      devices = PreviewDevices,
      connectionAttempt = ConnectionAttempt("office-speaker", ConnectionAttempt.Status.Failed),
      showLocalNetworkPermission = true,
      onRequestLocalNetworkPermission = {},
      onDeviceClick = {},
      onDismissRequest = {},
    )
  }
}
