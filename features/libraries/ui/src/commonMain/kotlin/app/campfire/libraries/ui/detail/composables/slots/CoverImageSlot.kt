package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.CoverImageSize
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.placeholder_book
import com.r0adkll.swatchbuckler.compose.Swatch
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource

private val RootCoverImageSize = 300.dp

@OptIn(ExperimentalSharedTransitionApi::class)
class CoverImageSlot(
  private val imageUrl: String?,
  private val contentDescription: String?,
  private val sharedTransitionKey: String,
  private val isDynamicThemingEnabled: Boolean = true,
) : ContentSlot {

  override val id: String = "cover_image"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) = SharedElementTransitionScope {
    Box(
      modifier = modifier,
    ) {
      // If we already cached the local theme, then don't waste cycles
      // computing it
      var swatch by remember { mutableStateOf<Swatch?>(null) }

      val size = if (LocalContentLayout.current == ContentLayout.Supporting) {
        CoverImageSize
      } else {
        RootCoverImageSize
      }
      CoverImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        size = size,
        placeholder = painterResource(Res.drawable.placeholder_book),
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            vertical = 16.dp,
          ),
        swatchListener = if (isDynamicThemingEnabled) {
          { palette -> swatch = palette }
        } else {
          null
        },
        sharedElementModifier = Modifier
          .sharedElement(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Image,
              ),
            ),
            animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
          ),
      )

      Box(
        modifier = Modifier
          .size(size)
          .align(Alignment.Center),
      ) {
        AnimatedVisibility(
          visible = swatch != null,
          modifier = Modifier
            .padding(8.dp)
            .align(Alignment.BottomStart),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          SwatchToolbar(
            swatch = swatch!!,
            onColorClicked = {
              eventSink(LibraryItemUiEvent.SeedColorChange(it))
            },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwatchToolbar(
  swatch: Swatch,
  onColorClicked: (Color) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilledIconToggleButton(
      checked = expanded,
      onCheckedChange = { expanded = it },
      shapes = IconToggleButtonShapes(
        shape = CircleShape,
        pressedShape = MaterialTheme.shapes.medium,
      ),
      colors = IconButtonDefaults.filledIconToggleButtonColors(),
    ) {
      Icon(
        if (expanded) Icons.Rounded.Close else Icons.Rounded.Palette,
        contentDescription = null,
      )
    }

    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.CenterHorizontally),
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally),
    ) {
      Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier
            .padding(
              horizontal = 12.dp,
              vertical = 6.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val colorOptionSize by transition.animateDp { state ->
            if (state == EnterExitState.Visible) DefaultColorOptionSize else 8.dp
          }

          ColorOptionButton(
            color = swatch.dominant,
            onClick = {
              onColorClicked(swatch.dominant)
            },
            size = colorOptionSize,
            modifier = Modifier.animateEnterExit(
              enter = expandIn(expandFrom = Alignment.Center, clip = false),
              exit = shrinkOut(shrinkTowards = Alignment.Center, clip = false),
            ),
          )

          swatch.vibrant.forEach { c ->
            ColorOptionButton(
              color = c,
              onClick = {
                onColorClicked(c)
              },
              size = colorOptionSize,
              modifier = Modifier.animateEnterExit(
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center, clip = false),
                exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center, clip = false),
              ),
            )
          }
        }
      }
    }
  }
}

private val DefaultColorOptionSize = 32.dp

@Composable
private fun ColorOptionButton(
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = DefaultColorOptionSize,
) {
  Box(
    modifier
      .clip(CircleShape)
      .clickable(onClick = onClick)
      .size(size)
      .background(color)
      .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
  )
}
