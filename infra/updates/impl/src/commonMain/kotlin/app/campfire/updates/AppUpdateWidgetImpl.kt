package app.campfire.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.asReadableBytes
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
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
              appUpdate = null,
            ),
          )
        } else {
          val update = appUpdateSource.getAvailableUpdate()
          emit(
            AppUpdateState(
              isSignedIn = true,
              appUpdate = update,
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
      state.isSignedIn && state.appUpdate != null -> UpdateContent(state.appUpdate, modifier)
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
    ) {
      TitleBar(
        title = "Sign-in required",
        icon = Icons.AutoMirrored.Rounded.Login,
      )
      CardContent(
        text = buildAnnotatedString {
          append(
            "Automatic app updates require you to be signed into the Firebase " +
              "AppTester platform.",
          )
        },
        action = "Sign in",
        onClick = {
          scope.launch {
            appUpdateSource.signIn()
            invalidator++
          }
        },
      )
    }
  }

  @Composable
  private fun UpdateContent(
    appUpdate: AppUpdate,
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()
    var updateProgress by remember { mutableStateOf<AppUpdateProgress?>(null) }

    AppUpdateCard(
      modifier = modifier,
    ) {
      if (updateProgress != null) {
        AppUpdatingContent(appUpdate, updateProgress!!)
      } else {
        AppUpdateAvailableContent(
          appUpdate = appUpdate,
          onClick = {
            scope.launch {
              appUpdateSource.installUpdate()
                .collect {
                  updateProgress = it
                }
              invalidator++
            }
          },
        )
      }
    }
  }

  @Composable
  private fun AppUpdateAvailableContent(
    appUpdate: AppUpdate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
  ) {
    Column(modifier) {
      TitleBar(
        title = "New version is available!",
        icon = Icons.Rounded.SystemUpdate,
      )
      CardContent(
        text = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("${appUpdate.versionName} (${appUpdate.versionCode})")
          }
          append(" is available to update")

          if (!appUpdate.releaseNotes.isNullOrBlank()) {
            appendLine()
            appendLine()
            append(appUpdate.releaseNotes.trim())
          }
        },
        action = "Update now",
        onClick = onClick,
      )
    }
  }

  @Composable
  private fun AppUpdatingContent(
    appUpdate: AppUpdate,
    progress: AppUpdateProgress,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.secondary,
  ) {
    Column(
      modifier = modifier
        .drawBehind {
          drawRect(
            color = progressColor,
            size = Size(
              width = size.width * progress.progress,
              height = size.height,
            ),
            alpha = 0.6f,
          )
        },
    ) {
      TitleBar(
        title = "Updating to v${appUpdate.versionName}",
        icon = Icons.Rounded.SystemUpdate,
      )

      Text(
        text = when (progress.status) {
          AppUpdateProgress.Status.Pending -> "Download queued…"
          AppUpdateProgress.Status.Downloading ->
            "Downloading ${progress.bytes.asReadableBytes()} of ${progress.totalBytes.asReadableBytes()}"
          AppUpdateProgress.Status.Downloaded -> "Download completed!"
          AppUpdateProgress.Status.Failed -> "Update failed!"
          AppUpdateProgress.Status.Canceled -> "Update canceled"
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
          .padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
          ),
      )
    }
  }

  @Composable
  private fun AppUpdateCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
  ) {
    ElevatedCard(
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
    text: AnnotatedString,
    action: String,
    onClick: () -> Unit = {},
  ) {
    var isExpandable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = if (expanded) Int.MAX_VALUE else 5,
      overflow = TextOverflow.Ellipsis,
      onTextLayout = { result ->
        if (result.didOverflowHeight && !isExpandable) {
          isExpandable = true
        }
      },
      modifier = Modifier
        .clickable(
          enabled = isExpandable,
          onClick = {
            expanded = !expanded
          },
        )
        .padding(
          start = 16.dp,
          end = 16.dp,
        ),
    )

    Spacer(Modifier.height(16.dp))

    Button(
      onClick = onClick,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Text(action)
    }

    Spacer(Modifier.height(16.dp))
  }
}

data class AppUpdateState(
  val isSignedIn: Boolean = false,
  val appUpdate: AppUpdate? = null,
)
