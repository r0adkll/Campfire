// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
#parse("File Header.java")
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.${NAME}Screen
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject

@CircuitInject(${NAME}Screen::class, UserScope::class)
@Inject
class ${NAME}Presenter(
  private val screen: ${NAME}Screen,
  private val navigator: Navigator,
) : Presenter<${NAME}UiState> {

  @Composable
  override fun present(): ${NAME}UiState {
    TODO("Handle your presentation logic here")
    
    return ${NAME}UiState { event ->
      TODO("Handle your events here")
    }
  }
}