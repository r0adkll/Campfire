// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.test

import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps custom themes in [themes] and the current theme in [currentTheme]. Saves and deletes are
 * recorded in [saved] and [deleted], and show in [observeCustomThemes].
 */
class FakeAppThemeRepository : AppThemeRepository {
  val themes = mutableMapOf<String, AppTheme.Fixed>()
  val saved = mutableListOf<AppTheme.Fixed>()
  val deleted = mutableListOf<String>()
  val currentTheme = MutableStateFlow<AppTheme>(AppTheme.Fixed.Tent)
  private val customThemes = MutableStateFlow<List<AppTheme.Fixed>>(emptyList())

  override fun observeCurrentAppTheme(): StateFlow<AppTheme> = currentTheme

  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> {
    customThemes.value = themes.values.toList()
    return customThemes
  }

  override fun setCurrentTheme(theme: AppTheme) {
    currentTheme.value = theme
  }

  override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed> =
    themes[id]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(id))

  override suspend fun saveCustomTheme(theme: AppTheme.Fixed) {
    saved += theme
    themes[theme.id] = theme
    customThemes.value = themes.values.toList()
  }

  override suspend fun deleteCustomTheme(id: String) {
    deleted += id
    themes.remove(id)
    customThemes.value = themes.values.toList()
  }
}
