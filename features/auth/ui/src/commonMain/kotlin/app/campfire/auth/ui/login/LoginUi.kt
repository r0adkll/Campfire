package app.campfire.auth.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.auth.ui.login.composables.ServerCard
import app.campfire.auth.ui.login.composables.TitleBanner
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_add_campsite
import campfire.features.auth.ui.generated.resources.label_authenticating_loading_message
import campfire.features.auth.ui.generated.resources.login_add_account_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LoginScreen::class, UserScope::class)
@Composable
fun Login(
  screen: LoginScreen,
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {
  CampfireTheme(
    tent = state.tent,
  ) {
    LoginContent(
      state = state,
      isAddingAccount = screen.isAddingAccount,
      modifier = modifier,
    )
  }
}

@Composable
private fun LoginContent(
  state: LoginUiState,
  isAddingAccount: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .systemBarsPadding()
      .fillMaxSize(),
  ) {
    Box {
      if (isAddingAccount) {
        CampfireTopAppBar(
          title = { Text(stringResource(Res.string.login_add_account_title)) },
          navigationIcon = {
            IconButton(
              onClick = { state.eventSink(LoginUiEvent.NavigateBack) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
          },
        )
      } else {
        TitleBanner(
          modifier = Modifier
            .padding(
              horizontal = 24.dp,
              vertical = 48.dp,
            ),
        )
      }

      LoginUiContent(
        state = state,
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth(),
      )
    }
  }
}

@Composable
internal fun LoginUiContent(
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink
  var hasFocus by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.padding(horizontal = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ServerCard(
      tent = state.tent,
      onTentChange = { eventSink(LoginUiEvent.ChangeTent(it)) },
      serverName = state.serverName,
      onServerNameChange = { eventSink(LoginUiEvent.ServerName(it)) },
      serverUrl = state.serverUrl,
      onServerUrlChange = { eventSink(LoginUiEvent.ServerUrl(it)) },
      username = state.userName,
      onUsernameChange = { eventSink(LoginUiEvent.UserName(it)) },
      password = state.password,
      onPasswordChange = { eventSink(LoginUiEvent.Password(it)) },
      onGo = { eventSink(LoginUiEvent.AddCampsite) },
      connectionState = state.connectionState,
      authError = state.authError,
      isAuthenticating = state.isAuthenticating,
      modifier = Modifier.onFocusChanged {
        hasFocus = it.hasFocus
      }.widthIn(max = MaxContentWidth),
    )

    Spacer(Modifier.height(16.dp))

    Button(
      enabled = state.serverUrl.isNotBlank() &&
        state.userName.isNotBlank() &&
        state.password.isNotBlank() &&
        !state.isAuthenticating,
      onClick = {
        eventSink(LoginUiEvent.AddCampsite)
      },
      modifier = Modifier
        .widthIn(max = MaxContentWidth)
        .fillMaxWidth(),
    ) {
      if (!state.isAuthenticating) {
        Icon(
          Icons.Rounded.Add,
          contentDescription = null,
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(Res.string.action_add_campsite))
      } else {
        Text(stringResource(Res.string.label_authenticating_loading_message))
      }
    }

    Spacer(
      Modifier.imePadding(),
    )
  }
}
