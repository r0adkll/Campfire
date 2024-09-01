package app.campfire.common.compose.navigation

import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.runtime.screen.Screen

val LocalRootScreen = compositionLocalOf<Screen> {
  error("No local root screen provided")
}
