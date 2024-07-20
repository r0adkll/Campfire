package app.campfire.auth.ui.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.WelcomeScreen
import app.campfire.core.di.MergeActivityScope
import com.r0adkll.kotlininject.merge.annotations.CircuitInject
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ui.ui

@CircuitInject(MergeActivityScope::class, WelcomeScreen::class)
@Composable
fun Welcome(
  modifier: Modifier,
) {

}
