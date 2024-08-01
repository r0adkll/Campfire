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
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.login.composables.ServerCard
import app.campfire.auth.ui.login.composables.TitleBanner
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.ActivityScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_add_campsite
import com.moriatsushi.insetsx.ExperimentalSoftwareKeyboardApi
import com.moriatsushi.insetsx.imePadding
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSoftwareKeyboardApi::class)
@CircuitInject(ActivityScope::class, LoginScreen::class)
@Composable
fun Login(
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {
  val eventSink = state.eventSink

  Box(
    modifier = modifier
      .systemBarsPadding()
      .fillMaxSize(),
  ) {
    TitleBanner(
      modifier = Modifier
        .padding(
          horizontal = 24.dp,
          vertical = 24.dp,
        ),
    )

    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
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
      )

      Spacer(Modifier.height(16.dp))

      Button(
        enabled = state.serverUrl.isNotBlank() &&
          state.userName.isNotBlank() &&
          state.password.isNotBlank(),
        onClick = {
          eventSink(LoginUiEvent.AddCampsite)
        },
        modifier = Modifier
          .fillMaxWidth(),
      ) {
        Icon(
          Icons.Rounded.Add,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(Res.string.action_add_campsite))
      }

      Spacer(
        Modifier.imePadding()
      )
    }
  }
}
