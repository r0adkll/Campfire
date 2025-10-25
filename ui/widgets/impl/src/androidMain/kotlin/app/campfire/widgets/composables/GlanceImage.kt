package app.campfire.widgets.composables

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import app.campfire.widgets.theme.withAlpha
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun GlanceImage(
  url: Any?,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(url) {
    withContext(Dispatchers.IO) {
      val request = ImageRequest.Builder(context)
        .data(url)
        .build()

      bitmap = when (val result = context.imageLoader.execute(request)) {
        is ErrorResult -> null
        is SuccessResult -> {
          result.image.toBitmap()
        }
      }
    }
  }

  bitmap.let {
    if (bitmap != null) {
      Image(
        provider = ImageProvider(bitmap!!),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.75f),
        ),
      )
    } else {
      Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }
  }
}
