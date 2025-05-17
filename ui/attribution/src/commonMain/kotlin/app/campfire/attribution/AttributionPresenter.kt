package app.campfire.attribution

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.AttributionScreen
import app.campfire.core.attributions.LicenseAttributionLoader
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

@Inject
class AttributionPresenter(
  private val licenseAttributionLoader: LicenseAttributionLoader,
  @Assisted val navigator: Navigator,
) : Presenter<AttributionUiState> {

  @CircuitInject(AttributionScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): AttributionPresenter
  }

  @Composable
  override fun present(): AttributionUiState {
    val attributions by remember {
      flow {
        val libs = licenseAttributionLoader.load()
        emit(LoadState.Loaded(libs))
      }.catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return AttributionUiState(
      attributionState = attributions,
    ) { event ->
      when (event) {
        AttributionUiEvent.Back -> navigator.pop()
      }
    }
  }
}
