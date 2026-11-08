// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.User
import app.campfire.libraries.ui.detail.manage.itemActions
import app.campfire.offline.Download

/**
 * Something an account can do with an item for its own listening, rather than to it on the
 * server, offered first among what its card's long press or right click offers.
 */
sealed interface QuickAction {
  /** Listen to it, picking up where it was left when [resume] */
  data class Play(val resume: Boolean) : QuickAction

  /** Read it, an item with nothing to listen to, picking up where it was left when [resume] */
  data class Read(val resume: Boolean) : QuickAction

  data object Download : QuickAction

  /** Stop a download that's still to finish */
  data object StopDownload : QuickAction

  /** Remove the download from this device, or what of it there is */
  data object RemoveDownload : QuickAction

  data object MarkFinished : QuickAction
  data object MarkNotFinished : QuickAction
  data object AddToQueue : QuickAction
  data object RemoveFromQueue : QuickAction
  data object AddToPlaylist : QuickAction
  data object AddToCollection : QuickAction
}

/**
 * The [QuickAction]s [user] can take with [item], in the order they're offered, given its
 * [progress], its [download] if it has one, whether it [isQueued], and whether it [isPlaying].
 * Each follows the item's state, as Play becomes Resume once started, and one that can't be taken
 * isn't offered: a podcast is listened to, downloaded and finished by the episode, so only its
 * collections are.
 */
fun quickActions(
  item: LibraryItem,
  user: User,
  progress: MediaProgress?,
  download: Download?,
  isQueued: Boolean,
  isPlaying: Boolean,
): List<QuickAction> = buildList {
  if (item.media is Media.Book) {
    val isFinished = progress?.isFinished == true
    when {
      item.isEbookOnly -> add(QuickAction.Read(resume = (progress?.actualProgress ?: 0f) > 0f && !isFinished))
      // What's playing already has the player
      !isPlaying -> add(QuickAction.Play(resume = (progress?.progress ?: 0f) > 0f && !isFinished))
    }

    when {
      download?.isAvailable == true -> add(QuickAction.RemoveDownload)
      // An ebook's single file can't be stopped part way
      download?.isInProgress == true -> if (!item.isEbookOnly) add(QuickAction.StopDownload)
      user.canDownload -> add(QuickAction.Download)
      // What was stopped or failed can still be taken off the device without the permission
      download != null -> add(QuickAction.RemoveDownload)
    }

    add(if (isFinished) QuickAction.MarkNotFinished else QuickAction.MarkFinished)

    // An ebook can't be queued to play, nor what's already playing
    if (!item.isEbookOnly && !isPlaying) {
      add(if (isQueued) QuickAction.RemoveFromQueue else QuickAction.AddToQueue)
    }

    add(QuickAction.AddToPlaylist)
  }

  if (user.canEditCollections) add(QuickAction.AddToCollection)
}

/**
 * Whether [user] can do anything with [item] from its card: any book has something to listen to or
 * read, and a podcast has its collections or what the server would let [user] do to it.
 */
fun User.canActOn(item: LibraryItem): Boolean =
  item.media is Media.Book || canEditCollections || itemActions(item).isNotEmpty()
