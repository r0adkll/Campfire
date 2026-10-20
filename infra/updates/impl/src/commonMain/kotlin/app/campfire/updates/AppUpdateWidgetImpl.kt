// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.asReadableBytes
import app.campfire.updates.source.AppUpdate
import campfire.infra.updates.impl.generated.resources.Res
import campfire.infra.updates.impl.generated.resources.update_action_dismiss
import campfire.infra.updates.impl.generated.resources.update_action_install
import campfire.infra.updates.impl.generated.resources.update_action_reinstall
import campfire.infra.updates.impl.generated.resources.update_action_retry
import campfire.infra.updates.impl.generated.resources.update_action_switch
import campfire.infra.updates.impl.generated.resources.update_action_update
import campfire.infra.updates.impl.generated.resources.update_card_available
import campfire.infra.updates.impl.generated.resources.update_card_channel_ended
import campfire.infra.updates.impl.generated.resources.update_card_downloading
import campfire.infra.updates.impl.generated.resources.update_card_failed
import campfire.infra.updates.impl.generated.resources.update_card_installing
import campfire.infra.updates.impl.generated.resources.update_card_pull_request
import campfire.infra.updates.impl.generated.resources.update_card_ready
import campfire.infra.updates.impl.generated.resources.update_card_reinstall
import campfire.infra.updates.impl.generated.resources.update_card_switch_to_main
import campfire.infra.updates.impl.generated.resources.update_card_version
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(AppScope::class)
@Inject
class AppUpdateWidgetImpl(
  private val appUpdates: AppUpdates,
) : AppUpdateWidget {

  @Composable
  override fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  ) {
    if (!appUpdates.isSupported) return

    val visibleUpdate by appUpdates.undismissedUpdate.collectAsState()
    var showInstallSheet by remember { mutableStateOf(false) }

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
      AppUpdateCard(
        update = update,
        // A pull request build is short-lived, so its card says which one it is
        pullRequest = (appUpdates.channel?.let(ReleaseChannel::parse) as? ReleaseChannel.PullRequest)?.number,
        onClick = onClick,
        onInstall = { showInstallSheet = true },
        onDismiss = { appUpdates.dismiss(update) }.takeIf { update.status.isDismissable },
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
      )
    }

    if (showInstallSheet) {
      appUpdates.installSheet.Content(
        onDismiss = {
          showInstallSheet = false
          appUpdates.refresh()
        },
      )
    }
  }
}

/**
 * The drawer's update card: a small version of the App updates pane's hero, saying where the
 * update stands, with its one action, or its progress while it downloads and installs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppUpdateCard(
  update: AppUpdate,
  onClick: () -> Unit,
  onInstall: () -> Unit,
  modifier: Modifier = Modifier,
  pullRequest: Int? = null,
  onDismiss: (() -> Unit)? = null,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    shape = MaterialTheme.shapes.large,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          Modifier
            .size(8.dp)
            .background(
              color = if (update.status == AppUpdate.Status.Failed) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.primary
              },
              shape = CircleShape,
            ),
        )
        Text(
          text = update.status.kicker()
            .let { kicker ->
              pullRequest?.let { stringResource(Res.string.update_card_pull_request, it, kicker) } ?: kicker
            }
            .uppercase(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp,
          modifier = Modifier.weight(1f),
        )
        if (onDismiss != null) {
          // Kept to the kicker's height, so the close button doesn't push the content down
          Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            val dismissLabel = stringResource(Res.string.update_action_dismiss)
            IconButtonTooltip(text = dismissLabel) {
              IconButton(
                onClick = onDismiss,
                modifier = Modifier
                  .offset(x = 6.dp)
                  .requiredSize(36.dp),
              ) {
                Icon(
                  CampfireIcons.Rounded.Close,
                  contentDescription = dismissLabel,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }

      Text(
        text = stringResource(Res.string.update_card_version, update.versionName),
        style = MaterialTheme.typography.titleLarge,
        fontFamily = PaytoneOneFontFamily,
        modifier = Modifier.padding(top = 4.dp),
      )

      val headline = if (update.status == AppUpdate.Status.ChannelEnded) {
        stringResource(Res.string.update_card_switch_to_main)
      } else {
        update.headline
      }
      if (headline != null) {
        Text(
          text = headline,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(Modifier.size(10.dp))

      when (val status = update.status) {
        is AppUpdate.Status.Downloading -> LinearWavyProgressIndicator(
          progress = { status.progress },
          modifier = Modifier.fillMaxWidth(),
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        AppUpdate.Status.Installing -> LinearWavyProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        else -> Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = update.sizeBytes?.asReadableBytes().orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = onInstall,
          ) {
            Text(update.status.action())
          }
        }
      }
    }
  }
}

@Composable
private fun AppUpdate.Status.kicker(): String = when (this) {
  AppUpdate.Status.Available -> stringResource(Res.string.update_card_available)
  AppUpdate.Status.ChannelEnded -> stringResource(Res.string.update_card_channel_ended)
  AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.update_card_reinstall)
  is AppUpdate.Status.Downloading -> stringResource(Res.string.update_card_downloading, (progress * 100).toInt())
  AppUpdate.Status.ReadyToInstall -> stringResource(Res.string.update_card_ready)
  AppUpdate.Status.Installing -> stringResource(Res.string.update_card_installing)
  AppUpdate.Status.Failed -> stringResource(Res.string.update_card_failed)
}

@Composable
private fun AppUpdate.Status.action(): String = when (this) {
  AppUpdate.Status.ChannelEnded -> stringResource(Res.string.update_action_switch)
  AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.update_action_reinstall)
  AppUpdate.Status.ReadyToInstall -> stringResource(Res.string.update_action_install)
  AppUpdate.Status.Failed -> stringResource(Res.string.update_action_retry)
  else -> stringResource(Res.string.update_action_update)
}
