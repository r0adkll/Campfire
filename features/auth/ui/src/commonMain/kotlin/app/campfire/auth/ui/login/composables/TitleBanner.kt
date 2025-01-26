package app.campfire.auth.ui.login.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TitleBanner(
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.aligned(Alignment.CenterHorizontally),
  ) {
    Image(
      CampfireIcons.Campfire,
      contentDescription = null,
      modifier = Modifier.size(96.dp),
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = stringResource(Res.string.welcome_title),
      style = MaterialTheme.typography.displayMedium,
      fontFamily = PaytoneOneFontFamily,
    )
  }
}
