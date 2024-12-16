// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun colorScheme(
  colorPalette: ColorPalette,
  useDarkColors: Boolean,
  useDynamicColors: Boolean,
): ColorScheme = when {
  useDarkColors -> colorPalette.darkColorScheme
  else -> colorPalette.lightColorScheme
}
