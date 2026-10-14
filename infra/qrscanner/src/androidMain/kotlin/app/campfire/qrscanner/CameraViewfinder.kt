// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FlashlightOff
import app.campfire.common.compose.icons.rounded.FlashlightOn
import app.campfire.common.compose.icons.rounded.NoPhotography
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.infra.qrscanner.generated.resources.Res
import campfire.infra.qrscanner.generated.resources.action_allow_camera
import campfire.infra.qrscanner.generated.resources.action_open_app_settings
import campfire.infra.qrscanner.generated.resources.action_turn_off_flashlight
import campfire.infra.qrscanner.generated.resources.action_turn_on_flashlight
import campfire.infra.qrscanner.generated.resources.camera_permission_denied_message
import campfire.infra.qrscanner.generated.resources.camera_permission_message
import campfire.infra.qrscanner.generated.resources.camera_permission_title
import campfire.infra.qrscanner.generated.resources.camera_unavailable_message
import campfire.infra.qrscanner.generated.resources.camera_unavailable_title
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.Executors
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the camera the first time it's shown, then shows the camera's view and reports each
 * QR code it reads. Without the camera it explains why it wants it, and once Android stops
 * asking, sends the user to Campfire's app settings instead.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun CameraViewfinder(
  onScanned: (String) -> Unit,
  captured: Boolean,
  hint: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  var requested by rememberSaveable { mutableStateOf(false) }
  var answered by rememberSaveable { mutableStateOf(false) }
  val permission = rememberPermissionState(Manifest.permission.CAMERA) { answered = true }

  LaunchedEffect(Unit) {
    if (!permission.status.isGranted && !requested) {
      requested = true
      permission.launchPermissionRequest()
    }
  }

  val context = LocalContext.current
  when {
    permission.status.isGranted -> CameraPreview(
      onScanned = onScanned,
      captured = captured,
      hint = hint,
      modifier = modifier,
    )

    // The system's own dialog is up; it explains itself
    requested && !answered -> Box(modifier)

    permission.status.shouldShowRationale -> CameraMessage(
      icon = CampfireIcons.Rounded.NoPhotography,
      title = stringResource(Res.string.camera_permission_title),
      message = stringResource(Res.string.camera_permission_message),
      action = stringResource(Res.string.action_allow_camera),
      onAction = permission::launchPermissionRequest,
      modifier = modifier,
    )

    else -> CameraMessage(
      icon = CampfireIcons.Rounded.NoPhotography,
      title = stringResource(Res.string.camera_permission_title),
      message = stringResource(Res.string.camera_permission_denied_message),
      action = stringResource(Res.string.action_open_app_settings),
      onAction = { context.openAppSettings() },
      modifier = modifier,
    )
  }
}

@Composable
private fun CameraPreview(
  onScanned: (String) -> Unit,
  captured: Boolean,
  hint: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnScanned by rememberUpdatedState(onScanned)

  var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
  var camera by remember { mutableStateOf<Camera?>(null) }
  var unavailable by remember { mutableStateOf(false) }
  var torchOn by remember { mutableStateOf(false) }

  LaunchedEffect(lifecycleOwner) {
    val provider = ProcessCameraProvider.awaitInstance(context)
    val scans = Channel<String>(Channel.CONFLATED)
    val executor = Executors.newSingleThreadExecutor()
    val preview = Preview.Builder().build().apply {
      setSurfaceProvider { surfaceRequest = it }
    }
    val analysis = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      // A setup link's code is dense (about 57 modules a side); CameraX's default 640x480 frames
      // leave too few pixels per module unless it fills the view
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setResolutionStrategy(
            ResolutionStrategy(AnalysisResolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
          )
          .build(),
      )
      .build()
      .apply { setAnalyzer(executor, QrCodeAnalyzer { scans.trySend(it) }) }

    try {
      val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
        CameraSelector.DEFAULT_BACK_CAMERA
      } else {
        CameraSelector.DEFAULT_FRONT_CAMERA
      }
      camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
      launch {
        for (text in scans) currentOnScanned(text)
      }
      awaitCancellation()
    } catch (_: CameraInfoUnavailableException) {
      unavailable = true
    } catch (_: IllegalArgumentException) {
      // No camera matched the selector
      unavailable = true
    } catch (_: IllegalStateException) {
      // The camera couldn't be opened, e.g. another app holds it
      unavailable = true
    } finally {
      analysis.clearAnalyzer()
      provider.unbind(preview, analysis)
      executor.shutdown()
      scans.close()
      camera = null
      surfaceRequest = null
    }
  }

  if (unavailable) {
    CameraMessage(
      icon = CampfireIcons.Rounded.NoPhotography,
      title = stringResource(Res.string.camera_unavailable_title),
      message = stringResource(Res.string.camera_unavailable_message),
      modifier = modifier,
    )
    return
  }

  Box(modifier) {
    val request = surfaceRequest ?: return@Box
    CameraXViewfinder(
      surfaceRequest = request,
      modifier = Modifier.fillMaxSize(),
    )

    val activeCamera = camera
    ViewfinderGuide(
      captured = captured,
      hint = hint,
      controls = {
        if (activeCamera != null && activeCamera.cameraInfo.hasFlashUnit()) {
          val label = stringResource(
            if (torchOn) Res.string.action_turn_off_flashlight else Res.string.action_turn_on_flashlight,
          )
          IconButtonTooltip(text = label) {
            FilledTonalIconToggleButton(
              checked = torchOn,
              onCheckedChange = { on ->
                torchOn = on
                activeCamera.cameraControl.enableTorch(on)
              },
              modifier = Modifier.size(56.dp),
            ) {
              Icon(
                if (torchOn) CampfireIcons.Rounded.FlashlightOff else CampfireIcons.Rounded.FlashlightOn,
                contentDescription = label,
              )
            }
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
private fun CameraMessage(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  action: String? = null,
  onAction: () -> Unit = {},
) {
  Box(modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .widthIn(max = 360.dp)
        .padding(24.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(48.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (action != null) {
        Button(onClick = onAction) {
          Text(action)
        }
      }
    }
  }
}

private fun Context.openAppSettings() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      .setData(Uri.fromParts("package", packageName, null)),
  )
}

private val AnalysisResolution = Size(1280, 720)
