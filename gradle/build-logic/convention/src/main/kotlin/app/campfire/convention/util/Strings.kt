package app.campfire.convention.util

import java.util.Locale

fun String.capitalized(): CharSequence = let<CharSequence, CharSequence> {
  if (it.isEmpty()) it else it[0].titlecase(
    Locale.getDefault()
  ) + it.substring(1)
}
