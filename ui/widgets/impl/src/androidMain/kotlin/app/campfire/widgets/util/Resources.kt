package app.campfire.widgets.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext

@Composable
internal fun glanceStringResource(
  @StringRes resId: Int,
): String {
  return LocalContext.current.getString(resId)
}
