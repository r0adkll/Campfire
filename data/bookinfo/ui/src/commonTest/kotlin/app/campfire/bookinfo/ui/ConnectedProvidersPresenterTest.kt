// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.ui

import app.campfire.bookinfo.api.AccountLinkable
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.LinkedAccount
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.api.ProviderStatus
import app.campfire.bookinfo.test.FakeBookInfoProvider
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.common.screens.ConnectedProvidersScreen
import app.campfire.common.screens.UrlScreen
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

private class FakeLinkableProvider(
  private val delegate: FakeBookInfoProvider = FakeBookInfoProvider(),
) : BookInfoProvider by delegate, AccountLinkable {

  override val linkHelpUrl: String = "https://hardcover.app/account/api"

  var linkResult: Result<LinkedAccount> = Result.success(LinkedAccount("reader"))
  val linkedTokens = mutableListOf<String>()
  var unlinked = false

  override suspend fun verifyAndLink(token: String): Result<LinkedAccount> {
    linkedTokens += token
    return linkResult
  }

  override suspend fun unlink() {
    unlinked = true
  }
}

private class FakeProviderSettings : BookInfoProviderSettings {
  val enabled = MutableStateFlow(mapOf<ProviderId, Boolean>())
  override fun isEnabled(id: ProviderId): Boolean = enabled.value[id] ?: true
  override fun setEnabled(id: ProviderId, enabled: Boolean) {
    this.enabled.value = this.enabled.value + (id to enabled)
  }
  override fun observeEnabled(id: ProviderId): Flow<Boolean> = enabled.map { it[id] ?: true }
}

class ConnectedProvidersPresenterTest {

  private val navigator = FakeNavigator(ConnectedProvidersScreen)
  private val registry = FakeBookInfoRegistry()
  private val settings = FakeProviderSettings()
  private val provider = FakeLinkableProvider()

  private val presenter = ConnectedProvidersPresenter(
    navigator = navigator,
    bookInfoRegistry = registry,
    settings = settings,
  )

  private suspend fun emitProvider(linkState: ProviderLinkState = ProviderLinkState.NotLinked) {
    registry.providersFlow.emit(
      listOf(ProviderStatus(provider = provider, enabled = true, linkState = linkState)),
    )
  }

  @Test
  fun present_MapsProviderRows() = runTest {
    emitProvider(ProviderLinkState.Linked("reader"))

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      val row = state.providers.single()
      assertThat(row.name).isEqualTo("Fake Provider")
      assertThat(row.supportsLinking).isTrue()
      assertThat(row.linkState).isEqualTo(ProviderLinkState.Linked("reader"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_ToggleEnabled_UpdatesSettings() = runTest {
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.ToggleEnabled(ProviderId.Hardcover, false))

      assertThat(settings.isEnabled(ProviderId.Hardcover)).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Link_VerifiesToken() = runTest {
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.Link(ProviderId.Hardcover, "a-token"))

      cancelAndIgnoreRemainingEvents()
    }

    assertThat(provider.linkedTokens).isEqualTo(listOf("a-token"))
  }

  @Test
  fun present_LinkFailure_FlagsRow() = runTest {
    provider.linkResult = Result.failure(Exception("nope"))
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.Link(ProviderId.Hardcover, "bad-token"))

      val failed = awaitItemMatching { it.providers.single().linkFailed }
      assertThat(failed.providers.single().isVerifying).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Unlink_DelegatesToProvider() = runTest {
    emitProvider(ProviderLinkState.Linked("reader"))

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.Unlink(ProviderId.Hardcover))

      cancelAndIgnoreRemainingEvents()
    }

    assertThat(provider.unlinked).isTrue()
  }

  @Test
  fun present_OpenLinkHelp_NavigatesToUrl() = runTest {
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.OpenLinkHelp("https://hardcover.app/account/api"))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(UrlScreen("https://hardcover.app/account/api"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_ClearCache_DelegatesToRegistry() = runTest {
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.ClearCache)

      awaitItemMatching { !it.isClearingCache && registry.clearCacheCount == 1 }
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(registry.clearCacheCount).isEqualTo(1)
  }

  @Test
  fun present_Back_PopsNavigator() = runTest {
    emitProvider()

    presenter.test {
      skipItems(1)
      val state = awaitItem()

      state.eventSink(ConnectedProvidersUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend inline fun app.cash.turbine.ReceiveTurbine<ConnectedProvidersUiState>.awaitItemMatching(
  predicate: (ConnectedProvidersUiState) -> Boolean,
): ConnectedProvidersUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
