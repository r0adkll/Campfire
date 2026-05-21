package app.campfire.common.compose.theme.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FatCheck
import app.campfire.common.compose.theme.CampfireTheme

@Immutable
class CampfireColorScheme(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,

  val loading: Color,
  val onLoading: Color,
)

fun lightCampfireColorScheme(): CampfireColorScheme = CampfireColorScheme(
  success = Color(0xFF008000),
  onSuccess = Color(0xFFD3FFC2),
  successContainer = Color(0xFFC1EFAF),
  onSuccessContainer = Color(0xFF012200),

  loading = Color(0xffFEE650),
  onLoading = Color(0xff4A4100),
)

fun darkCampfireColorScheme(): CampfireColorScheme = CampfireColorScheme(
  success = Color(0xFF008000),
  onSuccess = Color(0xFFC5F0C8),
  successContainer = Color(0xFF719C63),
  onSuccessContainer = Color(0xFF000000),

  loading = Color(0xffFEE650),
  onLoading = Color(0xff4A4100),
)

val LocalCampfireColorScheme = staticCompositionLocalOf {
  lightCampfireColorScheme()
}

@Composable
fun CampfireColorSchemePreview(
  useDarkColors: Boolean,
) {
  val scheme = if (useDarkColors) darkCampfireColorScheme() else lightCampfireColorScheme()

  CampfireTheme(
    useDarkColors = useDarkColors,
  ) {
    Surface(
      modifier = Modifier
        .size(300.dp, 200.dp),
    ) {
      Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(
                color = scheme.success,
                shape = CircleShape,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              CampfireIcons.Rounded.FatCheck,
              contentDescription = null,
              tint = scheme.onSuccess,
            )
          }

          Box(
            modifier = Modifier
              .size(40.dp)
              .background(
                color = scheme.successContainer,
                shape = CircleShape,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              CampfireIcons.Rounded.FatCheck,
              contentDescription = null,
              tint = scheme.onSuccessContainer,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(
                color = scheme.loading,
                shape = CircleShape,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              CampfireIcons.Rounded.FatCheck,
              contentDescription = null,
              tint = scheme.onLoading,
            )
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun CampfireColorSchemePreviewLight() {
  CampfireColorSchemePreview(false)
}

@Preview(uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
fun CampfireColorSchemePreviewDark() {
  CampfireColorSchemePreview(true)
}
