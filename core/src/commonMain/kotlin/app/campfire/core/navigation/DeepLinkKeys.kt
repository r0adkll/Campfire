package app.campfire.core.navigation

import app.campfire.core.model.LibraryItemId

@Suppress("ConstPropertyName")
object DeepLinkKeys {

  const val LibraryItemId = "library_item_id"
}

sealed interface DeepLink {
  data object None : DeepLink

  data class ItemDetail(
    val libraryItemId: LibraryItemId,
  ) : DeepLink
}
