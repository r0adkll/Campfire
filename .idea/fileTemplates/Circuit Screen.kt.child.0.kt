#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class ${NAME}UiState(
  // TODO: Your state variables here
  val eventSink: (${NAME}UiEvent) -> Unit,
) : CircuitUiState

sealed interface ${NAME}UiEvent : CircuitUiEvent