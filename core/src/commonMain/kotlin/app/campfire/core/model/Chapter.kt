package app.campfire.core.model

import app.campfire.core.extensions.seconds
import kotlin.time.Duration

data class Chapter(
  val id: Int,
  val start: Float,
  val end: Float,
  val title: String,
) {

  val duration: Duration = (end - start).seconds
}
