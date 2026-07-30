// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

actual val isNoisyCampfireLoadingIndicatorEnabled: Boolean
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
