package app.campfire.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.core.di.AppScope
import app.campfire.updates.source.AppUpdateSource
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppUpdateWidgetImpl(
  private val appUpdateSource: AppUpdateSource,
) : AppUpdateWidget {

  private var invalidator by mutableIntStateOf(0)

  @Composable
  override fun Content(modifier: Modifier) {
    val state by remember(invalidator) {
      flow {
        val isSignedIn = appUpdateSource.isSignedIn()
        if (!isSignedIn) {
          emit(
            AppUpdateState(
              isSignedIn = false,
              isUpdateAvailable = false,
            ),
          )
        } else {
          val isUpdateAvailable = appUpdateSource.isUpdateAvailable()
          emit(
            AppUpdateState(
              isSignedIn = true,
              isUpdateAvailable = isUpdateAvailable,
            ),
          )
        }
      }
    }.collectAsState(AppUpdateState())

    Content(state, modifier)
  }

  @Composable
  private fun Content(
    state: AppUpdateState,
    modifier: Modifier = Modifier,
  ) {
    when {
      state.isSignedIn && state.isUpdateAvailable -> UpdateContent(modifier)
      !state.isSignedIn -> SignInContent(modifier)
      else -> Unit
    }
  }

  @Composable
  private fun SignInContent(
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()

    AppUpdateCard(
      modifier = modifier,
      onClick = {
        scope.launch {
          appUpdateSource.signIn()
          invalidator++
        }
      },
    ) {
      TitleBar(
        title = "Sign-in required",
        icon = Icons.AutoMirrored.Rounded.Login,
      )
      CardContent(
        text = "Automatic app updates require you to be signed into the Firebase " +
          "AppTester platform.",
        action = "Sign in",
      )
    }
  }

  @Composable
  private fun UpdateContent(
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()

    AppUpdateCard(
      modifier = modifier,
      onClick = {
        scope.launch {
          appUpdateSource.installUpdate()
          invalidator++
        }
      },
    ) {
      TitleBar(
        title = "New version is available!",
        icon = Icons.Rounded.SystemUpdate,
      )
      CardContent(
        text = "A new version of the app is available to update",
        action = "Update now",
      )
    }
  }

  @Composable
  private fun AppUpdateCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
  ) {
    ElevatedCard(
      onClick = onClick,
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ),
      content = content,
      modifier = modifier.padding(16.dp),
    )
  }

  @Composable
  private fun TitleBar(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
  ) {
    Row(
      modifier = modifier
        .height(48.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }

  @Composable
  private fun ColumnScope.CardContent(
    text: String,
    action: String,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier
        .padding(
          start = 16.dp,
          end = 16.dp,
        ),
    )

    Spacer(Modifier.height(16.dp))

    Text(
      text = action,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .align(Alignment.Start)
        .padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(16.dp))
  }
}

data class AppUpdateState(
  val isSignedIn: Boolean = false,
  val isUpdateAvailable: Boolean = false,
)
