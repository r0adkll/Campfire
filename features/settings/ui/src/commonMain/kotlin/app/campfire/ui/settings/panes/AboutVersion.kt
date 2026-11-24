// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.BuildCommit
import app.campfire.core.toast.Toast
import app.campfire.ui.settings.composables.ActionSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.about_version_clip_label
import campfire.features.settings.ui.generated.resources.about_version_copied
import campfire.features.settings.ui.generated.resources.about_version_copy_action
import campfire.features.settings.ui.generated.resources.about_version_title
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * The Version row in About: the version, plus the commit the build came from when known.
 * Long-pressing copies both for a bug report; [onClick] is the developer-mode tap counter, null
 * once developer mode is on.
 */
@Composable
internal fun VersionSetting(
  applicationInfo: ApplicationInfo,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
  val clipboard = LocalClipboard.current
  val toast = LocalToast.current
  val scope = rememberCoroutineScope()
  val copiedMessage = stringResource(Res.string.about_version_copied)
  val clipLabel = stringResource(Res.string.about_version_clip_label)
  val commitLine = applicationInfo.buildCommit?.line(timeZone)

  ActionSetting(
    headlineContent = { Text(stringResource(Res.string.about_version_title)) },
    supportingContent = {
      Column {
        Text(applicationInfo.readableVersion)
        commitLine?.let { Text(it) }
      }
    },
    onClick = onClick,
    onLongClick = {
      scope.launch {
        clipboard.setClipEntry(plainTextClipEntry(applicationInfo.versionCopyText(timeZone), clipLabel))
        toast.show(copiedMessage, Toast.Duration.SHORT)
      }
    },
    onLongClickLabel = stringResource(Res.string.about_version_copy_action),
    modifier = modifier,
  )
}

private const val SEPARATOR = " · "

private val CommitTimeFormat = LocalDateTime.Format {
  year()
  char('-')
  monthNumber()
  char('-')
  day()
  char(' ')
  hour()
  char(':')
  minute()
}

// Not UtcOffset's ISO format, which writes a zero offset as "Z".
private val OffsetFormat = UtcOffset.Format {
  offsetHours()
  char(':')
  offsetMinutesOfHour()
}

/** `1.4.0 (1040099)` */
internal val ApplicationInfo.readableVersion: String
  get() = "$versionName ($versionCode)"

/**
 * `2026-11-24 09:03 -05:00 (220c0be)`, in [timeZone]. Without a time it's just the SHA; without a
 * SHA, just the time.
 */
internal fun BuildCommit.line(timeZone: TimeZone): String {
  val formattedTime = time?.let { instant ->
    val local = instant.toLocalDateTime(timeZone)
    "${CommitTimeFormat.format(local)} ${OffsetFormat.format(timeZone.offsetAt(instant))}"
  }
  return when {
    formattedTime != null && sha != null -> "$formattedTime ($sha)"
    else -> formattedTime ?: sha.orEmpty()
  }
}

/** What a long-press on Version copies: [readableVersion] plus the commit [line] when known. */
internal fun ApplicationInfo.versionCopyText(timeZone: TimeZone): String =
  listOfNotNull(readableVersion, buildCommit?.line(timeZone)).joinToString(SEPARATOR)
