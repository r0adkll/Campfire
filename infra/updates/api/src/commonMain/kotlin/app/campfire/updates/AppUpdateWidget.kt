package app.campfire.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A widget implementation that dynamically shows UI to prompt the user to update the app.
 * On Beta builds:
 *   - This will check if the user is signed into the app tester account.
 *   - If so, it will show a prompt to update the app.
 * On StandardRelease builds:
 *   - This will use the GooglePlay InAppUpdates to check for new releases
 *   - If so, it will prompt the user to update the app.
 *
 * The sign-in prompt can be permanently dismissed by the user, in which case
 * nothing is rendered until an update is otherwise available.
 */
interface AppUpdateWidget {

  @Composable
  fun Content(modifier: Modifier)
}
