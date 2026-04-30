package app.campfire.ui.theming.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppThemeRepository {

  fun observeCurrentAppTheme(): StateFlow<AppTheme>

  fun observeCustomThemes(): Flow<List<AppTheme.Fixed>>

  fun setCurrentTheme(theme: AppTheme)

  suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed>

  suspend fun saveCustomTheme(theme: AppTheme.Fixed)

  suspend fun deleteCustomTheme(id: String)
}
