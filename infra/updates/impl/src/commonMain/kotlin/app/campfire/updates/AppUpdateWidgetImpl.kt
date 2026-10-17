// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.NewReleases
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.asReadableBytes
import app.campfire.settings.api.AppStateSettings
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSource
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@ContributesBinding(AppScope::class)
@Inject
class AppUpdateWidgetImpl(
  private val appUpdateSource: AppUpdateSource,
  private val appUpdateChecker: AppUpdateChecker,
  private val appStateSettings: AppStateSettings,
) : AppUpdateWidget {

  @Composable
  override fun Content(modifier: Modifier) {
    if (!appUpdateSource.isSupported) return

    val scope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current

    val appUpdate by appUpdateChecker.update.collectAsState()
    val dismissedVersionCode by appStateSettings.observeAppUpdateDismissedVersionCode().collectAsState()
    var showSourceSheet by remember { mutableStateOf(false) }

    // Updates already under way can't be dismissed; the card tracks them to the install.
    val visibleUpdate = appUpdate?.takeIf { !it.status.isDismissable || it.versionCode != dismissedVersionCode }

    // Retain the last visible update so the exit animation renders the same card
    // that was on screen when the widget was dismissed or invalidated.
    var lastUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    if (visibleUpdate != null && visibleUpdate != lastUpdate) {
      lastUpdate = visibleUpdate
    }

    AnimatedVisibility(
      visible = visibleUpdate != null,
      enter = slideInHorizontally { -it } + fadeIn(),
      exit = slideOutHorizontally { -it } +
        shrinkVertically(shrinkTowards = Alignment.CenterVertically) { 0 } +
        fadeOut(),
      modifier = modifier,
    ) {
      val update = visibleUpdate ?: lastUpdate ?: return@AnimatedVisibility
      AppUpdateWidgetCard(
        title = update.status.cardTitle,
        subtitle = update.cardSubtitle,
        icon = CampfireIcons.Rounded.NewReleases,
        onClick = {
          if (appUpdateSource.updateSheet != null) {
            showSourceSheet = true
          } else {
            scope.launch {
              overlayHost.showAppUpdateSheet(update)
              appUpdateChecker.refresh()
            }
          }
        },
        onDismiss = {
          appStateSettings.appUpdateDismissedVersionCode = update.versionCode
        }.takeIf { update.status.isDismissable },
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
      )
    }

    val sourceSheet = appUpdateSource.updateSheet
    if (showSourceSheet && sourceSheet != null) {
      sourceSheet.Content(
        onDismiss = {
          showSourceSheet = false
          appUpdateChecker.refresh()
        },
      )
    }
  }

  private suspend fun OverlayHost.showAppUpdateSheet(appUpdate: AppUpdate) {
    show(
      BottomSheetOverlay<AppUpdate, Unit>(
        model = appUpdate,
        sheetShape = RoundedCornerShape(
          topStart = 32.dp,
          topEnd = 32.dp,
        ),
        onDismiss = { },
        skipPartiallyExpandedState = true,
      ) { update, _ ->
        AppUpdateSheetContent(update)
      },
    )
  }

  @Composable
  private fun AppUpdateSheetContent(
    appUpdate: AppUpdate,
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()
    var updateProgress by remember { mutableStateOf<AppUpdateProgress?>(null) }

    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = "Update available",
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = PaytoneOneFontFamily,
      )

      Text(
        text = "${appUpdate.versionName} (${appUpdate.versionCode})",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      val releaseNotes = appUpdate.releaseNotes?.trim()
      if (!releaseNotes.isNullOrBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(
          text = releaseNotes,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
        )
      }

      Spacer(Modifier.height(24.dp))

      val progress = updateProgress
      if (progress == null || progress.status.isTerminal) {
        if (progress != null) {
          Text(
            text = progress.statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
          Spacer(Modifier.height(8.dp))
        }
        Button(
          onClick = {
            scope.launch {
              appUpdateSource.installUpdate()
                .collect { updateProgress = it }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (progress == null) "Update now" else "Try again")
        }
      } else {
        if (progress.status == AppUpdateProgress.Status.Pending) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          LinearProgressIndicator(
            progress = { progress.progress },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text = progress.statusText,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}

private val AppUpdate.Status.isDismissable: Boolean
  get() = when (this) {
    AppUpdate.Status.Available,
    AppUpdate.Status.ChannelEnded,
    AppUpdate.Status.ReinstallRequired,
    AppUpdate.Status.Failed,
    -> true
    is AppUpdate.Status.Downloading,
    AppUpdate.Status.ReadyToInstall,
    AppUpdate.Status.Installing,
    -> false
  }

private val AppUpdate.Status.cardTitle: String
  get() = when (this) {
    AppUpdate.Status.Available -> "Update available"
    AppUpdate.Status.ChannelEnded -> "Pull request closed"
    AppUpdate.Status.ReinstallRequired -> "Update needs a reinstall"
    is AppUpdate.Status.Downloading -> "Downloading update"
    AppUpdate.Status.ReadyToInstall -> "Update ready"
    AppUpdate.Status.Installing -> "Installing update"
    AppUpdate.Status.Failed -> "Update failed"
  }

private val AppUpdate.cardSubtitle: String
  get() = when (val status = status) {
    AppUpdate.Status.ChannelEnded -> "Switch to main"
    is AppUpdate.Status.Downloading -> "${(status.progress * 100).toInt()}%"
    AppUpdate.Status.ReadyToInstall -> "Tap to install"
    AppUpdate.Status.Failed -> "Tap to retry"
    else -> headline ?: versionName
  }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppUpdateWidgetCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ),
    shape = MaterialTheme.shapes.large,
    onClick = onClick,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          icon,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )

        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelMediumEmphasized,
        )
      }

      if (onDismiss != null) {
        val dismissLabel = "Dismiss"
        IconButtonTooltip(text = dismissLabel) {
          IconButton(
            onClick = onDismiss,
          ) {
            Icon(
              CampfireIcons.Rounded.Close,
              contentDescription = dismissLabel,
            )
          }
        }
      }

      Spacer(Modifier.size(8.dp))
    }
  }
}

private val AppUpdateProgress.Status.isTerminal: Boolean
  get() = this == AppUpdateProgress.Status.Failed || this == AppUpdateProgress.Status.Canceled

private val AppUpdateProgress.statusText: String
  get() = when (status) {
    AppUpdateProgress.Status.Pending -> "Download queued…"
    AppUpdateProgress.Status.Downloading ->
      "Downloading ${bytes.asReadableBytes()} of ${totalBytes.asReadableBytes()}"
    AppUpdateProgress.Status.Downloaded -> "Download completed!"
    AppUpdateProgress.Status.Failed -> "Update failed!"
    AppUpdateProgress.Status.Canceled -> "Update canceled"
  }
