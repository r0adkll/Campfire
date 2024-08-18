package app.campfire.auth.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import app.campfire.auth.ui.login.composables.ServerCard
import app.campfire.auth.ui.login.composables.TitleBanner
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.ActivityScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_add_campsite
import campfire.features.auth.ui.generated.resources.label_authenticating_loading_message
import com.moriatsushi.insetsx.ExperimentalSoftwareKeyboardApi
import com.moriatsushi.insetsx.imePadding
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSoftwareKeyboardApi::class)
@CircuitInject(LoginScreen::class, ActivityScope::class)
@Composable
fun Login(
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink

  var hasFocus by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .systemBarsPadding()
      .fillMaxSize(),
  ) {
    TitleBanner(
      modifier = Modifier
        .padding(
          horizontal = 24.dp,
          vertical = 48.dp,
        ),
    )

    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
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
        connectionState = state.connectionState,
        authError = state.authError,
        isAuthenticating = state.isAuthenticating,
        modifier = Modifier.onFocusChanged {
          hasFocus = it.hasFocus
        }.widthIn(max = 500.dp)
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
          .widthIn(max = 500.dp)
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
        Modifier.imePadding()
      )
    }
  }
}
