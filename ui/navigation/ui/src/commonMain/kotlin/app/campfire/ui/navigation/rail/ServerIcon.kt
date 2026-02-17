package app.campfire.ui.navigation.rail

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.widgets.DefaultServerIconSize
import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ContributesTo(AppScope::class)
interface ServerIconComponent {
  val themeIconContent: ThemeIconContent
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun ServerIcon(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = DefaultServerIconSize,
  component: ServerIconComponent = rememberComponent(),
) {
  component.themeIconContent.Content(
    onClick = onClick,
    modifier = modifier
      .size(size),
  )
}
