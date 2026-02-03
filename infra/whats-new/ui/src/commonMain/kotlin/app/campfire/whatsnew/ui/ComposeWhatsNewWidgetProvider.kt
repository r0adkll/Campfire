package app.campfire.whatsnew.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.LoadingCampfireIcon
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.di.AppScope
import app.campfire.whatsnew.api.WhatsNewRepository
import app.campfire.whatsnew.api.WhatsNewWidgetProvider
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.ui.tooling.preview.Preview

@Inject
@ContributesBinding(AppScope::class)
class ComposeWhatsNewWidgetProvider(
  private val repository: WhatsNewRepository,
) : WhatsNewWidgetProvider {

  @Composable
  override fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  ) {
    val scope = rememberCoroutineScope()

    val showWhatsNewWidget by remember {
      repository.observeShouldShowWhatsNew()
    }.collectAsState(initial = false)

    AnimatedVisibility(
      visible = showWhatsNewWidget,
      enter = slideInHorizontally { -it } + fadeIn(),
      exit = slideOutHorizontally { -it } +
        shrinkVertically(shrinkTowards = Alignment.CenterVertically) { 0 } +
        fadeOut(),
      modifier = modifier,
    ) {
      WhatsNewWidget(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onDismiss = {
          scope.launch {
            repository.dismissWhatsNew()
          }
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WhatsNewWidget(
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    shape = MaterialTheme.shapes.large,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val scrim = Color.White
        .copy(alpha = 0.55f)
        .compositeOver(MaterialTheme.colorScheme.secondaryContainer)
      Box(
        modifier = Modifier
          .background(
            brush = Brush.radialGradient(
              0f to scrim,
              .5f to scrim.copy(alpha = .4f),
              1f to Color.Transparent,
            ),
          )
          .padding(12.dp),
      ) {
        LoadingCampfireIcon(
          size = 48.dp,
          modifier = Modifier
            .size(48.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = "See what's new!",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = PaytoneOneFontFamily,
        )

        Text(
          text = "v0.10.0-beta",
          style = MaterialTheme.typography.labelMediumEmphasized,
        )
      }

      IconButton(
        onClick = onDismiss,
      ) {
        Icon(
          Icons.Rounded.Close,
          contentDescription = null,
        )
      }

      Spacer(Modifier.size(8.dp))
    }
  }
}

@Preview
@Composable
private fun WhatsNewWidgetPreview() {
  CampfireTheme {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      PermanentDrawerSheet {
        WhatsNewWidget(
          onClick = {},
          onDismiss = {},
          modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        )
      }
    }
  }
}
