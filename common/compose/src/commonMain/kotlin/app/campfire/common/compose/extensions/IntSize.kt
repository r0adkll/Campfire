// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import androidx.compose.ui.unit.IntSize

val IntSize.area: Int get() = width * height
