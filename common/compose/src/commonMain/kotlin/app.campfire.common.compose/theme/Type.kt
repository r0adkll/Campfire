package app.campfire.common.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import campfire.common.compose.generated.resources.PaytoneOne_Regular
import campfire.common.compose.generated.resources.Res
import org.jetbrains.compose.resources.Font

// Replace with your font locations
val Roboto = FontFamily.Default

val CampfireTypography: Typography
  @Composable get() {
    val default = Typography()
    val fontFamily = Roboto
    return Typography(
      displayLarge = default.displayLarge.copy(fontFamily = fontFamily),
      displayMedium = default.displayMedium.copy(fontFamily = fontFamily),
      displaySmall = default.displaySmall.copy(fontFamily = fontFamily),
      headlineLarge = default.headlineLarge.copy(fontFamily = fontFamily),
      headlineMedium = default.headlineMedium.copy(fontFamily = fontFamily),
      headlineSmall = default.headlineSmall.copy(fontFamily = fontFamily),
      titleLarge = default.titleLarge.copy(fontFamily = fontFamily),
      titleMedium = default.titleMedium.copy(fontFamily = fontFamily),
      titleSmall = default.titleSmall.copy(fontFamily = fontFamily),
      bodyLarge = default.bodyLarge.copy(fontFamily = fontFamily),
      bodyMedium = default.bodyMedium.copy(fontFamily = fontFamily),
      bodySmall = default.bodySmall.copy(fontFamily = fontFamily),
      labelLarge = default.labelLarge.copy(fontFamily = fontFamily),
      labelMedium = default.labelMedium.copy(fontFamily = fontFamily),
      labelSmall = default.labelSmall.copy(fontFamily = fontFamily),
    )
  }

val PaytoneOneFontFamily: FontFamily
  @Composable get() {
    val font = Font(Res.font.PaytoneOne_Regular)
    return FontFamily(font)
  }
