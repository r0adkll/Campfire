// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.blockstore

import app.campfire.account.api.AbsToken
import app.campfire.account.api.BackedUpAccount
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

class KeptAccountsTest {

  @Test
  fun `an account reads back as it was kept`() {
    val bytes = encodeKeptAccount(ACCOUNT)

    assertThat(bytes?.let(::decodeKeptAccount)).isEqualTo(ACCOUNT)
  }

  @Test
  fun `extra headers are dropped when they would overflow the entry`() {
    val account = ACCOUNT.copy(extraHeaders = mapOf("Cf-Access-Token" to "x".repeat(MAX_ENTRY_BYTES)))

    val bytes = encodeKeptAccount(account)

    assertThat(bytes).isNotNull()
    assertThat(decodeKeptAccount(bytes!!)).isEqualTo(account.copy(extraHeaders = emptyMap()))
  }

  @Test
  fun `an account too large even without headers isn't kept`() {
    val account = ACCOUNT.copy(token = AbsToken("x".repeat(MAX_ENTRY_BYTES), "refresh"))

    assertThat(encodeKeptAccount(account)).isNull()
  }

  @Test
  fun `unreadable bytes read as nothing`() {
    assertThat(decodeKeptAccount("not json".encodeToByteArray())).isNull()
  }

  @Test
  fun `keys are namespaced by user`() {
    assertThat(keyFor("user-1")).isEqualTo("campfire.account.user-1")
  }

  private companion object {
    val ACCOUNT = BackedUpAccount(
      serverUrl = "https://abs.example.com",
      serverName = "Home",
      userId = "user-1",
      token = AbsToken("access", "refresh"),
      extraHeaders = mapOf("X-Gate" to "open"),
    )
  }
}
