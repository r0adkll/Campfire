// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.rounded.Book
import app.campfire.common.compose.icons.rounded.BookShelf
import app.campfire.common.compose.icons.rounded.Database
import app.campfire.common.compose.icons.rounded.Favorite
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.HeadsetMic
import app.campfire.common.compose.icons.rounded.Mic
import app.campfire.common.compose.icons.rounded.MusicNote
import app.campfire.common.compose.icons.rounded.Newstand
import app.campfire.common.compose.icons.rounded.PhotoAlbum
import app.campfire.common.compose.icons.rounded.Podcasts
import app.campfire.common.compose.icons.rounded.Power
import app.campfire.common.compose.icons.rounded.Radio
import app.campfire.common.compose.icons.rounded.Rocket
import app.campfire.common.compose.icons.rounded.RssFeed
import app.campfire.common.compose.icons.rounded.Shelves
import app.campfire.common.compose.icons.rounded.Star
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
  Microphone1 -> CampfireIcons.Rounded.Mic
  Microphone3 -> CampfireIcons.Rounded.HeadsetMic
  Radio -> CampfireIcons.Rounded.Radio
  Podcast -> CampfireIcons.Rounded.Podcasts
  Rss -> CampfireIcons.Rounded.RssFeed
  Headphones -> CampfireIcons.Rounded.Headphones
  Music -> CampfireIcons.Rounded.MusicNote
  FilePicture -> CampfireIcons.Rounded.PhotoAlbum
  Rocket -> CampfireIcons.Rounded.Rocket
  Power -> CampfireIcons.Rounded.Power
  Star -> CampfireIcons.Rounded.Star
  Heart -> CampfireIcons.Rounded.Favorite
  None -> CampfireIcons.Filled.Library
}
