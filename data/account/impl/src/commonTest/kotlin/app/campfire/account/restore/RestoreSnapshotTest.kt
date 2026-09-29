// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.restore

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class RestoreSnapshotTest {

  @Test
  fun `accounts in the database stay live`() {
    val snapshot = RestoreSnapshot().reconcile(listOf(ALICE, BOB), currentUserId = ALICE.userId)

    assertThat(snapshot.live).containsExactly(ALICE, BOB)
    assertThat(snapshot.pending).isEmpty()
    assertThat(snapshot.lastUserId).isEqualTo(ALICE.userId)
  }

  @Test
  fun `a restored snapshot against an empty database becomes pending`() {
    val restored = RestoreSnapshot(live = listOf(ALICE, BOB), lastUserId = BOB.userId)

    val snapshot = restored.reconcile(emptyList(), currentUserId = null)

    assertThat(snapshot.live).isEmpty()
    assertThat(snapshot.pending).containsExactly(ALICE, BOB)
    assertThat(snapshot.lastUserId).isEqualTo(BOB.userId)
  }

  @Test
  fun `signing back in on another scheme clears the pending account`() {
    val restored = RestoreSnapshot(pending = listOf(ALICE, BOB))
    val signedIn = ALICE.copy(serverUrl = "http://abs.example.com/")

    val snapshot = restored.reconcile(listOf(signedIn), currentUserId = ALICE.userId)

    assertThat(snapshot.live).containsExactly(signedIn)
    assertThat(snapshot.pending).containsExactly(BOB)
  }

  @Test
  fun `the same user id on another server is a different account`() {
    val restored = RestoreSnapshot(pending = listOf(ALICE))
    val otherServer = ALICE.copy(serverUrl = "https://other.example.com")

    val snapshot = restored.reconcile(listOf(otherServer), currentUserId = null)

    assertThat(snapshot.pending).containsExactly(ALICE)
  }

  @Test
  fun `a forgotten account isn't carried into pending once deleted`() {
    val snapshot = RestoreSnapshot(live = listOf(ALICE, BOB))
      .forget(ALICE.userId)
      .reconcile(listOf(BOB), currentUserId = BOB.userId)

    assertThat(snapshot.live).containsExactly(BOB)
    assertThat(snapshot.pending).isEmpty()
  }

  @Test
  fun `an account both live and pending is only pending once`() {
    val snapshot = RestoreSnapshot(live = listOf(ALICE), pending = listOf(ALICE))
      .reconcile(emptyList(), currentUserId = null)

    assertThat(snapshot.pending).containsExactly(ALICE)
  }

  @Test
  fun `dismissing removes the pending account`() {
    val snapshot = RestoreSnapshot(pending = listOf(ALICE, BOB))
      .dismiss(ALICE.asRestorableAccount())

    assertThat(snapshot.pending).containsExactly(BOB)
  }

  @Test
  fun `the last used account is offered first`() {
    val snapshot = RestoreSnapshot(pending = listOf(ALICE, BOB), lastUserId = BOB.userId)

    assertThat(snapshot.restorableAccounts()).containsExactly(
      BOB.asRestorableAccount(),
      ALICE.asRestorableAccount(),
    )
  }

  private companion object {
    val ALICE = RestoreEntry(
      serverUrl = "https://abs.example.com",
      serverName = "Home",
      userId = "user-alice",
      userName = "alice",
    )
    val BOB = RestoreEntry(
      serverUrl = "https://books.example.org",
      serverName = "Library",
      userId = "user-bob",
      userName = "bob",
    )
  }
}
