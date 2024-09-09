#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.${NAME}Screen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(${NAME}Screen::class, UserScope::class)
@Inject
class ${NAME}Presenter(
  @Assisted private val screen: ${NAME}Screen,
  @Assisted private val navigator: Navigator,
) : Presenter<${NAME}UiState> {

  @Composable
  override fun present(): ${NAME}UiState {
    TODO("Handle your presentation logic here")
    
    return ${NAME}UiState { event ->
      TODO("Handle your events here")
    }
  }
}