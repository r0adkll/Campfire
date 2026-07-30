// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import androidx.compose.ui.text.TextStyle

fun TextStyle.alpha(alpha: Float): TextStyle = copy(color = color.copy(alpha = alpha))
