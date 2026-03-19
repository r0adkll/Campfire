package app.campfire.core

import android.content.Intent

interface ActivityIntentProvider {

  fun provide(): Intent
}
