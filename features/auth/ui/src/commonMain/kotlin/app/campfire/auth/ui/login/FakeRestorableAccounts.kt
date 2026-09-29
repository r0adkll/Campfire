// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.account.api.RestorableAccount

/**
 * [count] placeholder accounts for trying the restored accounts UI without a real restore. The
 * samples vary in length so truncation shows up; picking one prefills an address that won't
 * connect, and dismissing one does nothing.
 */
internal fun fakeRestorableAccounts(count: Int): List<RestorableAccount> = List(count) { index ->
  val (userName, serverUrl) = FakeSamples[index % FakeSamples.size]
  val round = index / FakeSamples.size
  RestorableAccount(
    serverUrl = if (round == 0) serverUrl else "$serverUrl/$round",
    serverName = "Fake campsite ${index + 1}",
    userId = "fake-restorable-$index",
    userName = userName,
  )
}

private val FakeSamples = listOf(
  "alice" to "https://abs.example.com",
  "bob" to "http://192.168.1.50:13378",
  "the.longest.username.on.the.server" to "https://audiobookshelf.a-very-long-home-lab-domain.example.org",
  "kid" to "https://books.example.net",
  "grandpa_listens" to "http://10.0.0.2",
)
