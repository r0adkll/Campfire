package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
fun ComposeUiTest.setCampfireContent(
  contentLayout: ContentLayout = ContentLayout.Root,
  content: @Composable () -> Unit,
) {
  setContent {
    PreviewSharedElementTransitionLayout {
      CompositionLocalProvider(
        LocalContentLayout provides contentLayout,
      ) {
        content()
      }
    }
  }
}
