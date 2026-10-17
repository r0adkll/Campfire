// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
 *
 * It opens just above its anchor, like a menu from the drawer's footer. Material's own placement
 * keeps 48dp clear of the window's bottom edge, so an anchor that close left the menu floating
 * well above it.
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
) {
  val density = LocalDensity.current
  val positionProvider = remember(density) { AboveAnchorPositionProvider(density) }
  DropdownMenuPopup(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    popupPositionProvider = positionProvider,
    // Room for the groups' shadows, which the popup's window would otherwise clip
    modifier = modifier.padding(
      start = ShadowClearance,
      top = ShadowClearance,
      end = ShadowClearance,
      bottom = ShadowDrop,
    ),
  ) {
    val reason = state.reason
    if (reason != null) {
      // Tinted like the drawer's offline button, so the status reads as the state it's in
      MenuGroup(
        shapes = MenuDefaults.groupShape(index = 0, count = 2),
        containerColor = MenuDefaults.groupVibrantContainerColor,
        border = null,
        modifier = Modifier.fillMaxWidth(),
      ) {
        MenuHeader(
          text = reason.statusText(),
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
      Spacer(Modifier.height(MenuDefaults.GroupSpacing))
      MenuGroup(
        shapes = MenuDefaults.groupShape(index = 1, count = 2),
      ) {
        DropdownMenuItem(
          text = { Text(reason.goOnlineLabel()) },
          leadingIcon = { Icon(CampfireIcons.Rounded.CloudDone, contentDescription = null) },
          onClick = {
            onDismissRequest()
            onGoOnline()
          },
        )
      }
    } else {
      MenuGroup(
        shapes = MenuDefaults.groupShape(index = 0, count = 1),
      ) {
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
}

/**
 * A menu group set apart from the drawer it opens over: a step up from its surface container,
 * outlined, and raised a level above Material's default.
 */
@Composable
private fun MenuGroup(
  shapes: MenuGroupShapes,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
  border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  content: @Composable ColumnScope.() -> Unit,
) {
  DropdownMenuGroup(
    shapes = shapes,
    containerColor = containerColor,
    shadowElevation = 6.dp,
    border = border,
    modifier = modifier,
    content = content,
  )
}

private val ShadowClearance = 8.dp

/** Shadows fall below their surface, so they need more room there than to the sides. */
private val ShadowDrop = 16.dp

@Composable
private fun MenuHeader(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = color,
    modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
  )
}

/**
 * Places the menu's bottom edge on its anchor's top edge, start-aligned with it, or below the
 * anchor when there's more room there; either way kept inside the window. Offsets by
 * [ShadowClearance] and [ShadowDrop] so the visible menu, not its padding, lines up with the anchor.
 */
internal class AboveAnchorPositionProvider(density: Density) : DropdownMenuPopupPositionProvider {
  private val clearance = with(density) { ShadowClearance.roundToPx() }
  private val drop = with(density) { ShadowDrop.roundToPx() }

  override var transformOrigin by mutableStateOf(TransformOrigin(0f, 1f))
    private set

  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val start = if (layoutDirection == LayoutDirection.Ltr) {
      anchorBounds.left - clearance
    } else {
      anchorBounds.right + clearance - popupContentSize.width
    }
    val x = start.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))

    val above = anchorBounds.top + drop - popupContentSize.height
    val opensAbove = above >= 0 || anchorBounds.top > windowSize.height - anchorBounds.bottom
    val y = if (opensAbove) above else anchorBounds.bottom - clearance

    transformOrigin = TransformOrigin(
      pivotFractionX = if (popupContentSize.width == 0) {
        0f
      } else {
        ((anchorBounds.center.x - x).toFloat() / popupContentSize.width).coerceIn(0f, 1f)
      },
      pivotFractionY = if (opensAbove) 1f else 0f,
    )
    return IntOffset(x, y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)))
  }
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
