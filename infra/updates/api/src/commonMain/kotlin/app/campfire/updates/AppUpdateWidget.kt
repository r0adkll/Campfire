package app.campfire.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A small, dismissable widget that surfaces app update state on builds that support
 * in-app updates (see [app.campfire.updates.source.AppUpdateSource.isSupported]).
 *
 * - If the user is not signed-in to receive updates, a compact sign-in prompt is shown
 *   that can be permanently dismissed.
 * - If the user is signed-in and an update is available, a compact "update available"
 *   card is shown that opens a bottom sheet with the full release details, download
 *   action, and download progress. Dismissing it hides the card for that release
 *   until a different one becomes available.
 * - Otherwise, nothing is rendered.
 */
interface AppUpdateWidget {

  @Composable
  fun Content(modifier: Modifier)
}
