package app.campfire.common.compose.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.rounded.Book
import app.campfire.common.compose.icons.rounded.BookShelf
import app.campfire.common.compose.icons.rounded.Database
import app.campfire.common.compose.icons.rounded.Newstand
import app.campfire.common.compose.icons.rounded.Shelves
import app.campfire.core.model.Library
import app.campfire.core.model.Library.Icon.AudioBookShelf
import app.campfire.core.model.Library.Icon.Book1
import app.campfire.core.model.Library.Icon.Books1
import app.campfire.core.model.Library.Icon.Books2
import app.campfire.core.model.Library.Icon.Database
import app.campfire.core.model.Library.Icon.FilePicture
import app.campfire.core.model.Library.Icon.Headphones
import app.campfire.core.model.Library.Icon.Heart
import app.campfire.core.model.Library.Icon.Microphone1
import app.campfire.core.model.Library.Icon.Microphone3
import app.campfire.core.model.Library.Icon.Music
import app.campfire.core.model.Library.Icon.None
import app.campfire.core.model.Library.Icon.Podcast
import app.campfire.core.model.Library.Icon.Power
import app.campfire.core.model.Library.Icon.Radio
import app.campfire.core.model.Library.Icon.Rocket
import app.campfire.core.model.Library.Icon.Rss
import app.campfire.core.model.Library.Icon.Star

fun Library.Icon.asComposeIcon(): ImageVector = when (this) {
  Database -> CampfireIcons.Rounded.Database
  AudioBookShelf -> CampfireIcons.Rounded.BookShelf
  Books1 -> CampfireIcons.Rounded.Newstand
  Books2 -> CampfireIcons.Rounded.Shelves
  Book1 -> CampfireIcons.Rounded.Book
  Microphone1 -> Icons.Rounded.Mic
  Microphone3 -> Icons.Rounded.HeadsetMic
  Radio -> Icons.Rounded.Radio
  Podcast -> Icons.Rounded.Podcasts
  Rss -> Icons.Rounded.RssFeed
  Headphones -> Icons.Rounded.Headphones
  Music -> Icons.Rounded.MusicNote
  FilePicture -> Icons.Rounded.PhotoAlbum
  Rocket -> Icons.Rounded.Rocket
  Power -> Icons.Rounded.Power
  Star -> Icons.Rounded.Star
  Heart -> Icons.Rounded.Favorite
  None -> Icons.Filled.Library
}
