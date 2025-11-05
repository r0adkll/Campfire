package app.campfire.sessions.ui.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val SessionSheetTitleHeight = 56.dp

@Composable
internal fun SessionSheetLayout(
  title: @Composable BoxScope.() -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable BoxScope.() -> Unit = {},
  titleBarHeight: Dp = SessionSheetTitleHeight,
  colors: SessionSheetColors = SessionSheetDefaults.colors(),
  state: SessionSheetTitleState? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val containerColor by animateColorAsState(
    targetValue = colors.containerColor(state?.isScrolled ?: false),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
  )

  Column(
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .background(containerColor)
        .fillMaxWidth(),
    ) {
      // FIXME: Having this here is a hack to make the scrolling color change
      //  work with this setup. We should hoist this and apply the color to the actual
      //  component we hand to the bottom sheet
      if (state != null) {
        BottomSheetDefaults.DragHandle(
          modifier = Modifier.align(Alignment.CenterHorizontally),
        )
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .height(titleBarHeight),
        contentAlignment = Alignment.Center,
      ) {
        ProvideTextStyle(
          MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
          ),
        ) {
          title()
        }

        trailingContent()
      }
    }

    content()
  }
}

@Stable
class SessionSheetTitleState(
  initiallyScrolled: Boolean,
) {
  var isScrolled by mutableStateOf(initiallyScrolled)

  companion object {
    val Saver: Saver<SessionSheetTitleState, *> =
      listSaver(
        save = { listOf(it.isScrolled) },
        restore = {
          SessionSheetTitleState(
            initiallyScrolled = it[0],
          )
        },
      )
  }
}

@Stable
class SessionSheetColors internal constructor(
  val containerColor: Color,
  val scrolledContainerColor: Color,
  val contentColor: Color,
) {

  @Stable
  internal fun containerColor(isScrolled: Boolean) = if (isScrolled) scrolledContainerColor else containerColor

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other !is SessionSheetColors) return false

    if (containerColor != other.containerColor) return false
    if (scrolledContainerColor != other.scrolledContainerColor) return false
    if (contentColor != other.contentColor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = containerColor.hashCode()
    result = 31 * result + scrolledContainerColor.hashCode()
    result = 31 * result + contentColor.hashCode()
    return result
  }
}

@Composable
internal fun rememberSessionSheetTitleState(
  isAlreadyScrolled: Boolean = false,
): SessionSheetTitleState = rememberSaveable(saver = SessionSheetTitleState.Saver) {
  SessionSheetTitleState(isAlreadyScrolled)
}

object SessionSheetDefaults {

  @Composable
  fun colors(
    containerColor: Color? = null,
    scrolledContainerColor: Color? = null,
    contentColor: Color? = null,
  ): SessionSheetColors = SessionSheetColors(
    containerColor = containerColor ?: BottomSheetDefaults.ContainerColor,
    scrolledContainerColor = scrolledContainerColor ?: MaterialTheme.colorScheme.secondaryContainer
      .copy(alpha = 0.5f)
      .compositeOver(MaterialTheme.colorScheme.surfaceContainer),
    contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
  )
}
