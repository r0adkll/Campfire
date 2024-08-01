package app.campfire.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.ui.login.LoginUiEvent.AddCampsite
import app.campfire.auth.ui.login.LoginUiEvent.ChangeTent
import app.campfire.auth.ui.login.LoginUiEvent.Password
import app.campfire.auth.ui.login.LoginUiEvent.ServerName
import app.campfire.auth.ui.login.LoginUiEvent.ServerUrl
import app.campfire.auth.ui.login.LoginUiEvent.UserName
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.ActivityScope
import app.campfire.core.model.Tent
import coil3.toUri
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ActivityScope::class, LoginScreen::class)
@Inject
class LoginPresenter(
  @Assisted private val navigator: Navigator,
  private val authRepository: AuthRepository,
) : Presenter<LoginUiState> {

  @Composable
  override fun present(): LoginUiState {
    var tent by remember { mutableStateOf(Tent.Red) }
    var serverName by remember { mutableStateOf("Add campsite") }
    var serverUrl by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var passWord by remember { mutableStateOf("") }

    return LoginUiState(
      tent = tent,
      serverName = serverName,
      serverUrl = serverUrl,
      userName = userName,
      password = passWord,
      connectionState = connectionState(serverUrl)
    ) { event ->
      when (event) {
        is ChangeTent -> tent = event.tent
        is UserName -> userName = event.userName
        is Password -> passWord = event.password
        is ServerName -> serverName = event.serverName
        is ServerUrl -> serverUrl = event.url
        is AddCampsite -> {

        }
      }
    }
  }

  @Composable
  private fun connectionState(serverUrl: String): ConnectionState? {
    var connectionState by remember { mutableStateOf<ConnectionState?>(null) }

    LaunchedEffect(serverUrl) {
      connectionState = ConnectionState.Loading

      val uri = serverUrl.toUri()
      if (uri.scheme == null || uri.authority == null) {
        connectionState = ConnectionState.Error
        return@LaunchedEffect
      }

      connectionState = if (authRepository.healthCheck(serverUrl)) {
        ConnectionState.Success
      } else {
        ConnectionState.Error
      }
    }

    return connectionState
  }
}
