// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.campfire.auth.api.screen.ScanSetupCodeScreen
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Error
import app.campfire.common.compose.icons.rounded.QrCodeScanner
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.qrscanner.QrCodeScanner
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_close
import campfire.features.auth.ui.generated.resources.scan_setup_code_found
import campfire.features.auth.ui.generated.resources.scan_setup_code_instructions
import campfire.features.auth.ui.generated.resources.scan_setup_code_other_code
import campfire.features.auth.ui.generated.resources.scan_setup_code_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(ScanSetupCodeScreen::class, UserScope::class)
@Composable
fun ScanSetupCodeUi(
  state: ScanSetupCodeUiState,
  qrCodeScanner: QrCodeScanner,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface),
  ) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.captured) {
      if (state.captured) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    LaunchedEffect(state.sawOtherCode) {
      if (state.sawOtherCode) haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }

    qrCodeScanner.Viewfinder(
      onScanned = { state.eventSink(ScanSetupCodeUiEvent.Scanned(it)) },
      captured = state.captured,
      hint = { ScanHint(state) },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = OverlayAlpha))
        .statusBarsPadding()
        .padding(4.dp),
    ) {
      val closeLabel = stringResource(Res.string.action_close)
      IconButtonTooltip(text = closeLabel) {
        IconButton(onClick = { state.eventSink(ScanSetupCodeUiEvent.Close) }) {
          Icon(CampfireIcons.Rounded.Close, contentDescription = closeLabel)
        }
      }
      Text(
        text = stringResource(Res.string.scan_setup_code_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}

private enum class HintKind { Instructions, OtherCode, Captured }

@Composable
private fun ScanHint(
  state: ScanSetupCodeUiState,
  modifier: Modifier = Modifier,
) {
  val kind = when {
    state.captured -> HintKind.Captured
    state.sawOtherCode -> HintKind.OtherCode
    else -> HintKind.Instructions
  }
  AnimatedContent(
    targetState = kind,
    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
    contentAlignment = Alignment.Center,
    modifier = modifier.widthIn(max = 360.dp),
  ) { hint ->
    val (container, content) = when (hint) {
      HintKind.Instructions -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
      HintKind.OtherCode -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
      HintKind.Captured -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
      color = container,
      contentColor = content,
      shape = MaterialTheme.shapes.large,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Icon(
          imageVector = when (hint) {
            HintKind.Instructions -> CampfireIcons.Rounded.QrCodeScanner
            HintKind.OtherCode -> CampfireIcons.Rounded.Error
            HintKind.Captured -> CampfireIcons.Rounded.CheckCircle
          },
          contentDescription = null,
        )
        Text(
          text = stringResource(
            when (hint) {
              HintKind.Instructions -> Res.string.scan_setup_code_instructions
              HintKind.OtherCode -> Res.string.scan_setup_code_other_code
              HintKind.Captured -> Res.string.scan_setup_code_found
            },
          ),
          style = MaterialTheme.typography.bodyLarge,
        )
      }
    }
  }
}

private const val OverlayAlpha = 0.85f
