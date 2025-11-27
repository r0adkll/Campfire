package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.isSupportingPaneEnabled

/**
 * Window insets for [TopAppBar]s that might be displayed in a context where
 * the supporting content layout is visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
val CampfireTopAppBarInsets: WindowInsets
  @Composable get() {
    val windowSizeClass = LocalWindowSizeClass.current
    val contentLayout = LocalContentLayout.current

    return if (windowSizeClass.isSupportingPaneEnabled && contentLayout == ContentLayout.Root) {
      TopAppBarDefaults.windowInsets
    } else {
      TopAppBarDefaults.windowInsets
    }
  }
