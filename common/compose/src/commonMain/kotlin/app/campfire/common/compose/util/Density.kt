package app.campfire.common.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
inline fun <T> withDensity(block: Density.() -> T): T = with(LocalDensity.current, block)
