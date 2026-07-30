// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.logging.loggableUrl

data class Server(
  val url: String,
  val user: User,
  val name: String,
  val settings: Settings,
) {

  // Deliberately redacts url — Server objects get interpolated into logs that ship
  // to crash reporting as breadcrumbs.
  override fun toString(): String =
    "Server(url=${url.loggableUrl}, user=$user, name=$name, settings=$settings)"

  data class Settings(
    val scannerFindCovers: Boolean,
    val scannerCoverProvider: String,
    val scannerParseSubtitle: Boolean,
    val scannerPreferMatchedMetadata: Boolean,
    val scannerDisableWatcher: Boolean,
    val storeCoverWithItem: Boolean,
    val storeMetadataWithItem: Boolean,
    val metadataFileFormat: String,
    val rateLimitLoginRequests: Int,
    val rateLimitLoginWindow: Int,
    val backupSchedule: String,
    val backupsToKeep: Int,
    val maxBackupSize: Int,
    val loggerDailyLogsToKeep: Int,
    val loggerScannerLogsToKeep: Int,
    val homeBookshelfView: Int,
    val bookshelfView: Int,
    val sortingIgnorePrefix: Boolean,
    val sortingPrefixes: List<String>,
    val chromecastEnabled: Boolean,
    val dateFormat: String,
    val timeFormat: String,
    val language: String,
    val logLevel: Int,
    val version: String,
  )
}
