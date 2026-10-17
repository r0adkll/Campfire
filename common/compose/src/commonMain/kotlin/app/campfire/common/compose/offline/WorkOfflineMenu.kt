// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.offline

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CalendarClock
import app.campfire.common.compose.icons.rounded.CloudDone
import app.campfire.common.compose.icons.rounded.CloudOff
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.icons.rounded.Wifi
import app.campfire.common.compose.time.is24HourFormat
import app.campfire.network.reachability.OfflineChoice
import app.campfire.network.reachability.OfflineReason
import app.campfire.network.reachability.OfflineState
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.work_offline_choice_four_hours
import campfire.common.compose.generated.resources.work_offline_choice_one_hour
import campfire.common.compose.generated.resources.work_offline_choice_until_tomorrow
import campfire.common.compose.generated.resources.work_offline_choice_until_turned_off
import campfire.common.compose.generated.resources.work_offline_choice_until_wifi
import campfire.common.compose.generated.resources.work_offline_choice_until_wifi_reconnect
import campfire.common.compose.generated.resources.work_offline_go_online
import campfire.common.compose.generated.resources.work_offline_go_online_until_wifi
import campfire.common.compose.generated.resources.work_offline_status_mobile_data
import campfire.common.compose.generated.resources.work_offline_status_until_today
import campfire.common.compose.generated.resources.work_offline_status_until_tomorrow
import campfire.common.compose.generated.resources.work_offline_status_until_turned_off
import campfire.common.compose.generated.resources.work_offline_status_until_wifi
import campfire.common.compose.generated.resources.work_offline_status_until_wifi_reconnect
import campfire.common.compose.generated.resources.work_offline_title
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * The ways to work offline, or — while offline — why and until when, with a way back online.
 * Shared by the navigation drawer and Settings so both always offer the same thing.
 */
@Composable
fun WorkOfflineMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  state: OfflineState,
  canDetectWifi: Boolean,
  onGoOffline: (OfflineChoice) -> Unit,
  onGoOnline: () -> Unit,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp),
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.medium,
    offset = offset,
    modifier = modifier,
  ) {
    val reason = state.reason
    if (reason != null) {
      MenuHeader(reason.statusText())
      DropdownMenuItem(
        text = { Text(reason.goOnlineLabel()) },
        leadingIcon = { Icon(CampfireIcons.Rounded.CloudDone, contentDescription = null) },
        onClick = {
          onDismissRequest()
          onGoOnline()
        },
      )
    } else {
      MenuHeader(stringResource(Res.string.work_offline_title))
      offlineChoices(canDetectWifi)
        .forEach { choice ->
          DropdownMenuItem(
            text = { Text(choice.label(onWifi = state.onWifi)) },
            leadingIcon = { Icon(choice.icon, contentDescription = null) },
            onClick = {
              onDismissRequest()
              onGoOffline(choice)
            },
          )
        }
    }
  }
}

@Composable
private fun MenuHeader(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
  )
}

/** Why Campfire is working offline, e.g. "Offline until 3:45 PM". */
@Composable
fun OfflineReason.statusText(): String = when (this) {
  OfflineReason.UntilTurnedOff -> stringResource(Res.string.work_offline_status_until_turned_off)
  is OfflineReason.Until -> {
    val zone = TimeZone.currentSystemDefault()
    val ends = Instant.fromEpochMilliseconds(endsAtMillis).toLocalDateTime(zone)
    val today = Clock.System.now().toLocalDateTime(zone).date
    val time = ends.time.format(clockFormat())
    if (ends.date == today) {
      stringResource(Res.string.work_offline_status_until_today, time)
    } else {
      stringResource(Res.string.work_offline_status_until_tomorrow, time)
    }
  }
  is OfflineReason.UntilWifi -> stringResource(
    if (reconnect) Res.string.work_offline_status_until_wifi_reconnect else Res.string.work_offline_status_until_wifi,
  )
  OfflineReason.OnMobileData -> stringResource(Res.string.work_offline_status_mobile_data)
}

/** The ways to work offline this device offers: the Wi-Fi one only where it can tell Wi-Fi apart. */
fun offlineChoices(canDetectWifi: Boolean): List<OfflineChoice> =
  OfflineChoice.entries.filter { it != OfflineChoice.UntilWifi || canDetectWifi }

/** How to end working offline for this reason: the mobile data rule only stands aside until Wi-Fi. */
@Composable
fun OfflineReason.goOnlineLabel(): String = stringResource(
  if (this == OfflineReason.OnMobileData) {
    Res.string.work_offline_go_online_until_wifi
  } else {
    Res.string.work_offline_go_online
  },
)

/** The choice as an option, e.g. "For 1 hour"; the Wi-Fi one reads by whether it's on Wi-Fi now. */
@Composable
fun OfflineChoice.label(onWifi: Boolean): String = stringResource(
  when (this) {
    OfflineChoice.UntilTurnedOff -> Res.string.work_offline_choice_until_turned_off
    OfflineChoice.OneHour -> Res.string.work_offline_choice_one_hour
    OfflineChoice.FourHours -> Res.string.work_offline_choice_four_hours
    OfflineChoice.UntilTomorrow -> Res.string.work_offline_choice_until_tomorrow
    OfflineChoice.UntilWifi -> if (onWifi) {
      Res.string.work_offline_choice_until_wifi_reconnect
    } else {
      Res.string.work_offline_choice_until_wifi
    }
  },
)

val OfflineChoice.icon: ImageVector
  get() = when (this) {
    OfflineChoice.UntilTurnedOff -> CampfireIcons.Rounded.CloudOff
    OfflineChoice.OneHour, OfflineChoice.FourHours -> CampfireIcons.Rounded.Schedule
    OfflineChoice.UntilTomorrow -> CampfireIcons.Rounded.CalendarClock
    OfflineChoice.UntilWifi -> CampfireIcons.Rounded.Wifi
  }

@Composable
private fun clockFormat(): DateTimeFormat<LocalTime> = if (is24HourFormat()) {
  LocalTime.Format {
    hour(Padding.NONE); char(':'); minute()
  }
} else {
  LocalTime.Format {
    amPmHour(Padding.NONE); char(':'); minute(); char(' '); amPmMarker("AM", "PM")
  }
}
