// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.auth.api.ConnectLinkInbox
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.api.screen.ScanSetupCodeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.navigation.ConnectLink
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Waits for the camera to read a setup link's QR code, then replaces itself with
 * [ConnectScreen], which confirms the link exactly as it does one opened from a page. The first
 * setup link wins; anything read after it is ignored. A QR code that isn't a setup link only
 * shows a hint, and scanning carries on.
 */
@CircuitInject(ScanSetupCodeScreen::class, UserScope::class)
@Inject
class ScanSetupCodePresenter(
  private val navigator: Navigator,
  private val connectLinkInbox: ConnectLinkInbox,
) : Presenter<ScanSetupCodeUiState> {

  /** Identifies each scanned link in the [ConnectLinkInbox], as a deep link's nonce does. */
  internal var nextLinkId: () -> Long = { Random.nextLong() }

  @Composable
  override fun present(): ScanSetupCodeUiState {
    var handled by remember { mutableStateOf(false) }

    // The camera keeps reporting a code while it's in view; the hint fades once it's gone
    var otherCodeSightings by remember { mutableIntStateOf(0) }
    var sawOtherCode by remember { mutableStateOf(false) }
    LaunchedEffect(otherCodeSightings) {
      if (otherCodeSightings == 0) return@LaunchedEffect
      sawOtherCode = true
      delay(OtherCodeHintDuration)
      sawOtherCode = false
    }

    return ScanSetupCodeUiState(sawOtherCode = sawOtherCode) { event ->
      when (event) {
        is ScanSetupCodeUiEvent.Scanned -> {
          if (handled) return@ScanSetupCodeUiState
          val result = ConnectLink.parseUrl(event.text)
          if (result == null) {
            otherCodeSightings++
            return@ScanSetupCodeUiState
          }

          val linkId = nextLinkId()
          if (connectLinkInbox.deliver(linkId, result)) {
            handled = true
            navigator.pop()
            navigator.goTo(ConnectScreen(linkId))
          }
        }

        ScanSetupCodeUiEvent.Close -> navigator.pop()
      }
    }
  }

  private companion object {
    val OtherCodeHintDuration = 3.seconds
  }
}
