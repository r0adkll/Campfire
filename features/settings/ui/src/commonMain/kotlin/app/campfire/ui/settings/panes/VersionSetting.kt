// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.app.ApplicationInfo
import app.campfire.ui.settings.composables.ActionSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.about_version_title
import campfire.features.settings.ui.generated.resources.build_info_copy_action
import org.jetbrains.compose.resources.stringResource

/**
 * The Version row in About, showing the build info. Long-pressing copies it for a bug report;
 * [onClick] is the developer-mode tap counter, null once developer mode is on.
 */
@Composable
internal fun VersionSetting(
  applicationInfo: ApplicationInfo,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val version = applicationInfo.readableVersion
  val copy = copyBuildInfoAction(version)

  ActionSetting(
    headlineContent = { Text(stringResource(Res.string.about_version_title)) },
    supportingContent = { Text(version) },
    onClick = onClick,
    onLongClick = copy,
    onLongClickLabel = stringResource(Res.string.build_info_copy_action),
    modifier = modifier,
  )
}
