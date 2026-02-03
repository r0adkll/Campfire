package app.campfire.whatsnew.ui.changelog

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class ChangelogUiState(
  val currentVersion: String,
  val changeLogState: LoadState<out List<ChangeUi>>,
  val eventSink: (ChangelogUiEvent) -> Unit,
) : CircuitUiState

@Immutable
sealed interface ChangeUi {
  @Immutable
  data class Version(
    val version: String,
    val date: String?,
    val collapsed: Boolean,
  ) : ChangeUi

  @Immutable
  data class Category(
    val name: String,
  ) : ChangeUi

  @Immutable
  data class Change(
    val text: String,
    val position: Position = Position.Middle,
  ) : ChangeUi {
    enum class Position {
      Top, Middle, Bottom, Only
    }
  }
}

sealed interface ChangelogUiEvent : CircuitUiEvent {
  data object Back : ChangelogUiEvent
  data class ToggleVersion(val version: ChangeUi.Version) : ChangelogUiEvent
}
