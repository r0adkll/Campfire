// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.BuildCommit
import app.campfire.core.toast.Toast
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.build_info_clip_label
import campfire.features.settings.ui.generated.resources.build_info_copied
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** Copies build info [text] to the clipboard and confirms it with a toast. */
@Composable
internal fun copyBuildInfoAction(text: String): () -> Unit {
  val clipboard = LocalClipboard.current
  val toast = LocalToast.current
  val scope = rememberCoroutineScope()
  val copiedMessage = stringResource(Res.string.build_info_copied)
  val clipLabel = stringResource(Res.string.build_info_clip_label)
  return {
    scope.launch {
      clipboard.setClipEntry(plainTextClipEntry(text, clipLabel))
      toast.show(copiedMessage, Toast.Duration.SHORT)
    }
  }
}

/** `1.4.0 (1040099) [220c0be]`, or `1.4.0 (1040099)` without a SHA. */
internal fun readableVersion(versionName: String, versionCode: Long, sha: String?): String = buildString {
  append(versionName).append(" (").append(versionCode).append(')')
  sha?.let { append(" [").append(it).append(']') }
}

/** This build's [readableVersion]. */
internal val ApplicationInfo.readableVersion: String
  get() = readableVersion(versionName, versionCode.toLong(), buildCommit?.sha)

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

/** `2026-11-24 09:03 -05:00`: the commit time in [timeZone], with its offset; null when unknown. */
internal fun BuildCommit.formattedTime(timeZone: TimeZone): String? = time?.let { instant ->
  "${CommitTimeFormat.format(instant.toLocalDateTime(timeZone))} ${OffsetFormat.format(timeZone.offsetAt(instant))}"
}
