package app.campfire.auth.ui.login

import Campfire.features.auth.ui.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.ui.login.LoginUiEvent.AddCampsite
import app.campfire.auth.ui.login.LoginUiEvent.ChangeTent
import app.campfire.auth.ui.login.LoginUiEvent.Password
import app.campfire.auth.ui.login.LoginUiEvent.ServerName
import app.campfire.auth.ui.login.LoginUiEvent.ServerUrl
import app.campfire.auth.ui.login.LoginUiEvent.UserName
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.capitalized
import app.campfire.core.model.Tent
import coil3.toUri
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import okio.IOException

@CircuitInject(LoginScreen::class, UserScope::class)
@Inject
class LoginPresenter(
  @Assisted private val navigator: Navigator,
  private val authRepository: AuthRepository,
) : Presenter<LoginUiState> {

  @Composable
  override fun present(): LoginUiState {
    val coroutineScope = rememberCoroutineScope()

    var tent by remember { mutableStateOf(Tent.Default) }
    var serverName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(BuildConfig.TEST_SERVER_URL ?: "") }
    var username by remember { mutableStateOf(BuildConfig.TEST_USERNAME ?: "") }
    var password by remember { mutableStateOf(BuildConfig.TEST_PASSWORD ?: "") }

    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<AuthError?>(null) }

    val connectionState = connectionState(serverUrl)

    LaunchedEffect(serverUrl, connectionState) {
      serverUrl.toUri().authority?.split('.')?.firstOrNull()?.let {
        if (serverName.isBlank() && connectionState == ConnectionState.Success) {
          serverName = it.capitalized()
        }
      }
    }

    return LoginUiState(
      tent = tent,
      serverName = serverName,
      serverUrl = serverUrl,
      userName = username,
      password = password,
      isAuthenticating = isAuthenticating,
      authError = authError,
      connectionState = connectionState,
    ) { event ->
      when (event) {
        is ChangeTent -> tent = event.tent
        is UserName -> username = event.userName
        is Password -> password = event.password
        is ServerName -> serverName = event.serverName
        is ServerUrl -> serverUrl = event.url
        is AddCampsite -> {
          // Validate that we can actually add a campsite
          if (
            connectionState != ConnectionState.Success ||
            username.isBlank() ||
            password.isBlank()
          ) {
            return@LoginUiState
          }

          isAuthenticating = true
          authError = null
          coroutineScope.launch {
            authRepository.authenticate(
              serverUrl = serverUrl,
              serverName = serverName,
              username = username,
              password = password,
              tent = tent,
            ).onFailure {
              isAuthenticating = false
              authError = when (it.cause) {
                is IOException -> AuthError.NetworkError
                else -> AuthError.InvalidCredentials
              }
            }
          }
        }
      }
    }
  }

  @Composable
  private fun connectionState(serverUrl: String): ConnectionState? {
    var connectionState by remember { mutableStateOf<ConnectionState?>(null) }

    LaunchedEffect(serverUrl) {
      connectionState = ConnectionState.Loading

      // Give the user some time to type the URL
      delay(PING_DELAY)

      val uri = serverUrl.toUri()
      if (uri.scheme == null || uri.authority == null) {
        connectionState = ConnectionState.Error
        return@LaunchedEffect
      }

      val result = try {
        authRepository.ping(serverUrl)
      } catch (e: Exception) {
        false
      }
      connectionState = if (result) {
        ConnectionState.Success
      } else {
        ConnectionState.Error
      }
    }

    return connectionState
  }
}

private const val PING_DELAY = 500L
