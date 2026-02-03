package app.campfire.whatsnew.ui.changelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.whatsnew.api.Changelog
import app.campfire.whatsnew.api.WhatsNewRepository
import app.campfire.whatsnew.api.screen.ChangelogScreen
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Bottom
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Middle
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Only
import app.campfire.whatsnew.ui.changelog.ChangeUi.Change.Position.Top
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ChangelogScreen::class, UserScope::class)
@Inject
class ChangelogPresenter(
  @Assisted private val navigator: Navigator,
  private val applicationInfo: ApplicationInfo,
  private val repository: WhatsNewRepository,
) : Presenter<ChangelogUiState> {

  @Composable
  override fun present(): ChangelogUiState {
    LaunchedEffect(Unit) {
      repository.dismissWhatsNew()
    }

    val collapsedVersions = rememberRetained {
      mutableStateMapOf(
        applicationInfo.versionName to false,
      )
    }

    val changeLog: LoadState<out Changelog> by remember {
      flow {
        try {
          val changelog = repository.getChangelog()
          changelog.changes
            .firstOrNull()
            ?.let {
              collapsedVersions[it.version] = false
            }
          emit(LoadState.Loaded(changelog))
        } catch (e: Exception) {
          emit(LoadState.Error)
        }
      }
    }.collectAsState(LoadState.Loading)

    val changeLogState by remember {
      derivedStateOf {
        changeLog.map { log ->
          buildUiChangelog(
            changelog = log,
            collapsedVersions = collapsedVersions,
          )
        }
      }
    }

    return ChangelogUiState(
      currentVersion = applicationInfo.versionName,
      changeLogState = changeLogState,
    ) { event ->
      when (event) {
        ChangelogUiEvent.Back -> navigator.pop()
        is ChangelogUiEvent.ToggleVersion -> {
          collapsedVersions[event.version.version] = !(collapsedVersions[event.version.version] ?: true)
        }
      }
    }
  }
}

fun buildUiChangelog(
  changelog: Changelog,
  collapsedVersions: Map<String, Boolean>,
): List<ChangeUi> {
  return buildList {
    changelog.changes.forEach { changes ->
      val isCollapsed = collapsedVersions[changes.version] ?: true
      add(ChangeUi.Version(changes.version, changes.date, isCollapsed))

      if (!isCollapsed) {
        changes.changes.forEach { changeSet ->
          changeSet.name?.let {
            add(ChangeUi.Category(it))
          }
          changeSet.changes.forEachIndexed { index, change ->
            add(
              ChangeUi.Change(
                text = change,
                position = when {
                  changeSet.changes.size == 1 -> Only
                  index == 0 -> Top
                  index == changeSet.changes.lastIndex -> Bottom
                  else -> Middle
                },
              ),
            )
          }
        }
      }
    }
  }
}
