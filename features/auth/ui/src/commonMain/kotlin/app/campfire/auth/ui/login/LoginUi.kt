package app.campfire.auth.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.composables.MaxContentWidth
import app.campfire.auth.ui.login.composables.ServerCard
import app.campfire.auth.ui.login.composables.TitleBanner
import app.campfire.auth.ui.login.settings.NetworkSettingsResult
import app.campfire.auth.ui.login.settings.showNetworkSettingsBottomSheet
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.IdBadge
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Tent
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.action_add_campsite
import campfire.features.auth.ui.generated.resources.action_login_openid
import campfire.features.auth.ui.generated.resources.label_authenticating_loading_message
import campfire.features.auth.ui.generated.resources.login_add_account_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginUiContent(
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val overlayHost = LocalOverlayHost.current
  val eventSink = state.eventSink
  var hasFocus by remember { mutableStateOf(false) }
  val authMethodState = (state.connectionState as? ConnectionState.Success)?.authMethodState

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
      networkSettings = state.networkSettings,
      onEditNetworkSettingsClick = {
        scope.launch {
          val result = overlayHost.showNetworkSettingsBottomSheet(state.networkSettings)
          if (result is NetworkSettingsResult.Success) {
            eventSink(LoginUiEvent.ChangeNetworkSettings(result.settings))
          }
        }
      },
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

    if (authMethodState?.passwordAuthEnabled == true) {
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
    } else {
      AnimatedVisibility(
        visible = state.isAuthenticating,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularWavyProgressIndicator(
            Modifier.size(32.dp),
          )
          Text(
            text = stringResource(Res.string.label_authenticating_loading_message),
            style = MaterialTheme.typography.labelLargeEmphasized,
          )
        }
      }
    }

    // Show the OIDC authentication button if available
    OpenIdAuthButton(
      authMethodState = authMethodState,
      isAuthenticating = state.isAuthenticating,
      onClick = {
        eventSink(LoginUiEvent.StartOpenIdAuth)
      },
    )

    Spacer(
      Modifier.imePadding(),
    )
  }
}

@Composable
private fun OpenIdAuthButton(
  authMethodState: AuthMethodState?,
  isAuthenticating: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = authMethodState?.openIdState != null,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column {
      // Only show the '----- OR -----' if password auth is also enabled
      if (authMethodState?.passwordAuthEnabled == true) {
        Row(
          modifier = Modifier
            .widthIn(max = 500.dp)
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HorizontalDivider(
            Modifier.weight(1f),
          )

          Text(
            text = "OR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )

          HorizontalDivider(
            Modifier.weight(1f),
          )
        }
      } else {
        Spacer(Modifier.size(8.dp))
      }

      FilledTonalButton(
        enabled = !isAuthenticating,
        onClick = onClick,
        modifier = Modifier
          .widthIn(max = 500.dp)
          .fillMaxWidth(),
      ) {
        Icon(
          CampfireIcons.Rounded.IdBadge,
          contentDescription = null,
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(authMethodState?.openIdState?.buttonText ?: stringResource(Res.string.action_login_openid))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun LoginUiPreview(
  state: LoginUiState,
  screen: LoginScreen = LoginScreen(),
) {
  PreviewSharedElementTransitionLayout {
    CampfireTheme {
      CompositionLocalProvider(
        LocalWindowSizeClass provides calculateWindowSizeClass(),
      ) {
        Login(
          screen = screen,
          state = state,
        )
      }
    }
  }
}

@Preview
@Composable
fun LoginUI_Blank() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "",
    serverUrl = "",
    userName = "",
    password = "",
    isAuthenticating = false,
    authError = null,
    connectionState = null,
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_Both_Methods() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "",
    password = "",
    isAuthenticating = false,
    authError = null,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = true,
        openIdState = OpenIdUiState(
          customMessage = "Custom message",
          buttonText = "Login with Pocket ID",
        ),
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyOIDC() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "",
    password = "",
    isAuthenticating = false,
    authError = null,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = false,
        openIdState = OpenIdUiState(
          customMessage = "Custom message",
          buttonText = "Login with Pocket ID",
        ),
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyOIDC_Failure() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "",
    password = "",
    isAuthenticating = false,
    authError = AuthError.OAuthError,
    connectionState = ConnectionState.Error(Throwable()),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyOIDC_Authenticating() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "",
    password = "",
    isAuthenticating = true,
    authError = null,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = false,
        openIdState = OpenIdUiState(
          customMessage = "Custom message",
          buttonText = "Login with Pocket ID",
        ),
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyPassword() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "Admin",
    password = "password",
    isAuthenticating = false,
    authError = null,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = true,
        openIdState = null,
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyPassword_Failure() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "Admin",
    password = "password",
    isAuthenticating = false,
    authError = AuthError.InvalidCredentials,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = true,
        openIdState = null,
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)

@Preview
@Composable
fun LoginUI_OnlyPassword_Authenticating() = LoginUiPreview(
  state = LoginUiState(
    tent = Tent.Default,
    serverName = "Campfire",
    serverUrl = "https://campfire.homelab.net",
    userName = "Admin",
    password = "password",
    isAuthenticating = true,
    authError = null,
    connectionState = ConnectionState.Success(
      authMethodState = AuthMethodState(
        passwordAuthEnabled = true,
        openIdState = null,
      ),
    ),
    networkSettings = null,
    eventSink = {},
  ),
)
