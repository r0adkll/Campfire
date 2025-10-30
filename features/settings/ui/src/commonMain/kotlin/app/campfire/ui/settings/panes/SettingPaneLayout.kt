package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireTopAppBarInsets
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.widgets.CampfireTopAppBar

enum class PaneState {
  Single, Double,
}

val LocalPaneState = compositionLocalOf { PaneState.Single }

@Composable
internal fun SettingPaneLayout(
  title: @Composable () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val paneState by rememberUpdatedState(LocalPaneState.current)

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Column(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
  ) {
    CampfireTopAppBar(
      title = title,
      scrollBehavior = scrollBehavior,
      windowInsets = CampfireTopAppBarInsets
        .takeIf { paneState == PaneState.Single }
        ?: WindowInsets(0.dp),
      navigationIcon = {
        if (paneState == PaneState.Single) {
          IconButton(
            onClick = onBackClick,
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = null,
            )
          }
        }
      },
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      content = {
        content()
        Spacer(
          Modifier.height(
            CampfireWindowInsets.asPaddingValues().calculateBottomPadding(),
          ),
        )
      },
    )
  }
}
