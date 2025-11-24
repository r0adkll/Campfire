package app.campfire.collections.ui.detail.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.widgets.CampfireTopAppBar

@Composable
fun CollectionDetailTopAppBar(
  name: String,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = Color.Unspecified,
  scrolledContainerColor: Color = Color.Unspecified,
) {
  CampfireTopAppBar(
    modifier = modifier,
    title = { Text(name) },
    containerColor = containerColor,
    scrolledContainerColor = scrolledContainerColor,
    scrollBehavior = scrollBehavior,
    navigationIcon = {
      IconButton(
        onClick = onBack,
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
      }
    },
    actions = {
      IconButton(
        onClick = onDelete,
      ) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
      }
    },
  )
}
