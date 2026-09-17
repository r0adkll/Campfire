// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.whatsnew

import app.campfire.core.Platform
import app.campfire.whatsnew.api.ChangeSet
import app.campfire.whatsnew.api.VersionChanges
import kotlinx.serialization.Serializable

/**
 * A version section of `changelog.json`, generated from CHANGELOG.md by the
 * `app.campfire.changelog` convention plugin with every entry resolved to its platforms.
 */
@Serializable
internal data class VersionEntry(
  val version: String,
  val date: String?,
  val changes: List<ChangeSetEntry>,
)

@Serializable
internal data class ChangeSetEntry(
  val name: String?,
  val changes: List<ChangeEntry>,
)

@Serializable
internal data class ChangeEntry(
  val text: String,
  val platforms: Set<Platform>,
)

/** The changes that apply to [platform], dropping categories and versions left empty. */
internal fun List<VersionEntry>.forPlatform(platform: Platform): List<VersionChanges> = mapNotNull { version ->
  val changeSets = version.changes.mapNotNull { changeSet ->
    changeSet.changes
      .filter { platform in it.platforms }
      .map { it.text }
      .takeIf { it.isNotEmpty() }
      ?.let { ChangeSet(changeSet.name, it) }
  }
  changeSets
    .takeIf { it.isNotEmpty() }
    ?.let { VersionChanges(version.version, version.date, it) }
}

/**
 * Whether [versionName] has anything to announce on [platform]. A version without its own
 * section (e.g. a dev or alpha build) still announces, so the unreleased changes surface.
 */
internal fun List<VersionEntry>.hasChangesFor(versionName: String, platform: Platform): Boolean {
  val version = firstOrNull { it.version == versionName } ?: return true
  return version.changes.any { changeSet -> changeSet.changes.any { platform in it.platforms } }
}
