// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.account.api.AbsToken
import app.campfire.audioplayer.impl.offline.FakeAccountManager
import app.campfire.audioplayer.impl.offline.FakeUserSessionManager
import app.campfire.audioplayer.impl.offline.downloadingUser
import app.campfire.core.session.UserSession
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ServerRequestHeadersTest {

  private val accountManager = FakeAccountManager().apply {
    token = AbsToken(accessToken = "abc123", refreshToken = null)
    extraHeaders = mapOf("CF-Access-Client-Id" to "id", "CF-Access-Client-Secret" to "secret")
  }
  private val userSessionManager = FakeUserSessionManager(UserSession.LoggedIn(downloadingUser))
  private val headers = ServerRequestHeaders(accountManager, userSessionManager)

  @Test
  fun `a url on the user's server gets the token and the extra headers`() = runTest {
    assertThat(headers.forUrl(downloadingUser.id, "https://abs.example.com/api/items/i/file/1")).isEqualTo(
      mapOf(
        "CF-Access-Client-Id" to "id",
        "CF-Access-Client-Secret" to "secret",
        "Authorization" to "Bearer abc123",
      ),
    )
  }

  @Test
  fun `host matching ignores case and an explicit default port`() = runTest {
    assertThat(headers.forUrl(downloadingUser.id, "https://ABS.example.com:443/api/items/i/cover").size)
      .isEqualTo(3)
  }

  @Test
  fun `a server url without a scheme is treated as https`() = runTest {
    userSessionManager.current = UserSession.LoggedIn(downloadingUser.copy(serverUrl = "abs.example.com/"))

    assertThat(headers.forUrl(downloadingUser.id, "https://abs.example.com/api/items/i/file/1").size)
      .isEqualTo(3)
  }

  @Test
  fun `a url on another host gets nothing`() = runTest {
    assertThat(headers.forUrl(downloadingUser.id, "https://covers.example.org/cover.jpg")).isEmpty()
  }

  @Test
  fun `a url on another port of the same host gets nothing`() = runTest {
    assertThat(headers.forUrl(downloadingUser.id, "https://abs.example.com:8443/api/items/i/file/1")).isEmpty()
  }

  @Test
  fun `a user other than the logged in one gets nothing`() = runTest {
    assertThat(headers.forUrl("someone-else", "https://abs.example.com/api/items/i/file/1")).isEmpty()
  }

  @Test
  fun `nothing is sent once logged out`() = runTest {
    userSessionManager.current = UserSession.LoggedOut

    assertThat(headers.forUrl(downloadingUser.id, "https://abs.example.com/api/items/i/file/1")).isEmpty()
  }

  @Test
  fun `an account without a token still sends its extra headers`() = runTest {
    accountManager.token = null

    assertThat(headers.forUrl(downloadingUser.id, "https://abs.example.com/api/items/i/file/1")).isEqualTo(
      mapOf("CF-Access-Client-Id" to "id", "CF-Access-Client-Secret" to "secret"),
    )
  }
}
