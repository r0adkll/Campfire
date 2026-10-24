// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/** The item permissions follow the server's own rules, account type and all. */
class UserItemPermissionsTest {

  private fun user(
    type: User.Type,
    update: Boolean = false,
    delete: Boolean = false,
    isActive: Boolean = true,
  ) = User(
    id = "user-1",
    name = "Listener",
    selectedLibraryId = "lib-1",
    type = type,
    isActive = isActive,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = User.Permissions(
      download = true,
      update = update,
      delete = delete,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    serverUrl = "https://abs.example.com",
  )

  @Test
  fun aUserGivenDelete_canDelete() {
    assertThat(user(User.Type.User, delete = true).canDeleteItems).isTrue()
  }

  @Test
  fun anAdminWithoutDelete_cannotDelete() {
    // The server's default for an admin: update and upload, but not delete
    assertThat(user(User.Type.Admin, update = true).canDeleteItems).isFalse()
  }

  @Test
  fun aDeactivatedAccount_cannotDeleteOrUpdate() {
    val deactivated = user(User.Type.Root, update = true, delete = true, isActive = false)

    assertThat(deactivated.canDeleteItems).isFalse()
    assertThat(deactivated.canUpdateItems).isFalse()
    assertThat(deactivated.canRescanItems).isFalse()
  }

  @Test
  fun rescanning_needsAnAdminWhoCanUpdate() {
    assertThat(user(User.Type.Admin, update = true).canRescanItems).isTrue()
    assertThat(user(User.Type.Root, update = true).canRescanItems).isTrue()
    assertThat(user(User.Type.Admin, update = false).canRescanItems).isFalse()
    assertThat(user(User.Type.User, update = true).canRescanItems).isFalse()
    assertThat(user(User.Type.Guest, update = true).canRescanItems).isFalse()
  }
}
