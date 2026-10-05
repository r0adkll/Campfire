// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import com.r0adkll.flatprefs.FlatPreferences
import kotlin.io.path.createTempDirectory
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/** A settings file in a fresh directory. FlatPrefs keeps one store per file per process, so each test needs its own. */
internal fun newSettingsFile(): Path = createTempDirectory("settings").toOkioPath() / SETTINGS_STORE_FILE_NAME

/** What's on disk in [file], read independently of the store that wrote it. */
internal fun readSettingsFile(file: Path): FlatPreferences =
  FlatPreferences.decode(FileSystem.SYSTEM.read(file) { readByteArray() })
