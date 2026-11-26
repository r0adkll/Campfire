// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.asComposeIcon
import app.campfire.common.compose.icons.rounded.UnfoldMore
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import campfire.ui.appbar.generated.resources.Res
import campfire.ui.appbar.generated.resources.library_switcher_action_next
import campfire.ui.appbar.generated.resources.library_switcher_action_open
import campfire.ui.appbar.generated.resources.library_switcher_action_previous
import campfire.ui.appbar.generated.resources.library_switcher_description
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * What the app bar's library switcher shows and does. Null wherever the switcher stays hidden,
 * e.g. when the user only has one library to pick from.
 */
@Immutable
internal class LibrarySwitcherModel(
  val currentLibrary: Library,
  /** Every library, in the order the switcher steps through them. */
  val libraries: List<Library>,
  val onLibrarySelected: (Library) -> Unit,
)

/** The model for the switcher, or null when there's nothing to switch between. */
internal fun librarySwitcherModel(
  currentLibrary: Library?,
  libraries: List<Library>,
  onLibrarySelected: (Library) -> Unit,
): LibrarySwitcherModel? {
  if (currentLibrary == null || libraries.size < 2) return null
  if (libraries.none { it.id == currentLibrary.id }) return null
  return LibrarySwitcherModel(currentLibrary, libraries, onLibrarySelected)
}

private val ChipHeight = 40.dp

/** How far a vertical swipe travels to step one library. */
private val SwipeStep = 32.dp

/**
 * The library a vertical swipe of [dragY] lands on, starting from [startIndex]. Swiping up steps
 * forward through the list like a picker wheel, and the ends hold rather than wrapping.
 */
internal fun swipeTargetIndex(startIndex: Int, dragY: Float, stepPx: Float, count: Int): Int {
  return (startIndex - (dragY / stepPx).toInt()).coerceIn(0, count - 1)
}

/**
 * A compact chip showing the current library's icon that switches libraries three ways:
 *
 * - Tap to open a menu of every library.
 * - Swipe vertically to step through the libraries, committing the one shown on release.
 * - Long press to open the menu, then slide onto a library and let go to pick it.
 */
@Composable
internal fun LibrarySwitcher(
  model: LibrarySwitcherModel,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val tooltipState = rememberTooltipState(isPersistent = true)

  val currentModel by rememberUpdatedState(model)
  var menuExpanded by remember { mutableStateOf(false) }

  // The library a swipe is landing on, shown in the chip until the selection catches up.
  var pendingLibrary by remember { mutableStateOf<Library?>(null) }
  // The menu item under the finger while long-press dragging.
  var highlightedId by remember { mutableStateOf<LibraryId?>(null) }
  // While the finger that opened the menu is still down. A focusable popup would take that
  // finger's events from the chip, so the menu only takes focus once it lifts.
  var dragSelecting by remember { mutableStateOf(false) }

  // Coordinates resolve on screen so the finger, owned by the chip's window, can be tested
  // against items laid out in the menu's popup.
  var chipCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  val itemBounds = remember { mutableStateMapOf<LibraryId, Rect>() }

  LaunchedEffect(model.currentLibrary.id) { pendingLibrary = null }

  fun select(library: Library) {
    menuExpanded = false
    highlightedId = null
    if (library.id != currentModel.currentLibrary.id) {
      haptics.performHapticFeedback(HapticFeedbackType.Confirm)
      pendingLibrary = library
      currentModel.onLibrarySelected(library)
    }
  }

  fun step(by: Int): Boolean {
    val libraries = currentModel.libraries
    val index = libraries.indexOfFirst { it.id == currentModel.currentLibrary.id }
    val target = libraries.getOrNull(index + by) ?: return false
    select(target)
    return true
  }

  val displayedLibrary = pendingLibrary ?: model.currentLibrary
  val description = stringResource(Res.string.library_switcher_description, displayedLibrary.name)
  val openLabel = stringResource(Res.string.library_switcher_action_open)
  val nextLabel = stringResource(Res.string.library_switcher_action_next)
  val previousLabel = stringResource(Res.string.library_switcher_action_previous)

  Box(modifier) {
    TooltipBox(
      positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
      tooltip = {
        PlainTooltip(
          caretShape = TooltipDefaults.caretShape(),
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            text = displayedLibrary.name,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
          )
        }
      },
      state = tooltipState,
      enableUserInput = false,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(end = 4.dp)
          .height(ChipHeight)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer)
          .onGloballyPositioned { chipCoordinates = it }
          .semantics(mergeDescendants = true) {
            role = Role.DropdownList
            contentDescription = description
            onClick(label = openLabel) {
              menuExpanded = true
              true
            }
            customActions = listOf(
              CustomAccessibilityAction(nextLabel) { step(1) },
              CustomAccessibilityAction(previousLabel) { step(-1) },
            )
          }
          .pointerInput(Unit) {
            awaitEachGesture {
              val down = awaitFirstDown()
              // Keep the search field around the chip from treating this as a tap of its own
              down.consume()
              val gesture = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                awaitTapOrSwipe(down.id, down.position, viewConfiguration.touchSlop)
              } ?: ChipGesture.LongPress

              when (gesture) {
                ChipGesture.Cancel -> Unit
                ChipGesture.Tap -> menuExpanded = true

                ChipGesture.Swipe -> {
                  val libraries = currentModel.libraries
                  val startIndex = libraries.indexOfFirst { it.id == currentModel.currentLibrary.id }
                  var index = startIndex
                  scope.launch { tooltipState.show() }

                  val released = trackPointer(down.id) { change ->
                    val target = swipeTargetIndex(
                      startIndex = startIndex,
                      dragY = change.position.y - down.position.y,
                      stepPx = SwipeStep.toPx(),
                      count = libraries.size,
                    )
                    if (target != index) {
                      index = target
                      pendingLibrary = libraries[target].takeIf { target != startIndex }
                      haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                  }

                  tooltipState.dismiss()
                  if (released && index != startIndex) {
                    select(libraries[index])
                  } else {
                    pendingLibrary = null
                  }
                }

                ChipGesture.LongPress -> {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  dragSelecting = true
                  menuExpanded = true
                  var moved = false

                  val released = trackPointer(down.id) { change ->
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                      moved = true
                    }
                    // The menu opens over the chip, so the finger starts on a row: only pick
                    // once it slides, or a wobble on release would select that row
                    val onScreen = chipCoordinates?.localToScreen(change.position)?.takeIf { moved }
                    val hit = onScreen?.let { point ->
                      currentModel.libraries.firstOrNull { itemBounds[it.id]?.contains(point) == true }
                    }
                    if (hit?.id != highlightedId) {
                      highlightedId = hit?.id
                      if (hit != null) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                  }

                  dragSelecting = false
                  val picked = currentModel.libraries.firstOrNull { it.id == highlightedId }
                  when {
                    released && picked != null -> select(picked)
                    // Let go without sliding: leave the menu open to tap through
                    !moved -> highlightedId = null
                    // Slid off the menu: treat it as a cancel
                    else -> {
                      menuExpanded = false
                      highlightedId = null
                    }
                  }
                }
              }
            }
          }
          .padding(start = 10.dp, end = 4.dp),
      ) {
        AnimatedContent(
          targetState = displayedLibrary,
          contentKey = { it.id },
          transitionSpec = {
            val ids = model.libraries.map { it.id }
            val forward = ids.indexOf(targetState.id) >= ids.indexOf(initialState.id)
            val direction = if (forward) 1 else -1
            (slideInVertically { direction * it } + fadeIn()) togetherWith
              (slideOutVertically { -direction * it } + fadeOut()) using
              SizeTransform(clip = true)
          },
        ) { library ->
          Icon(
            imageVector = library.icon.asComposeIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
          )
        }
        Icon(
          imageVector = CampfireIcons.Rounded.UnfoldMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    DropdownMenu(
      expanded = menuExpanded,
      properties = PopupProperties(focusable = !dragSelecting),
      // Open over the chip rather than below it
      offset = DpOffset(x = 0.dp, y = -ChipHeight),
      shape = MaterialTheme.shapes.medium,
      // The chip's colors, so the menu reads as the chip opening up
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      onDismissRequest = {
        menuExpanded = false
        highlightedId = null
      },
    ) {
      val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
      model.libraries.forEach { library ->
        val selected = library.id == model.currentLibrary.id
        DropdownMenuItem(
          text = {
            Text(
              text = library.name,
              style = MaterialTheme.typography.bodyLarge,
            )
          },
          leadingIcon = { Icon(library.icon.asComposeIcon(), contentDescription = null) },
          // Radio buttons, matching the library picker in the drawer
          trailingIcon = {
            RadioButton(
              selected = selected,
              onClick = null,
              colors = RadioButtonDefaults.colors(
                selectedColor = contentColor,
                unselectedColor = contentColor,
              ),
            )
          },
          onClick = { select(library) },
          colors = MenuDefaults.itemColors(
            textColor = contentColor,
            leadingIconColor = contentColor,
            trailingIconColor = contentColor,
          ),
          modifier = Modifier
            .onGloballyPositioned { coordinates ->
              itemBounds[library.id] = Rect(
                offset = coordinates.localToScreen(Offset.Zero),
                size = coordinates.size.toSize(),
              )
            }
            .background(
              if (highlightedId == library.id) {
                contentColor.copy(alpha = 0.1f)
              } else {
                Color.Transparent
              },
            ),
        )
      }
    }
  }
}

private enum class ChipGesture { Tap, Swipe, LongPress, Cancel }

/**
 * Wait out the start of a gesture: a release is a tap, crossing the touch slop vertically is a
 * swipe, and crossing it sideways hands the gesture back. The caller's timeout makes a long press.
 */
private suspend fun AwaitPointerEventScope.awaitTapOrSwipe(
  pointerId: PointerId,
  downPosition: Offset,
  touchSlop: Float,
): ChipGesture {
  while (true) {
    val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
      ?: return ChipGesture.Cancel
    if (change.changedToUp()) {
      change.consume()
      return ChipGesture.Tap
    }
    if (change.isConsumed) return ChipGesture.Cancel
    val delta = change.position - downPosition
    if (abs(delta.y) > touchSlop && abs(delta.y) >= abs(delta.x)) {
      change.consume()
      return ChipGesture.Swipe
    }
    if (abs(delta.x) > touchSlop) return ChipGesture.Cancel
  }
}

/**
 * Feed each move of [pointerId] to [onMove], consuming it, until the pointer lifts. Returns true
 * for a clean release and false when the gesture was cancelled out from under us.
 */
private suspend fun AwaitPointerEventScope.trackPointer(
  pointerId: PointerId,
  onMove: (PointerInputChange) -> Unit,
): Boolean {
  while (true) {
    val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId } ?: return false
    if (change.changedToUp()) {
      change.consume()
      return true
    }
    if (!change.pressed) return false
    onMove(change)
    change.consume()
  }
}
