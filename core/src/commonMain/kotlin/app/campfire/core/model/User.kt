// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.logging.loggableUrl

typealias UserId = String

data class User(
  val id: UserId,
  val name: String,
  val selectedLibraryId: LibraryId,
  val type: Type,
  val isActive: Boolean,
  val isLocked: Boolean,
  val lastSeen: Long,
  val createdAt: Long,
  val permissions: Permissions,
  val serverUrl: String,
) {

  // Deliberately redacts serverUrl — User objects get interpolated into logs that ship
  // to crash reporting as breadcrumbs.
  override fun toString(): String =
    "User(id=$id, name=$name, selectedLibraryId=$selectedLibraryId, type=$type, " +
      "isActive=$isActive, isLocked=$isLocked, lastSeen=$lastSeen, createdAt=$createdAt, " +
      "permissions=$permissions, serverUrl=${serverUrl.loggableUrl})"

  val canEditCollections: Boolean
    get() = type == Type.Admin || type == Type.Root

  /** Mirrors the server's `User.isAdminOrUp`. */
  val isAdminOrUp: Boolean
    get() = type == Type.Admin || type == Type.Root

  /**
   * Mirrors the server's `User.canDelete`, which any account can be given: an admin doesn't have
   * it unless granted, and a regular user can be.
   */
  val canDeleteItems: Boolean
    get() = permissions.delete && isActive

  /** Mirrors the server's `User.canUpdate`, needed to change an item. */
  val canUpdateItems: Boolean
    get() = permissions.update && isActive

  /** Mirrors the server's `User.canUpload`, which giving an item a cover from an address takes too. */
  val canUploadFiles: Boolean
    get() = permissions.upload && isActive

  /**
   * Whether the server will scan an item's files again for this account: an admin route that the
   * item routes also hold to the update permission.
   */
  val canRescanItems: Boolean
    get() = isAdminOrUp && canUpdateItems

  /** Mirrors the server's `User.canDownload`, which also refuses downloads to a deactivated user. */
  val canDownload: Boolean
    get() = permissions.download && isActive

  enum class Type {
    Root,
    Guest,
    User,
    Admin,
    ;

    companion object {
      fun from(value: String): Type {
        return when (value.lowercase()) {
          "root" -> Root
          "guest" -> Guest
          "user" -> User
          "admin" -> Admin
          else -> throw IllegalArgumentException("Unknown user type: $value")
        }
      }
    }
  }

  data class Permissions(
    val download: Boolean,
    val update: Boolean,
    val delete: Boolean,
    val upload: Boolean,
    val accessAllLibraries: Boolean,
    val accessAllTags: Boolean,
    val accessExplicitContent: Boolean,
  )
}
