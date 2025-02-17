package app.campfire.debug.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import campfire.infra.debug.generated.resources.Res
import campfire.infra.debug.generated.resources.jbm_nerd_bold
import campfire.infra.debug.generated.resources.jbm_nerd_extrabold
import campfire.infra.debug.generated.resources.jbm_nerd_medium
import campfire.infra.debug.generated.resources.jbm_nerd_regular
import campfire.infra.debug.generated.resources.jbm_nerd_semibold
import org.jetbrains.compose.resources.Font

val JetBrainsMono: FontFamily
  @Composable get() {
    return FontFamily(
      Font(Res.font.jbm_nerd_regular),
      Font(Res.font.jbm_nerd_medium),
      Font(Res.font.jbm_nerd_semibold),
      Font(Res.font.jbm_nerd_bold),
      Font(Res.font.jbm_nerd_extrabold),
    )
  }
