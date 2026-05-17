package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CloudDownload
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_submit
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_submitting
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SubmitBar(
  isSubmitting: Boolean,
  canSubmit: Boolean,
  onSubmit: () -> Unit,
) {
  Surface(
    tonalElevation = 2.dp,
    shadowElevation = 3.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Spacer(Modifier.weight(1f))
      Button(
        onClick = onSubmit,
        enabled = canSubmit && !isSubmitting,
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
          )
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(Res.string.add_podcast_builder_submitting))
        } else {
          Icon(
            CampfireIcons.Rounded.CloudDownload,
            contentDescription = stringResource(Res.string.add_podcast_builder_submit),
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(Res.string.add_podcast_builder_submit))
        }
      }
    }
  }
}
