// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.campfire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.account.api.ServerRepository
import app.campfire.android.plugin.common.LoadingIndicator
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.common.SegmentedSection
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryRepository
import app.campfire.network.AuthAudioBookShelfApi
import com.livewire.ui.actions.clickAction
import com.livewire.ui.layout.Column
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Text
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.launch

/**
 * UserScope surface the Server tab resolves lazily — only available while a user
 * session exists.
 */
@ContributesTo(UserScope::class)
interface CampfireDebugUserComponent {
  val libraryRepository: LibraryRepository
}

/**
 * Current server details, settings, a live ping against the status endpoint, and
 * the libraries the server exposes.
 */
@Composable
internal fun ServerTab(
  serverRepository: ServerRepository,
  authApi: AuthAudioBookShelfApi,
) {
  val server by serverRepository.observeCurrentServer().collectAsState(initial = null)
  val scope = rememberCoroutineScope()
  var pingResult by remember { mutableStateOf<String?>(null) }
  var pinging by remember { mutableStateOf(false) }

  Column(
    LivewireModifier
      .fillMaxSize()
      .verticalScroll()
      .padding(horizontal = 16.dp),
  ) {
    val currentServer = server
    if (currentServer == null) {
      SectionHeader("Server")
      Text("No current server — log in first.", color = Color.Yellow)
      return@Column
    }

    SectionHeader("Server") {
      SectionButton(
        action = clickAction {
          if (!pinging) {
            pinging = true
            pingResult = null
            scope.launch {
              val startMs = System.currentTimeMillis()
              val result = authApi.status(currentServer.url)
              val elapsedMs = System.currentTimeMillis() - startMs
              pingResult = result.fold(
                onSuccess = { status ->
                  "OK in ${elapsedMs}ms — ${status.app} v${status.serverVersion} " +
                    "(language=${status.language}, authMethods=${status.authMethods.joinToString()})"
                },
                onFailure = { "Failed after ${elapsedMs}ms: ${it::class.simpleName}: ${it.message}" },
              )
              pinging = false
            }
          }
        },
      ) { Text(if (pinging) "Pinging…" else "Ping") }
    }
    SegmentedSection(
      title = "",
      rows = listOf(
        "name" to currentServer.name,
        "url" to currentServer.url,
        "user" to currentServer.user.name,
      ),
    )
    val ping = pingResult
    if (ping != null) {
      Text(
        text = ping,
        style = LivewireTheme.typography.bodySmall,
        color = if (ping.startsWith("OK")) Color.Green else Color.Red,
        modifier = LivewireModifier.padding(vertical = 8.dp),
      )
    }

    SegmentedSection(
      title = "Settings",
      rows = listOf(
        "version" to currentServer.settings.version,
        "language" to currentServer.settings.language,
        "chromecastEnabled" to currentServer.settings.chromecastEnabled.toString(),
        "backupSchedule" to currentServer.settings.backupSchedule,
        "dateFormat" to currentServer.settings.dateFormat,
        "timeFormat" to currentServer.settings.timeFormat,
        "logLevel" to currentServer.settings.logLevel.toString(),
      ),
    )

    LibrariesSection()
  }
}

@Composable
private fun LibrariesSection() {
  // LibraryRepository is UserScope — only resolvable while a user session exists.
  val userComponent = remember {
    runCatching { ComponentHolder.component<CampfireDebugUserComponent>() }.getOrNull()
  }
  SectionHeader("Libraries")
  if (userComponent == null) {
    Text("No user session — libraries unavailable.", color = Color.Gray)
    return
  }

  val libraries by userComponent.libraryRepository
    .observeAllLibraries(refresh = false)
    .collectAsState(initial = null)
  val currentLibrary by userComponent.libraryRepository
    .observeCurrentLibrary(refresh = false)
    .collectAsState(initial = null)

  val allLibraries = libraries
  if (allLibraries == null) {
    LoadingIndicator("Loading libraries…", LivewireModifier.fillMaxWidth().padding(16.dp))
  } else {
    SegmentedSection(
      title = "",
      rows = allLibraries.map { library ->
        val marker = if (library.id == currentLibrary?.id) " (current)" else ""
        "${library.name}$marker" to "${library.mediaType} · ${library.id}"
      },
    )
  }
}
