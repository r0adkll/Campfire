// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * The cursor shown while hovering a horizontal resize handle: a left-right arrow on desktop,
 * the default pointer everywhere else (touch platforms never show one).
 */
internal expect val horizontalResizePointerIcon: PointerIcon
