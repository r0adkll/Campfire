// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.auth.ui.login.AuthMethodState
import app.campfire.auth.ui.login.ConnectionState
import app.campfire.ui.theming.api.AppTheme
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class ServerCardFocusTest {

  // The password field only appears once the server is found, like after picking an account
  private var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Loading)

  @Test
  fun `the password field takes focus when it appears if asked to`() = runComposeUiTest {
    setContent { Card(focusPassword = true) }
    connectionState = FOUND

    onNodeWithText("Password").assertIsFocused()
  }

  @Test
  fun `the password field doesn't take focus otherwise`() = runComposeUiTest {
    setContent { Card(focusPassword = false) }
    connectionState = FOUND

    onNodeWithText("Password").assertIsNotFocused()
  }

  @Composable
  private fun Card(focusPassword: Boolean) = PreviewSharedElementTransitionLayout {
    ServerCard(
      theme = AppTheme.Fixed.Tent,
      onThemeChange = {},
      serverName = "Home",
      onServerNameChange = {},
      serverUrl = "https://abs.example.com",
      onServerUrlChange = {},
      urlState = rememberServerUrlFieldState("https://abs.example.com"),
      networkSettings = null,
      onEditNetworkSettingsClick = {},
      username = "alice",
      onUsernameChange = {},
      password = "",
      onPasswordChange = {},
      onGo = {},
      connectionState = connectionState,
      authError = null,
      isAuthenticating = false,
      focusPassword = focusPassword,
    )
  }

  private companion object {
    val FOUND = ConnectionState.Success(
      AuthMethodState(passwordAuthEnabled = true, openIdState = null),
    )
  }
}
