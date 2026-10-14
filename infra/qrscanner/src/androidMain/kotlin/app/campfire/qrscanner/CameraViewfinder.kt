// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FlashlightOff
import app.campfire.common.compose.icons.rounded.FlashlightOn
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.infra.qrscanner.generated.resources.Res
import campfire.infra.qrscanner.generated.resources.action_turn_off_flashlight
import campfire.infra.qrscanner.generated.resources.action_turn_on_flashlight
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

  KeepSystemBarIconsLight()

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

    permission.status.shouldShowRationale -> CameraPermissionMessage(
      denied = false,
      onAction = permission::launchPermissionRequest,
      modifier = modifier,
    )

    else -> CameraPermissionMessage(
      denied = true,
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
    CameraUnavailableMessage(modifier)
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

/**
 * The viewfinder is dark whatever the app's theme, so the status and navigation bars' icons stay
 * light while it's shown, and go back to the theme's afterwards.
 */
@Composable
private fun KeepSystemBarIconsLight() {
  val view = LocalView.current
  DisposableEffect(view) {
    val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
    val controller = WindowCompat.getInsetsController(window, view)
    val lightStatusBars = controller.isAppearanceLightStatusBars
    val lightNavigationBars = controller.isAppearanceLightNavigationBars
    controller.isAppearanceLightStatusBars = false
    controller.isAppearanceLightNavigationBars = false
    onDispose {
      controller.isAppearanceLightStatusBars = lightStatusBars
      controller.isAppearanceLightNavigationBars = lightNavigationBars
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
