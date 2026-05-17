package app.campfire.podcasts.ui

/**
 * Shared-element key for the "Add podcast" navigation transitions. The cover artwork animates
 * from a tapped search result (or the URL-paste preview card) into the builder's cover preview.
 *
 * [id] is the podcast's feed URL — the only identifier that's present and identical on both
 * sides of the transition for both flows (iTunes hit → builder, URL paste → builder).
 */
data class AddPodcastSharedTransitionKey(
  val id: String,
  val type: ElementType,
) {
  enum class ElementType {
    /** The full card / screen bounds — animates a search row outward into the builder Scaffold. */
    Bounds,

    /** The cover artwork — animates inside the bounds, settling into the builder's cover slot. */
    Cover,
  }
}
