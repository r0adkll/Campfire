#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.screens.${NAME}Screen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@CircuitInject(${NAME}Screen::class, UserScope::class)
@Composable
fun ${NAME}(
  state: ${NAME}UiState,
  modifier: Modifier = Modifier,
) {
  TODO("Add your compose UI code here")
}