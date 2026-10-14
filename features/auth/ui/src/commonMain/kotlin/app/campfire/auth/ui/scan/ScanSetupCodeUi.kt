// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.auth.api.screen.ScanSetupCodeScreen
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.qrscanner.QrCodeScanner
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_close
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
    qrCodeScanner.Viewfinder(
      onScanned = { state.eventSink(ScanSetupCodeUiEvent.Scanned(it)) },
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

    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = OverlayAlpha),
      shape = MaterialTheme.shapes.large,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(24.dp)
        .widthIn(max = 480.dp),
    ) {
      AnimatedContent(state.sawOtherCode) { sawOtherCode ->
        Text(
          text = stringResource(
            if (sawOtherCode) Res.string.scan_setup_code_other_code else Res.string.scan_setup_code_instructions,
          ),
          style = MaterialTheme.typography.bodyLarge,
          color = if (sawOtherCode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
      }
    }
  }
}

private const val OverlayAlpha = 0.85f
