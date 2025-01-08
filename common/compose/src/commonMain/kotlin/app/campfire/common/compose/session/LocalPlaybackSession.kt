package app.campfire.common.compose.session

import androidx.compose.runtime.compositionLocalOf
import app.campfire.core.model.Session

val LocalPlaybackSession = compositionLocalOf<Session?> { null }
