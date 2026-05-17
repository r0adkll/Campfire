package app.campfire.podcasts.api

/**
 * Typed failure surfaced by [PodcastsRepository.addPodcast]. The repository translates the
 * server's HTTP status codes into a [Kind] so callers don't need to depend on the network
 * exception type.
 */
class AddPodcastException(val kind: Kind) : Exception("Add podcast failed: $kind") {
  enum class Kind {
    /** The current user is not Admin/Root — server returned 403. */
    Forbidden,

    /** Path already exists or is invalid — server returned 400. */
    PathConflict,

    /** Anything else (network failure, 5xx, deserialization error). */
    Generic,
  }
}
