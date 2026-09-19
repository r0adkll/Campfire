// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackActionsFit
import app.campfire.sessions.ui.playback.expanded.composables.rememberUseCompact
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * Drives [PlaybackActionsFit] through a stand-in for the panel — a square cover in a weighted slot
 * above an actions row that is shorter when compact — and shrinks the column the way the sheet does.
 */
@OptIn(ExperimentalComposeUiApi::class)
class PlaybackActionsFitTest {

  private val coverWidth = 300
  private val stackedActionsHeight = 150
  private val compactActionsHeight = 80

  @Test
  fun `a tall panel keeps the stacked actions`() = panel(height = 500) {
    assertThat(useCompact).isFalse()
  }

  @Test
  fun `squeezing the cover switches to the compact actions`() = panel(height = 500) {
    resizeTo(400)
    assertThat(useCompact).isTrue()
  }

  @Test
  fun `the compact actions do not flip back once the freed height regrows the cover`() {
    panel(height = 500) {
      resizeTo(400)
      assertThat(useCompact).isTrue()

      // The compact row frees 70dp, so the cover is full width again — the height it would have
      // with the stacked row is what decides, so it has to stay compact rather than oscillate.
      repeat(3) { settle() }
      assertThat(useCompact).isTrue()
    }
  }

  @Test
  fun `a drag holds the layout it started with`() = panel(height = 500) {
    freeze()
    resizeTo(400)
    assertThat(useCompact).isFalse()

    unfreeze()
    assertThat(useCompact).isTrue()
  }

  @Test
  fun `a drag on a short panel holds the compact actions`() = panel(height = 400) {
    assertThat(useCompact).isTrue()

    freeze()
    resizeTo(500)
    assertThat(useCompact).isTrue()

    unfreeze()
    assertThat(useCompact).isFalse()
  }

  private fun panel(height: Int, block: PanelScope.() -> Unit) {
    val scope = PanelScope(height)
    ImageComposeScene(
      width = 400,
      height = 1000,
      density = Density(1f),
      content = {
        val fit = remember { PlaybackActionsFit() }
        val compact = fit.rememberUseCompact(threshold = 0.95f, frozen = scope.frozen)
        scope.onUseCompact(compact)
        Column(
          Modifier
            .width(coverWidth.dp)
            .height(scope.height.dp),
        ) {
          Box(
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .then(fit.coverSlotModifier),
          ) {
            Box(Modifier.aspectRatio(1f))
          }
          Box(
            fit.actionsModifier(compact)
              .fillMaxWidth()
              .height(if (compact) compactActionsHeight.dp else stackedActionsHeight.dp),
          )
        }
      },
    ).use { scene ->
      scope.scene = scene
      scope.settle()
      scope.block()
    }
  }

  private class PanelScope(height: Int) {
    lateinit var scene: ImageComposeScene

    var height by mutableStateOf(height)
      private set
    var frozen by mutableStateOf(false)
      private set

    private var useCompactState by mutableStateOf(false)

    val useCompact: Boolean get() = useCompactState

    fun onUseCompact(value: Boolean) {
      useCompactState = value
    }

    fun resizeTo(value: Int) {
      height = value
      settle()
    }

    fun freeze() {
      frozen = true
      settle()
    }

    fun unfreeze() {
      frozen = false
      settle()
    }

    /** The choice is made from measurements, so it lands a frame after whatever changed. */
    fun settle() = repeat(3) { scene.render(it * 16_000_000L) }
  }
}
