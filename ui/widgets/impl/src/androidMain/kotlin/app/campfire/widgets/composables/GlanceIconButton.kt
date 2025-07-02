package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import app.campfire.core.extensions.fluentIf
import kotlin.reflect.KClass

@Composable
fun GlanceIconButton(
  resourceId: Int,
  contentDescription: String?,
  onClickActionCallback: KClass<out ActionCallback>,
  modifier: GlanceModifier = GlanceModifier,
  size: Dp = Dp.Unspecified, // Optional: specify a size for the icon
  colorFilter: ColorFilter? = null,
) {
  Box(
    modifier = modifier
      .fluentIf(size != Dp.Unspecified) {
        size(size)
      }
      .clickable(onClick = actionRunCallback(onClickActionCallback.java)),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      provider = ImageProvider(resourceId),
      contentDescription = contentDescription,
      colorFilter = colorFilter,
      modifier = GlanceModifier
        .then(
          if (size != Dp.Unspecified) {
            GlanceModifier.size(size)
          } else {
            GlanceModifier
          },
        ),
    )
  }
}
