// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.settings.api.PerBookSettings
import app.campfire.settings.api.SignedInSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.UserThemeSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

/** Reaches the current user graph through [ComponentHolder], which holds the graph of whoever is signed in. */
@ContributesBinding(AppScope::class)
@Inject
class ComponentHolderSignedInSettings : SignedInSettings {

  private val component: UserSettingsComponent
    get() = ComponentHolder.component()

  override fun perBook(): PerBookSettings = component.perBookSettings

  override fun theme(): UserThemeSettings = component.userThemeSettings

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeThemeId(): Flow<ThemeKey> = ComponentHolder.subscribe<UserSettingsComponent>()
    .distinctUntilChanged { old, new -> old === new }
    .flatMapLatest { it.userThemeSettings.observeThemeId() }
}
