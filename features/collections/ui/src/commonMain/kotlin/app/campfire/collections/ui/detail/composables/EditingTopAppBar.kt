package app.campfire.collections.ui.detail.composables

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.theme.PaytoneOneFontFamily

@Composable
fun EditingTopAppBar(
  title: @Composable () -> Unit,
  actions: @Composable RowScope.() -> Unit = {},
  onDismiss: () -> Unit,
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    modifier = modifier,
    title = {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = PaytoneOneFontFamily,
        ),
      ) {
        title()
      }
    },
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
      actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    navigationIcon = {
      IconButton(
        onClick = onDismiss,
      ) {
        Icon(Icons.Rounded.Close, contentDescription = null)
      }
    },
    actions = actions,
  )
}
