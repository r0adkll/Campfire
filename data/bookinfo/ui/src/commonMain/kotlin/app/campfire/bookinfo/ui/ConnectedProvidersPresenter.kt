// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.bookinfo.api.AccountLinkable
import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.ProviderId
import app.campfire.common.screens.ConnectedProvidersScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ConnectedProvidersScreen::class, UserScope::class)
@Inject
class ConnectedProvidersPresenter(
  @Assisted private val navigator: Navigator,
  private val bookInfoRegistry: BookInfoRegistry,
  private val settings: BookInfoProviderSettings,
) : Presenter<ConnectedProvidersUiState> {

  @Composable
  override fun present(): ConnectedProvidersUiState {
    val scope = rememberCoroutineScope()

    val statuses by remember {
      bookInfoRegistry.observeProviders()
    }.collectAsState(emptyList())

    val preferredProvider by remember {
      settings.observePreferredProvider()
    }.collectAsState(settings.preferredProvider())

    var verifyingId by remember { mutableStateOf<ProviderId?>(null) }
    var failedId by remember { mutableStateOf<ProviderId?>(null) }
    var clearingCache by remember { mutableStateOf(false) }

    val rows = statuses.map { status ->
      val linkable = status.provider as? AccountLinkable
      ProviderRowState(
        id = status.provider.id,
        name = status.provider.displayName,
        capabilities = status.provider.capabilities,
        enabled = status.enabled,
        linkState = status.linkState,
        supportsLinking = linkable != null,
        linkHelpUrl = linkable?.linkHelpUrl,
        isVerifying = verifyingId == status.provider.id,
        linkFailed = failedId == status.provider.id,
      )
    }

    return ConnectedProvidersUiState(
      providers = rows,
      preferredProvider = preferredProvider,
      isClearingCache = clearingCache,
    ) { event ->
      when (event) {
        ConnectedProvidersUiEvent.Back -> navigator.pop()

        is ConnectedProvidersUiEvent.SetPreferredProvider -> {
          settings.setPreferredProvider(event.id)
        }

        ConnectedProvidersUiEvent.ClearCache -> {
          if (!clearingCache) {
            clearingCache = true
            scope.launch {
              bookInfoRegistry.clearCache()
              clearingCache = false
            }
          }
        }

        is ConnectedProvidersUiEvent.ToggleEnabled -> {
          settings.setEnabled(event.id, event.enabled)
        }

        is ConnectedProvidersUiEvent.Link -> {
          val linkable = statuses
            .firstOrNull { it.provider.id == event.id }
            ?.provider as? AccountLinkable
            ?: return@ConnectedProvidersUiState
          verifyingId = event.id
          failedId = null
          scope.launch {
            val result = linkable.verifyAndLink(event.token)
            verifyingId = null
            if (result.isFailure) {
              failedId = event.id
            }
          }
        }

        is ConnectedProvidersUiEvent.Unlink -> {
          val linkable = statuses
            .firstOrNull { it.provider.id == event.id }
            ?.provider as? AccountLinkable
            ?: return@ConnectedProvidersUiState
          failedId = null
          scope.launch { linkable.unlink() }
        }

        is ConnectedProvidersUiEvent.OpenLinkHelp -> {
          navigator.goTo(UrlScreen(event.url))
        }
      }
    }
  }
}
