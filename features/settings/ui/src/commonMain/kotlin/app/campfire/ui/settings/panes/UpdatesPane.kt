// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.icons.rounded.Smartphone
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.extensions.asReadableBytes
import app.campfire.ui.settings.AppUpdatesSettingsInfo
import app.campfire.ui.settings.SettingsUiEvent.AppUpdatesSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.updates.AppUpdateCheck
import app.campfire.updates.ReleaseChannel
import app.campfire.updates.source.AppUpdate
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_updates_title
import campfire.features.settings.ui.generated.resources.updates_action_check_now
import campfire.features.settings.ui.generated.resources.updates_action_checking
import campfire.features.settings.ui.generated.resources.updates_action_install
import campfire.features.settings.ui.generated.resources.updates_action_reinstall
import campfire.features.settings.ui.generated.resources.updates_action_retry
import campfire.features.settings.ui.generated.resources.updates_action_switch
import campfire.features.settings.ui.generated.resources.updates_action_update
import campfire.features.settings.ui.generated.resources.updates_action_view_progress
import campfire.features.settings.ui.generated.resources.updates_channel_beta
import campfire.features.settings.ui.generated.resources.updates_channel_main
import campfire.features.settings.ui.generated.resources.updates_check_again_title
import campfire.features.settings.ui.generated.resources.updates_installed_title
import campfire.features.settings.ui.generated.resources.updates_installed_version
import campfire.features.settings.ui.generated.resources.updates_kicker_available
import campfire.features.settings.ui.generated.resources.updates_kicker_channel_ended
import campfire.features.settings.ui.generated.resources.updates_kicker_checked
import campfire.features.settings.ui.generated.resources.updates_kicker_checking
import campfire.features.settings.ui.generated.resources.updates_kicker_downloading
import campfire.features.settings.ui.generated.resources.updates_kicker_failed
import campfire.features.settings.ui.generated.resources.updates_kicker_installing
import campfire.features.settings.ui.generated.resources.updates_kicker_not_checked
import campfire.features.settings.ui.generated.resources.updates_kicker_ready
import campfire.features.settings.ui.generated.resources.updates_kicker_reinstall
import campfire.features.settings.ui.generated.resources.updates_meta_pull_request
import campfire.features.settings.ui.generated.resources.updates_sub_channel_ended
import campfire.features.settings.ui.generated.resources.updates_sub_check_failed
import campfire.features.settings.ui.generated.resources.updates_sub_reinstall
import campfire.features.settings.ui.generated.resources.updates_sub_up_to_date
import campfire.features.settings.ui.generated.resources.updates_summary_available
import campfire.features.settings.ui.generated.resources.updates_summary_channel_ended
import campfire.features.settings.ui.generated.resources.updates_summary_check_failed
import campfire.features.settings.ui.generated.resources.updates_summary_checking
import campfire.features.settings.ui.generated.resources.updates_summary_downloading
import campfire.features.settings.ui.generated.resources.updates_summary_failed
import campfire.features.settings.ui.generated.resources.updates_summary_installing
import campfire.features.settings.ui.generated.resources.updates_summary_ready
import campfire.features.settings.ui.generated.resources.updates_summary_reinstall
import campfire.features.settings.ui.generated.resources.updates_summary_up_to_date
import campfire.features.settings.ui.generated.resources.updates_title_available
import campfire.features.settings.ui.generated.resources.updates_title_channel_ended
import campfire.features.settings.ui.generated.resources.updates_title_check_failed
import campfire.features.settings.ui.generated.resources.updates_title_reinstall
import campfire.features.settings.ui.generated.resources.updates_title_up_to_date
import campfire.features.settings.ui.generated.resources.updates_title_version
import campfire.features.settings.ui.generated.resources.updates_whats_new_header
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UpdatesPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_updates_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    state.appUpdates?.let { info ->
      UpdatesSettings(
        info = info,
        onEvent = state.eventSink,
      )

      if (info.showSheet) {
        info.sheet.Content(onDismiss = { state.eventSink(AppUpdatesSettingEvent.UpdateSheetDismissed) })
      }
    }
  }
}

/**
 * The App updates pane's content: a hero saying where things stand, then what's new, the installed
 * build and the one action.
 */
@Composable
internal fun UpdatesSettings(
  info: AppUpdatesSettingsInfo,
  onEvent: (AppUpdatesSettingEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val update = info.update
    if (update != null) {
      WaitingUpdate(update, info, onEvent)
    } else {
      NoUpdate(info, onEvent)
    }
  }
}

@Composable
private fun WaitingUpdate(
  update: AppUpdate,
  info: AppUpdatesSettingsInfo,
  onEvent: (AppUpdatesSettingEvent) -> Unit,
) {
  val status = update.status
  UpdateHero(
    channel = info.channelLabel(),
    highlighted = true,
    failed = status == AppUpdate.Status.Failed,
    kicker = update.kicker(),
    title = update.title(),
    sub = when (status) {
      AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.updates_sub_reinstall)
      AppUpdate.Status.ChannelEnded -> stringResource(Res.string.updates_sub_channel_ended)
      else -> update.headline
    },
    // A pull request build's own PR is already on the channel chip
    meta = update.meta(
      showPullRequest = info.channel?.let(ReleaseChannel::parse) !=
        update.pullRequest?.let(ReleaseChannel::PullRequest),
    ),
    progress = when (status) {
      is AppUpdate.Status.Downloading -> status.progress
      AppUpdate.Status.Installing -> INDETERMINATE
      else -> null
    },
  )

  val notes = update.releaseNoteLines()
  if (status != AppUpdate.Status.ReinstallRequired && status != AppUpdate.Status.ChannelEnded && notes.isNotEmpty()) {
    WhatsNew(notes)
  }

  Spacer(Modifier.size(4.dp))
  InstalledRow(info)
  ActionSetting(
    leadingContent = { Icon(CampfireIcons.Rounded.Refresh, contentDescription = null) },
    headlineContent = { Text(stringResource(Res.string.updates_check_again_title)) },
    supportingContent = { Text(info.check.checkedLine()) },
    onClick = { onEvent(AppUpdatesSettingEvent.CheckNow) }.takeIf { !info.check.isChecking },
  )

  UpdateButton(
    text = update.action(),
    trailing = update.sizeBytes?.asReadableBytes()
      ?.takeIf { status !is AppUpdate.Status.Downloading && status != AppUpdate.Status.Installing },
    onClick = { onEvent(AppUpdatesSettingEvent.OpenUpdateSheet) },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoUpdate(
  info: AppUpdatesSettingsInfo,
  onEvent: (AppUpdatesSettingEvent) -> Unit,
) {
  val check = info.check
  val failed = check.failed && !check.isChecking
  UpdateHero(
    channel = info.channelLabel(),
    highlighted = false,
    failed = false,
    kicker = check.checkedLine(),
    title = stringResource(if (failed) Res.string.updates_title_check_failed else Res.string.updates_title_up_to_date),
    sub = if (failed) {
      stringResource(Res.string.updates_sub_check_failed)
    } else {
      stringResource(Res.string.updates_sub_up_to_date, info.installedVersion)
    },
    meta = null,
  )

  Spacer(Modifier.size(4.dp))
  InstalledRow(info)

  val label = stringResource(
    when {
      check.isChecking -> Res.string.updates_action_checking
      failed -> Res.string.updates_action_retry
      else -> Res.string.updates_action_check_now
    },
  )
  OutlinedButton(
    onClick = { onEvent(AppUpdatesSettingEvent.CheckNow) },
    enabled = !check.isChecking,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .heightIn(ButtonDefaults.MediumContainerHeight),
  ) {
    Icon(
      CampfireIcons.Rounded.Refresh,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
    Text(label)
  }
}

/**
 * Where the update stands: in the primary container while a build is waiting, with a dot before
 * the kicker, or in a quiet surface when there's nothing to do. The [channel] this build follows
 * sits at its top end in every state.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateHero(
  channel: String?,
  highlighted: Boolean,
  failed: Boolean,
  kicker: String,
  title: String,
  sub: String?,
  meta: String?,
  modifier: Modifier = Modifier,
  progress: Float? = null,
) {
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = if (highlighted) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (highlighted) {
          Box(
            Modifier
              .size(8.dp)
              .background(
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = CircleShape,
              ),
          )
        }
        Text(
          text = kicker.uppercase(),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.5.sp,
          color = LocalContentColor.current.copy(alpha = 0.85f),
          modifier = Modifier.weight(1f),
        )
        if (channel != null) {
          ChannelChip(channel, highlighted)
        }
      }

      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = PaytoneOneFontFamily,
      )

      if (sub != null) {
        Text(
          text = sub,
          style = MaterialTheme.typography.bodyLarge,
        )
      }

      if (meta != null) {
        Text(
          text = meta.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          letterSpacing = 1.sp,
          color = LocalContentColor.current.copy(alpha = 0.75f),
        )
      }

      if (progress != null) {
        Spacer(Modifier.size(4.dp))
        if (progress == INDETERMINATE) {
          LinearWavyProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          )
        } else {
          LinearWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          )
        }
      }
    }
  }
}

@Composable
private fun ChannelChip(
  channel: String,
  highlighted: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = CircleShape,
    color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
    contentColor = if (highlighted) {
      MaterialTheme.colorScheme.onPrimary
    } else {
      MaterialTheme.colorScheme.onSecondaryContainer
    },
    modifier = modifier,
  ) {
    Text(
      text = channel,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

/** The release channel this build follows, as testers know it: "Alpha", "Beta" or "PR #1290". */
@Composable
private fun AppUpdatesSettingsInfo.channelLabel(): String? = channel?.let { name ->
  when (val channel = ReleaseChannel.parse(name)) {
    ReleaseChannel.Main -> stringResource(Res.string.updates_channel_main)
    ReleaseChannel.Beta -> stringResource(Res.string.updates_channel_beta)
    is ReleaseChannel.PullRequest -> stringResource(Res.string.updates_meta_pull_request, channel.number)
    is ReleaseChannel.Other -> channel.name.replaceFirstChar { it.uppercase() }
  }
}

@Composable
private fun WhatsNew(notes: List<String>) {
  Header(
    title = { Text(stringResource(Res.string.updates_whats_new_header)) },
  )
  Column(
    modifier = Modifier.padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    notes.forEach { note ->
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "—",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = note,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun InstalledRow(info: AppUpdatesSettingsInfo) {
  ActionSetting(
    leadingContent = { Icon(CampfireIcons.Rounded.Smartphone, contentDescription = null) },
    headlineContent = { Text(stringResource(Res.string.updates_installed_title)) },
    supportingContent = {
      Text(stringResource(Res.string.updates_installed_version, info.installedVersion, info.installedBuild))
    },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateButton(
  text: String,
  trailing: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val height = ButtonDefaults.MediumContainerHeight
  Button(
    onClick = onClick,
    contentPadding = ButtonDefaults.contentPaddingFor(height),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .heightIn(height),
  ) {
    Text(
      text = text,
      modifier = Modifier.weight(1f),
    )
    if (trailing != null) {
      Text(trailing)
    }
  }
}

/** The summary under App updates in the settings list. */
@Composable
internal fun AppUpdatesSettingsInfo.summary(): String {
  val update = update ?: return stringResource(
    when {
      check.isChecking -> Res.string.updates_summary_checking
      check.failed -> Res.string.updates_summary_check_failed
      else -> Res.string.updates_summary_up_to_date
    },
  )
  return when (val status = update.status) {
    AppUpdate.Status.Available -> stringResource(Res.string.updates_summary_available, update.versionName)
    AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.updates_summary_reinstall, update.versionName)
    AppUpdate.Status.ChannelEnded -> stringResource(Res.string.updates_summary_channel_ended)
    is AppUpdate.Status.Downloading -> stringResource(
      Res.string.updates_summary_downloading,
      (status.progress * 100).toInt(),
    )
    AppUpdate.Status.ReadyToInstall -> stringResource(Res.string.updates_summary_ready)
    AppUpdate.Status.Installing -> stringResource(Res.string.updates_summary_installing)
    AppUpdate.Status.Failed -> stringResource(Res.string.updates_summary_failed)
  }
}

@Composable
private fun AppUpdateCheck.checkedLine(): String {
  val checkedAt = checkedAt
  return when {
    isChecking -> stringResource(Res.string.updates_kicker_checking)
    checkedAt != null -> stringResource(Res.string.updates_kicker_checked, checkedAt.toEpochMilliseconds().timeAgo)
    else -> stringResource(Res.string.updates_kicker_not_checked)
  }
}

@Composable
private fun AppUpdate.kicker(): String = when (val status = status) {
  AppUpdate.Status.Available -> stringResource(Res.string.updates_kicker_available)
  AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.updates_kicker_reinstall)
  AppUpdate.Status.ChannelEnded -> stringResource(Res.string.updates_kicker_channel_ended)
  is AppUpdate.Status.Downloading -> stringResource(
    Res.string.updates_kicker_downloading,
    (status.progress * 100).toInt(),
  )
  AppUpdate.Status.ReadyToInstall -> stringResource(Res.string.updates_kicker_ready)
  AppUpdate.Status.Installing -> stringResource(Res.string.updates_kicker_installing)
  AppUpdate.Status.Failed -> stringResource(Res.string.updates_kicker_failed)
}

@Composable
private fun AppUpdate.title(): String = when (status) {
  AppUpdate.Status.Available -> stringResource(Res.string.updates_title_available, versionName)
  AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.updates_title_reinstall, versionName)
  AppUpdate.Status.ChannelEnded -> stringResource(Res.string.updates_title_channel_ended)
  else -> stringResource(Res.string.updates_title_version, versionName)
}

@Composable
private fun AppUpdate.action(): String = when (status) {
  AppUpdate.Status.Available -> stringResource(Res.string.updates_action_update, versionName)
  AppUpdate.Status.ReinstallRequired -> stringResource(Res.string.updates_action_reinstall)
  AppUpdate.Status.ChannelEnded -> stringResource(Res.string.updates_action_switch)
  is AppUpdate.Status.Downloading,
  AppUpdate.Status.Installing,
  -> stringResource(Res.string.updates_action_view_progress)
  AppUpdate.Status.ReadyToInstall -> stringResource(Res.string.updates_action_install)
  AppUpdate.Status.Failed -> stringResource(Res.string.updates_action_retry)
}

/** "48 MB · 2 hours ago · PR #1234", from whichever of those the source knows. */
@Composable
private fun AppUpdate.meta(showPullRequest: Boolean): String? = listOfNotNull(
  sizeBytes?.asReadableBytes(),
  publishedAt?.toEpochMilliseconds()?.timeAgo,
  pullRequest?.takeIf { showPullRequest }?.let { stringResource(Res.string.updates_meta_pull_request, it) },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** The release notes as separate changes, a line each, without headings or list markers. */
internal fun AppUpdate.releaseNoteLines(): List<String> = releaseNotes
  ?.lines()
  ?.map { it.trim() }
  ?.filter { it.isNotEmpty() && !it.startsWith("#") }
  ?.map { it.removePrefix("- ").removePrefix("* ").trim() }
  .orEmpty()

/** Stands in for a progress fraction while an install's progress is unknown. */
private const val INDETERMINATE = -1f
