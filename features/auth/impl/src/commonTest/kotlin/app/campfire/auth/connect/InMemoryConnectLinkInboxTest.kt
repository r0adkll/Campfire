// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.connect

import app.campfire.core.logging.LogRedaction
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.Test

class InMemoryConnectLinkInboxTest {

  private val inbox = InMemoryConnectLinkInbox()

  @AfterTest
  fun teardown() {
    LogRedaction.clear()
  }

  @Test
  fun `a delivered link is held until it's discarded`() {
    assertThat(inbox.deliver(1L, LINK)).isTrue()
    assertThat(inbox[1L]).isEqualTo(LINK)

    inbox.discard(1L)
    assertThat(inbox[1L]).isNull()
  }

  @Test
  fun `a link is only delivered once`() {
    inbox.deliver(1L, LINK)
    inbox.discard(1L)

    assertThat(inbox.deliver(1L, LINK)).isFalse()
    assertThat(inbox[1L]).isNull()
  }

  @Test
  fun `a newer link replaces the one waiting`() {
    inbox.deliver(1L, LINK)
    inbox.deliver(2L, ConnectLinkResult.UnsupportedVersion)

    assertThat(inbox[1L]).isNull()
    assertThat(inbox[2L]).isEqualTo(ConnectLinkResult.UnsupportedVersion)
    // Discarding the replaced link leaves the newer one alone
    inbox.discard(1L)
    assertThat(inbox[2L]).isEqualTo(ConnectLinkResult.UnsupportedVersion)
  }

  @Test
  fun `the service token is redacted from logs`() {
    LogRedaction.enabled = true
    LogRedaction.clear()

    inbox.deliver(1L, LINK)

    assertThat(LogRedaction.redact("secret ${LINK.link.clientSecret}")).isEqualTo("secret <secret>")
    assertThat(LogRedaction.redact("id ${LINK.link.clientId}")).isEqualTo("id <secret>")
  }

  private companion object {
    val LINK = ConnectLinkResult.Valid(
      ConnectLink(
        serverUrl = "https://abs.example.com",
        serverName = "Home",
        clientId = "0123456789abcdef.access",
        clientSecret = "0123456789abcdef0123456789abcdef",
        signInWithOpenId = false,
      ),
    )
  }
}
