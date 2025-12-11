package app.campfire.ui.theming.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultThemeIconContent(
  private val themeRepository: AppThemeRepository,
) : ThemeIconContent {

  @Composable
  override fun Content(onClick: () -> Unit, modifier: Modifier) {
    val currentAppTheme by remember {
      themeRepository.observeCurrentAppTheme()
    }.collectAsState()

    AppThemeImage(
      appTheme = currentAppTheme,
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .then(modifier),
    )
  }
}
