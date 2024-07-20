package app.campfire.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.LoginScreen
import app.campfire.core.di.MergeActivityScope
import com.r0adkll.kotlininject.merge.annotations.CircuitInject

@CircuitInject(MergeActivityScope::class, LoginScreen::class)
@Composable
fun Login(
  state: LoginUiState,
  modifier: Modifier = Modifier,
) {

}
