package app.campfire.auth.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.NoisyCampfireIcon
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TwoPaneLayout(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Surface {
    Row(
      modifier = modifier
        .fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        NoisyCampfireIcon(
          modifier = Modifier.size(236.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = stringResource(Res.string.welcome_title),
          style = MaterialTheme.typography.displayLarge,
          fontFamily = PaytoneOneFontFamily,
        )
      }

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f),
      ) {
        content()
      }
    }
  }
}
