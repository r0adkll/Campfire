// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class UserCanDownloadTest {

  private fun user(download: Boolean, isActive: Boolean) = User(
    id = "user-1",
    name = "Listener",
    selectedLibraryId = "lib-1",
    type = User.Type.User,
    isActive = isActive,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = User.Permissions(
      download = download,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    serverUrl = "https://abs.example.com",
  )

  @Test
  fun activeUserWithDownloadPermission_canDownload() {
    assertThat(user(download = true, isActive = true).canDownload).isTrue()
  }

  @Test
  fun userWithoutDownloadPermission_cannotDownload() {
    assertThat(user(download = false, isActive = true).canDownload).isFalse()
  }

  @Test
  fun deactivatedUser_cannotDownload() {
    assertThat(user(download = true, isActive = false).canDownload).isFalse()
  }
}
