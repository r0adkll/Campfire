// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.PasswordCredentials
import app.campfire.auth.api.model.AUTH_METHOD_LOCAL
import app.campfire.auth.api.model.AUTH_METHOD_OPENID
import app.campfire.auth.ui.BuildConfig
import app.campfire.auth.ui.login.LoginUiEvent.AddCampsite
import app.campfire.auth.ui.login.LoginUiEvent.ChangeNetworkSettings
import app.campfire.auth.ui.login.LoginUiEvent.ChangeTheme
import app.campfire.auth.ui.login.LoginUiEvent.DismissRestorableAccount
import app.campfire.auth.ui.login.LoginUiEvent.NavigateBack
import app.campfire.auth.ui.login.LoginUiEvent.Password
import app.campfire.auth.ui.login.LoginUiEvent.SelectRestorableAccount
import app.campfire.auth.ui.login.LoginUiEvent.ServerName
import app.campfire.auth.ui.login.LoginUiEvent.ServerUrl
import app.campfire.auth.ui.login.LoginUiEvent.UserName
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.capitalized
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.UserId
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import coil3.toUri
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.IOException

@CircuitInject(LoginScreen::class, UserScope::class)
@Inject
class LoginPresenter(
  private val screen: LoginScreen,
  private val navigator: Navigator,
  private val authRepository: AuthRepository,
  private val oauthAuthorizationFlow: AuthorizationFlow,
  private val localNetworkPermission: LocalNetworkPermissionController,
  private val appThemeRepository: AppThemeRepository,
  private val restorableAccountRepository: RestorableAccountRepository,
  private val passwordCredentials: PasswordCredentials,
) : Presenter<LoginUiState> {

  private val initialServerName: String = when (screen) {
    is LoginScreen.ReAuthentication -> screen.serverName
    is LoginScreen.Restore -> screen.serverName
    else -> ""
  }

  private val initialServerUrl: String = when (screen) {
    is LoginScreen.ReAuthentication -> screen.serverUrl
    is LoginScreen.Restore -> screen.serverUrl
    is LoginScreen.Additional -> ""
    else -> BuildConfig.TEST_SERVER_URL ?: ""
  }

  private val initialUserName: String = when (screen) {
    is LoginScreen.ReAuthentication -> screen.userName
    is LoginScreen.Restore -> screen.userName
    is LoginScreen.Additional -> ""
    else -> BuildConfig.TEST_USERNAME ?: ""
  }

  private val initialPassword: String = when (screen) {
    is LoginScreen.ReAuthentication -> ""
    is LoginScreen.Restore -> ""
    is LoginScreen.Additional -> ""
    else -> BuildConfig.TEST_PASSWORD ?: ""
  }

  /**
   * Placeholder accounts appended to the real restorable ones, for trying the UI without a
   * restore. Set with the `campfire_fake_restorable_accounts` Gradle property.
   */
  internal var fakeRestorableAccountCount: Int = BuildConfig.FAKE_RESTORABLE_ACCOUNTS

  private val existingUserId: UserId?
    get() = when (screen) {
      is LoginScreen.ReAuthentication -> screen.userId
      else -> null
    }

  @Composable
  override fun present(): LoginUiState {
    val coroutineScope = rememberCoroutineScope()

    var theme by remember { mutableStateOf<AppTheme.Fixed>(AppTheme.Fixed.Tent) }
    var serverName by remember { mutableStateOf(initialServerName) }
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var networkSettings by remember { mutableStateOf<NetworkSettings?>(null) }
    var username by remember { mutableStateOf(initialUserName) }
    var password by remember { mutableStateOf(initialPassword) }

    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<AuthError?>(null) }
    var isRestoringAccount by remember { mutableStateOf(screen is LoginScreen.Restore) }
    // A password from the password manager is already saved there, so don't offer to save it
    var isSavedPassword by remember { mutableStateOf(false) }
    val lookedUpAccounts = remember { mutableSetOf<Pair<String, String>>() }

    // Accounts from before a reinstall, offered to prefill the form. Re-authentication already
    // knows which account it's for.
    val restorableAccounts by remember {
      if (screen is LoginScreen.ReAuthentication) {
        flowOf(emptyList())
      } else {
        restorableAccountRepository.observeRestorableAccounts()
          .map { it + fakeRestorableAccounts(fakeRestorableAccountCount) }
      }
    }.collectAsState(emptyList())
    val restoredTheme by appThemeRepository.observeCurrentAppTheme().collectAsState()

    // If we are re-authenticating, be sure to load the existing extra headers
    // from the account manager.
    LaunchedEffect(screen) {
      if (screen is LoginScreen.ReAuthentication) {
        networkSettings = authRepository.getNetworkSettings(screen.userId)
      }
    }

    // Clear any auth errors if the inputs change
    LaunchedEffect(serverUrl, username, password) {
      if (authError != null) {
        authError = null
      }
    }

    val connectionState = connectionState(
      serverUrl = serverUrl,
      networkSettings = networkSettings,
      onUrlResolved = { serverUrl = it },
    )

    LaunchedEffect(serverUrl, connectionState) {
      serverUrl.toUri().authority?.split('.')?.firstOrNull()?.let {
        if (serverName.isBlank() && connectionState is ConnectionState.Success) {
          serverName = it.capitalized()
        }
      }
    }

    val signInWithPassword: () -> Unit = signIn@{
      // Validate that we can actually add a campsite
      if (
        connectionState !is ConnectionState.Success ||
        username.isBlank() ||
        password.isBlank()
      ) {
        return@signIn
      }

      isAuthenticating = true
      authError = null
      val offerToSave = !isSavedPassword
      coroutineScope.launch {
        authRepository.authenticate(
          serverUrl = serverUrl,
          serverName = serverName,
          username = username,
          password = password,
          userId = existingUserId,
          networkSettings = networkSettings,
        ).onSuccess {
          applySelectedTheme(theme, isRestoringAccount)
          if (offerToSave) passwordCredentials.offerToSave(serverUrl, username, password)
        }.onFailure {
          isAuthenticating = false
          authError = it.asAuthError()
        }
      }
    }

    // Signing back into an account we know, once its server is found, offer the password the user
    // saved for it. Picking one signs in, since everything else is already filled in.
    val knownAccount = if (isRestoringAccount || screen is LoginScreen.ReAuthentication) {
      serverUrl to username
    } else {
      null
    }
    LaunchedEffect(connectionState, knownAccount) {
      val authMethodState = (connectionState as? ConnectionState.Success)?.authMethodState
      if (
        knownAccount == null ||
        authMethodState?.passwordAuthEnabled != true ||
        password.isNotEmpty() ||
        // Ask once per account, not every time the server is found again
        !lookedUpAccounts.add(knownAccount)
      ) {
        return@LaunchedEffect
      }

      val savedPassword = passwordCredentials.find(knownAccount.first, knownAccount.second)
      if (savedPassword != null) {
        password = savedPassword
        isSavedPassword = true
        signInWithPassword()
      }
    }

    return LoginUiState(
      theme = theme,
      serverName = serverName,
      serverUrl = serverUrl,
      userName = username,
      password = password,
      isAuthenticating = isAuthenticating,
      authError = authError,
      connectionState = connectionState,
      networkSettings = networkSettings,
      restorableAccounts = restorableAccounts,
      restoredTheme = restoredTheme,
      focusPassword = isRestoringAccount && password.isEmpty(),
    ) { event ->
      when (event) {
        NavigateBack -> navigator.pop()

        is ChangeTheme -> theme = event.theme
        is ChangeNetworkSettings -> networkSettings = event.settings
        is UserName -> username = event.userName
        is Password -> {
          password = event.password
          isSavedPassword = false
        }
        is ServerName -> serverName = event.serverName
        is ServerUrl -> serverUrl = event.url

        is SelectRestorableAccount -> {
          serverName = event.account.serverName
          serverUrl = event.account.serverUrl
          username = event.account.userName
          password = ""
          isRestoringAccount = true
        }

        is DismissRestorableAccount -> coroutineScope.launch {
          restorableAccountRepository.dismiss(event.account)
        }

        is AddCampsite -> signInWithPassword()

        is LoginUiEvent.StartOpenIdAuth -> {
          isAuthenticating = true
          authError = null
          coroutineScope.launch {
            oauthAuthorizationFlow.getAuthorization(serverUrl, networkSettings?.extraHeaders)
              .onSuccess { authorization ->
                authRepository.authenticate(
                  serverUrl = serverUrl,
                  serverName = serverName,
                  codeVerifier = authorization.codeVerifier,
                  code = authorization.code,
                  state = authorization.state,
                  userId = existingUserId,
                  networkSettings = networkSettings,
                ).onSuccess {
                  applySelectedTheme(theme, isRestoringAccount)
                }.onFailure { e ->
                  isAuthenticating = false
                  authError = e.asAuthError()
                }
              }
              .onFailure {
                isAuthenticating = false
                authError = it.toOAuthAuthError()
              }
          }
        }
      }
    }
  }

  /**
   * A fresh login treats the picked default theme as the user's starting app theme.
   * Re-authentication must not clobber whatever theme (possibly custom/AI) they already use, and
   * neither must signing back into a restored account, whose theme came back with the backup.
   */
  private fun applySelectedTheme(theme: AppTheme.Fixed, isRestoringAccount: Boolean) {
    if (screen !is LoginScreen.ReAuthentication && !isRestoringAccount) {
      appThemeRepository.setCurrentTheme(theme)
    }
  }

  /**
   * Probes [serverUrl] for a reachable Audiobookshelf server. Scheme-less input (e.g.
   * `192.168.1.50:13378` or `abs.example.com`) is probed with both schemes via
   * [serverUrlProbeCandidates]; when one connects, [onUrlResolved] is invoked with the
   * full URL so the field reflects the scheme that actually worked.
   */
  @Composable
  private fun connectionState(
    serverUrl: String,
    networkSettings: NetworkSettings?,
    onUrlResolved: (String) -> Unit,
  ): ConnectionState? {
    var connectionState by remember { mutableStateOf<ConnectionState?>(null) }
    var autoResolvedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(serverUrl, networkSettings) {
      if (serverUrl.isBlank()) {
        connectionState = null
        return@LaunchedEffect
      }

      // The field was just rewritten with the scheme that connected; the current Success
      // state already belongs to this exact URL, so don't probe (and flash Loading) again.
      if (serverUrl == autoResolvedUrl && connectionState is ConnectionState.Success) {
        return@LaunchedEffect
      }
      autoResolvedUrl = null

      connectionState = ConnectionState.Loading

      // Give the user some time to type the URL
      delay(PING_DELAY)

      val candidates = serverUrlProbeCandidates(serverUrl)
        .filter { candidate ->
          val uri = candidate.toUri()
          uri.scheme != null && uri.authority != null
        }
      if (candidates.isEmpty()) {
        connectionState = ConnectionState.Error(IllegalArgumentException("Invalid URL"))
        return@LaunchedEffect
      }

      // A LAN server (e.g. 192.168.x.x) needs the local-network permission on Android 17+, or the
      // status probe below silently times out. Prompt lazily now that a private address is present.
      localNetworkPermission.requestIfNeeded(candidates.first())

      var lastError: Throwable? = null
      for (candidate in candidates) {
        val result = authRepository.status(candidate, networkSettings)
        val status = result.getOrNull()
        if (status != null) {
          connectionState = ConnectionState.Success(
            AuthMethodState(
              passwordAuthEnabled = status.authMethods.contains(AUTH_METHOD_LOCAL),
              openIdState = if (status.authMethods.contains(AUTH_METHOD_OPENID)) {
                OpenIdUiState(
                  customMessage = status.authFormData?.customMessage,
                  buttonText = status.authFormData?.openIdButtonText,
                )
              } else {
                null
              },
            ),
          )
          if (candidate != serverUrl) {
            autoResolvedUrl = candidate
            onUrlResolved(candidate)
          }
          return@LaunchedEffect
        }
        lastError = result.exceptionOrNull()
      }

      connectionState = ConnectionState.Error(
        lastError ?: IllegalArgumentException("Unable to connect"),
      )
    }

    return connectionState
  }
}

private fun Throwable.asAuthError(): AuthError = when (this) {
  is AuthException.InvalidCredentials -> AuthError.InvalidCredentials
  is AuthException.NoAccessibleLibraries -> AuthError.NoLibraryAccess
  is AuthException.Network -> AuthError.NetworkError
  is AuthException.UnexpectedResponse -> AuthError.UnexpectedResponse
  // Fallbacks for errors thrown outside of AuthRepository's typed mapping
  is IOException -> AuthError.NetworkError
  else -> if (cause is IOException) AuthError.NetworkError else AuthError.UnexpectedResponse
}

private fun Throwable.toOAuthAuthError(): AuthError {
  val msg = message.orEmpty()
  return when {
    msg.contains("invalid redirect_uri", ignoreCase = true) -> AuthError.OAuthInvalidRedirectUri
    else -> AuthError.OAuthError
  }
}

private const val PING_DELAY = 500L
