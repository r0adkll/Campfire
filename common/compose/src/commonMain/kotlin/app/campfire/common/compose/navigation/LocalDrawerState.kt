package app.campfire.common.compose.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * This provides a way for content to grab a handle on the drawer state for the NavigationDrawer, if present, in the
 * composition
 */
val LocalDrawerState = compositionLocalOf<DrawerState?> { null }

@Composable
fun localDrawerOpener(): () -> Unit {
  val coroutineScope = rememberCoroutineScope()
  val drawerState = rememberUpdatedState(LocalDrawerState.current)

  return remember {
    {
      coroutineScope.launch {
        drawerState.value?.open()
      }
    }
  }
}
