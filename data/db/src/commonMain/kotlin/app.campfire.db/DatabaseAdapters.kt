package app.campfire.db

import app.campfire.data.LibraryItem
import app.campfire.data.Media

interface DatabaseAdapters {

  val libraryItemAdapter: LibraryItem.Adapter
  val mediaAdapter: Media.Adapter
}
