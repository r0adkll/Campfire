package app.campfire.audioplayer.impl

import android.content.Intent

interface SessionActivityIntentProvider {

  fun provide(): Intent
}
