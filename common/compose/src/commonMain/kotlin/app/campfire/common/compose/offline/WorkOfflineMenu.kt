// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.offline

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
 * Its own popup rather than Material's menu, which draws its own container and keeps 48dp clear
 * of the window's bottom edge: this one is only its cards, opening just above its anchor in the
 * drawer's footer.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
  val visibility = remember { MutableTransitionState(false) }
  visibility.targetState = expanded
  // Stays in composition until it has animated out
  if (!visibility.currentState && !visibility.targetState) return

  val density = LocalDensity.current
  val positionProvider = remember(density) { AboveAnchorPositionProvider(density) }
  Popup(
    popupPositionProvider = positionProvider,
    onDismissRequest = onDismissRequest,
    properties = PopupProperties(focusable = true),
  ) {
    val transition = rememberTransition(visibility, label = "WorkOfflineMenu")
    val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val alphaSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val scale by transition.animateFloat(transitionSpec = { scaleSpec }) { if (it) 1f else 0.8f }
    val alpha by transition.animateFloat(transitionSpec = { alphaSpec }) { if (it) 1f else 0f }

    Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = modifier
        // Room for the cards' shadows, which the popup's window would otherwise clip
        .padding(start = ShadowClearance, top = ShadowClearance, end = ShadowClearance, bottom = ShadowDrop)
        .width(IntrinsicSize.Max)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
          this.alpha = alpha
          transformOrigin = positionProvider.transformOrigin
        },
    ) {
      val reason = state.reason
      if (reason != null) {
        // Tinted like the drawer's offline button, so the status reads as the state it's in
        MenuCard(
          shape = LeadingCardShape,
          color = MaterialTheme.colorScheme.tertiaryContainer,
          border = null,
        ) {
          MenuHeader(reason.statusText(), color = LocalContentColor.current)
        }
        MenuCard(shape = TrailingCardShape) {
          MenuRow(
            icon = CampfireIcons.Rounded.CloudDone,
            text = reason.goOnlineLabel(),
            onClick = {
              onDismissRequest()
              onGoOnline()
            },
          )
        }
      } else {
        MenuCard(shape = StandaloneCardShape) {
          MenuHeader(stringResource(Res.string.work_offline_title))
          offlineChoices(canDetectWifi)
            .forEach { choice ->
              MenuRow(
                icon = choice.icon,
                text = choice.label(onWifi = state.onWifi),
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
}

/**
 * One card of the menu, set apart from the drawer it opens over: a step up from its surface
 * container, outlined, and raised.
 */
@Composable
private fun MenuCard(
  shape: Shape,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
  border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    shape = shape,
    color = color,
    border = border,
    shadowElevation = 6.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 4.dp),
      content = content,
    )
  }
}

private val StandaloneCardShape = RoundedCornerShape(16.dp)
private val LeadingCardShape =
  RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val TrailingCardShape =
  RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

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

@Composable
private fun MenuRow(
  icon: ImageVector,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(24.dp),
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
    )
  }
}

/**
 * Places the menu's bottom edge on its anchor's top edge, start-aligned with it, or below the
 * anchor when there's more room there; either way kept inside the window. Offsets by
 * [ShadowClearance] and [ShadowDrop] so the visible menu, not its padding, lines up with the anchor.
 */
internal class AboveAnchorPositionProvider(density: Density) : PopupPositionProvider {
  private val clearance = with(density) { ShadowClearance.roundToPx() }
  private val drop = with(density) { ShadowDrop.roundToPx() }

  /** Where the menu grows from: the anchor's side of it. */
  var transformOrigin by mutableStateOf(TransformOrigin(0f, 1f))
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
