// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.roles

/**
 * A page the builder's controls pane shows in place of the list of controls, so the preview
 * stays in sight while it's open. Pages stack: Back closes the top one.
 */
internal sealed interface ControlsPage {

  /** Animates between pages with different keys; a page that only changes in place doesn't. */
  val contentKey: String

  /** Picking one of the palette colors. */
  data class Pick(val key: PaletteKey) : ControlsPage {
    override val contentKey: String get() = "pick"
  }

  /**
   * Fine-tuning [role], one of [check]'s two roles, or a role on its own when [check] is null.
   */
  data class Tune(val check: ContrastCheck?, val role: ThemeRole) : ControlsPage {
    val roles: List<ThemeRole> get() = check?.roles ?: listOf(role)
    override val contentKey: String get() = "tune:${check?.name ?: role.name}"
  }

  /** Every role in the theme, for tuning one on its own. */
  data object AllColors : ControlsPage {
    override val contentKey: String get() = "all"
  }

  companion object {
    /** Saves a stack of pages as plain strings. */
    val StackSaver: Saver<List<ControlsPage>, Any> = listSaver(
      save = { pages -> pages.map { it.encode() } },
      restore = { saved -> saved.mapNotNull { decode(it) } },
    )

    private fun ControlsPage.encode(): String = when (this) {
      is Pick -> "pick/${key.name}"
      is Tune -> "tune/${check?.name.orEmpty()}/${role.name}"
      AllColors -> "all"
    }

    private fun decode(saved: String): ControlsPage? {
      val parts = saved.split("/")
      return when (parts.first()) {
        "pick" -> PaletteKey.entries.firstOrNull { it.name == parts.getOrNull(1) }?.let(::Pick)
        "tune" -> {
          val role = ThemeRole.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: return null
          Tune(check = ContrastCheck.entries.firstOrNull { it.name == parts.getOrNull(1) }, role = role)
        }
        "all" -> AllColors
        else -> null
      }
    }
  }
}
