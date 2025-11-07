package app.campfire.core.extensions

import kotlinx.datetime.LocalDate

val LocalDate.readableFormat: String
  get() = "${month.name.capitalized()} $day, $year"
