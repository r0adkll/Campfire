// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.welcome

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.account.api.RestorableAccount
import app.campfire.auth.ui.login.LoginUiState
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.ui.theming.api.AppTheme
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the phone welcome screen with accounts waiting to be restored and writes a PNG to
 * `build/renders` for eyeballing. The assertion only guards that it composes and paints.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
class WelcomeRestorableAccountsRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `restored accounts sit under the add campsite card in light mode`() {
    render("welcome-restorable-accounts-light", useDarkColors = false)
  }

  @Test
  fun `restored accounts sit under the add campsite card in dark mode`() {
    render("welcome-restorable-accounts-dark", useDarkColors = true)
  }

  private fun render(name: String, useDarkColors: Boolean) {
    render(name, width = 400, height = 860) {
      PreviewSharedElementTransitionLayout {
        CampfireTheme(useDarkColors = useDarkColors) {
          CompositionLocalProvider(
            LocalWindowSizeClass provides currentWindowSizeClass(),
            LocalUseDarkColors provides useDarkColors,
          ) {
            // The app's root provides the background
            Surface(Modifier.fillMaxSize()) {
              Welcome(
                state = WelcomeUiState(
                  loginUiState = LoginUiState(
                    theme = AppTheme.Fixed.Tent,
                    serverName = "",
                    serverUrl = "",
                    connectionState = null,
                    userName = "",
                    password = "",
                    isAuthenticating = false,
                    authError = null,
                    networkSettings = null,
                    restorableAccounts = listOf(
                      RestorableAccount(
                        serverUrl = "https://abs.example.com",
                        serverName = "Home",
                        userId = "user-alice",
                        userName = "alice",
                      ),
                      RestorableAccount(
                        serverUrl = "http://192.168.1.50:13378/",
                        serverName = "Library",
                        userId = "user-bob",
                        userName = "bob",
                      ),
                    ),
                    restoredTheme = AppTheme.Fixed.Forest,
                    eventSink = {},
                  ),
                  eventSink = {},
                ),
                modifier = Modifier,
              )
            }
          }
        }
      }
    }
  }

  private fun render(
    name: String,
    width: Int,
    height: Int,
    content: @Composable () -> Unit,
  ) {
    ImageComposeScene(width = width * 2, height = height * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      val image = scene.render(nanoTime = 1_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      assertThat(bytes.size).isGreaterThan(1_000)
    }
  }
}
