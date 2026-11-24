// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.core.files.applicationSupportDirectory
import app.campfire.core.files.createExcludedFromBackup
import kotlinx.io.files.Path

/**
 * Where offline audio is kept: Application Support, which the system never purges and the Files app
 * doesn't show. Its absolute path changes when the app is updated, so it's looked up rather than
 * stored.
 */
internal fun iosDownloadsRoot(): Path = Path(applicationSupportDirectory(), "downloads")

/** Creates the downloads directory, kept out of backups: the files can be downloaded again. */
internal fun prepareDownloadsRoot(root: Path) = createExcludedFromBackup(root.toString())
