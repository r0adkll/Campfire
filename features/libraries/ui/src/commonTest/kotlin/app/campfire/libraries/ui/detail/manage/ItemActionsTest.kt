// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import app.campfire.common.test.user
import app.campfire.core.model.User
import app.campfire.core.model.User.Permissions
import app.campfire.home.ui.libraryItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

/** What each kind of account is offered, against what the server would let it do. */
class ItemActionsTest {

  private val item = libraryItem().copy(isFile = false)

  @Test
  fun anAdminWithTheServersDefaults_canRescanButNotDelete() {
    val admin = account(User.Type.Admin, update = true, delete = false)

    assertThat(admin.itemActions(item)).containsExactly(ItemAction.Rescan)
  }

  @Test
  fun theRootAccount_canDoBoth() {
    val root = account(User.Type.Root, update = true, delete = true)

    assertThat(root.itemActions(item)).containsExactly(ItemAction.Rescan, ItemAction.Delete)
  }

  @Test
  fun aUserGivenDelete_canDeleteButNotRescan() {
    val listener = account(User.Type.User, update = true, delete = true)

    assertThat(listener.itemActions(item)).containsExactly(ItemAction.Delete)
  }

  @Test
  fun aUserWithNothingGranted_isOfferedNothing() {
    assertThat(account(User.Type.User).itemActions(item)).isEmpty()
    assertThat(account(User.Type.Guest).itemActions(item)).isEmpty()
  }

  @Test
  fun aDeactivatedAccount_isOfferedNothing() {
    val deactivated = account(User.Type.Root, update = true, delete = true, isActive = false)

    assertThat(deactivated.itemActions(item)).isEmpty()
  }

  @Test
  fun anItemThatIsASingleFile_cantBeRescanned() {
    val root = account(User.Type.Root, update = true, delete = true)

    assertThat(root.itemActions(item.copy(isFile = true))).containsExactly(ItemAction.Delete)
  }

  private fun account(
    type: User.Type,
    update: Boolean = false,
    delete: Boolean = false,
    isActive: Boolean = true,
  ) = user(
    id = "user-1",
    type = type,
    isActive = isActive,
    permissions = Permissions(
      download = true,
      update = update,
      delete = delete,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
  )
}
