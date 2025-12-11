package app.campfire.ui.theming.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppThemeRepository {

  fun observeCurrentAppTheme(): StateFlow<AppTheme>

  fun observeCustomThemes(): Flow<List<AppTheme.Fixed.Custom>>

  fun setCurrentTheme(theme: AppTheme)

  suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed.Custom>

  suspend fun saveCustomTheme(theme: AppTheme.Fixed.Custom)

  suspend fun deleteCustomTheme(id: String)
}
