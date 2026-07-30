// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.shared

/**
 * Shared transition key to use between the [app.campfire.auth.ui.welcome.Welcome] and
 * [app.campfire.auth.ui.login.Login] screens.
 */
data class AuthSharedTransitionKey(
  val type: ElementType,
) {
  enum class ElementType {
    Logo,
    Title,
    Card,
    Tent,
  }
}
