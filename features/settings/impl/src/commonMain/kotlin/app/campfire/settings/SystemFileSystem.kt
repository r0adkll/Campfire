// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import okio.FileSystem

/** The device's file system; okio only exposes it outside common code. */
internal expect val SystemFileSystem: FileSystem
