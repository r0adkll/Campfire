package app.campfire.common.compose.widgets

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

actual val isNoisyCampfireLoadingIndicatorEnabled: Boolean
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
  get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
