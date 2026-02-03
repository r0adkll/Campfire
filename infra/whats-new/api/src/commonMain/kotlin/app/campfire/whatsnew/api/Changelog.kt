package app.campfire.whatsnew.api

import kotlinx.serialization.Serializable

/**
 * Represents the full changelog containing all version changes.
 */
@Serializable
data class Changelog(
  val changes: List<VersionChanges>,
)

/**
 * Represents the changes for a specific version release.
 */
@Serializable
data class VersionChanges(
  val version: String,
  val date: String?,
  val changes: List<ChangeSet>,
)

/**
 * Represents a categorized set of changes (e.g., "Added", "Fixed", "Changed").
 */
@Serializable
data class ChangeSet(
  val name: String?,
  val changes: List<String>,
)
