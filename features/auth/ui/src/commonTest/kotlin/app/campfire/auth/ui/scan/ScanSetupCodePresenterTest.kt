// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.auth.api.screen.ScanSetupCodeScreen
import app.campfire.auth.ui.login.FakeConnectLinkInbox
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ScanSetupCodePresenterTest {

  private val navigator = FakeNavigator(ScanSetupCodeScreen)
  private val inbox = FakeConnectLinkInbox()

  @Test
  fun `a setup link replaces the scanner with its confirmation`() = runTest {
    presenter().test {
      awaitItem().eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))

      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links.keys).containsOnly(LINK_ID)
    assertThat(inbox.links.getValue(LINK_ID)).isEqualTo(ConnectLink.parseUrl(SETUP_LINK))
  }

  @Test
  fun `the android fallback form is a setup link too`() = runTest {
    presenter().test {
      awaitItem().eventSink(ScanSetupCodeUiEvent.Scanned("campfireaudiobooks://connect?$FIELDS"))

      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links.getValue(LINK_ID)).isInstanceOf<ConnectLinkResult.Valid>()
  }

  @Test
  fun `a broken setup link is confirmed like an opened one, which explains it`() = runTest {
    presenter().test {
      awaitItem().eventSink(ScanSetupCodeUiEvent.Scanned("$SETUP_LINK_BASE#v=2&server=x"))

      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links.getValue(LINK_ID)).isEqualTo(ConnectLinkResult.UnsupportedVersion)
  }

  @Test
  fun `only the first setup link is used`() = runTest {
    var nextId = LINK_ID
    presenter(nextLinkId = { nextId++ }).test {
      val state = awaitItem()
      state.eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))
      state.eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))

      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      navigator.expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links.keys).containsOnly(LINK_ID)
  }

  @Test
  fun `another qr code shows a hint for a moment and scanning carries on`() = runTest {
    presenter().test {
      val initial = awaitItem()
      assertThat(initial.sawOtherCode).isFalse()

      initial.eventSink(ScanSetupCodeUiEvent.Scanned("https://example.com/menu"))
      assertThat(awaitItem().sawOtherCode).isTrue()
      assertThat(awaitItem().sawOtherCode).isFalse()

      navigator.expectNoEvents()
      initial.eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))
      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the guide shows the code was read before the confirmation opens`() = runTest {
    presenter().test {
      val initial = awaitItem()
      assertThat(initial.captured).isFalse()

      initial.eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))
      assertThat(awaitItem().captured).isTrue()
      assertThat(inbox.links).isEmpty()

      navigator.awaitPop()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ConnectScreen(LINK_ID))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `closing while the code is shown as read keeps nothing`() = runTest {
    presenter().test {
      val initial = awaitItem()
      initial.eventSink(ScanSetupCodeUiEvent.Scanned(SETUP_LINK))
      awaitItem().eventSink(ScanSetupCodeUiEvent.Close)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    navigator.expectNoEvents()
    assertThat(inbox.links).isEmpty()
  }

  @Test
  fun `closing leaves without keeping anything`() = runTest {
    presenter().test {
      awaitItem().eventSink(ScanSetupCodeUiEvent.Close)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(inbox.links).isEmpty()
  }

  @Test
  fun `a scanned code is kept out of logs`() {
    assertThat(ScanSetupCodeUiEvent.Scanned(SETUP_LINK).toString()).doesNotContain("secret")
  }

  private fun presenter(nextLinkId: () -> Long = { LINK_ID }) = ScanSetupCodePresenter(
    navigator = navigator,
    connectLinkInbox = inbox,
  ).apply {
    this.nextLinkId = nextLinkId
  }

  private companion object {
    const val LINK_ID = 7L
    const val SETUP_LINK_BASE = "https://thescavengers.software/campfire/connect"
    const val FIELDS = "v=1&server=https%3A%2F%2Fabs.firenation.app&name=Fire+Nation&auth=openid" +
      "&id=0123456789abcdef.access&secret=0123456789abcdef0123456789abcdef"
    const val SETUP_LINK = "$SETUP_LINK_BASE#$FIELDS"
  }
}
