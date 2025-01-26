package app.campfire.auth.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.auth.ui.welcome.composables.AddCampsiteCard
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.UserScope
import campfire.features.auth.ui.generated.resources.Res
import campfire.features.auth.ui.generated.resources.welcome_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(WelcomeScreen::class, UserScope::class)
@Composable
fun Welcome(
  state: WelcomeUiState,
  modifier: Modifier,
) {
  val eventSink = state.eventSink

  Column(modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      Image(
        CampfireIcons.Campfire,
        contentDescription = null,
        modifier = Modifier.size(236.dp),
      )

      Spacer(Modifier.height(16.dp))

      Text(
        text = stringResource(Res.string.welcome_title),
        style = MaterialTheme.typography.displayLarge,
        fontFamily = PaytoneOneFontFamily,
      )
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      AddCampsiteCard(
        onClick = { eventSink(WelcomeUiEvent.AddCampsite) },
        modifier = Modifier
          .widthIn(max = 500.dp)
          .fillMaxWidth()
          .padding(
            horizontal = 26.dp,
          ),
      )
    }
  }
}
