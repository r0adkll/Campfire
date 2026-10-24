// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui.web

import androidx.compose.runtime.Stable
import app.campfire.core.logging.bark
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The two-way line to the reader page (`files/reader/reader.js`). Calls into the page are queued
 * as scripts for the platform web view to evaluate; the page's JSON messages come back through
 * [onMessage], from any thread.
 */
@Stable
class ReaderBridge {
  private val scriptChannel = Channel<String>(Channel.UNLIMITED)
  private val messageChannel = Channel<ReaderMessage>(Channel.UNLIMITED)

  /** Scripts for the web view to evaluate, in order. Consumed by the platform renderer. */
  val scripts: ReceiveChannel<String> get() = scriptChannel

  /** Messages from the page, in order. Consumed by the presenter. */
  val messages: ReceiveChannel<ReaderMessage> get() = messageChannel

  fun onMessage(json: String) {
    runCatching { BridgeJson.decodeFromString<ReaderMessage>(json) }
      .onSuccess { messageChannel.trySend(it) }
      .onFailure { bark(throwable = it) { "Unreadable reader message: $json" } }
  }

  fun open(url: String, format: String, location: String?, style: ReaderStyle) {
    call("open", BridgeJson.encodeToString(OpenArgs(url, format, location, style)))
  }

  fun next() = call("next")

  fun prev() = call("prev")

  fun goTo(href: String) = call("goTo", BridgeJson.encodeToString(href))

  fun goToFraction(fraction: Float) = call("goToFraction", fraction.toString())

  /** Show a position in the format [ReaderMessage.Relocate.location] reports. */
  fun goToLocation(location: String) = call("goToLocation", BridgeJson.encodeToString(location))

  fun setStyle(style: ReaderStyle) = call("setStyle", BridgeJson.encodeToString(style))

  private fun call(function: String, argument: String = "") {
    scriptChannel.trySend("globalThis.campfire.$function($argument)")
  }

  @Serializable
  private data class OpenArgs(
    val url: String,
    val format: String,
    val location: String?,
    val style: ReaderStyle,
  )
}

/**
 * How the page draws the book. Colors are CSS colors.
 *
 * @param fontFamily a CSS font family list for the text, or null to keep the book's
 * @param gap the space between and beside the columns, as a percentage of the page
 * @param columns how many pages side by side at most, where the window is wide enough
 * @param portraitColumns the same, in a portrait window
 */
@Serializable
data class ReaderStyle(
  val background: String,
  val foreground: String,
  val link: String,
  val fontFamily: String? = null,
  val fontScale: Int = 100,
  val lineHeight: Float = 1.5f,
  val justify: Boolean = true,
  val hyphenate: Boolean = true,
  val gap: Int = 7,
  val scrolled: Boolean = false,
  val columns: Int = 2,
  val portraitColumns: Int = 1,
)

@Serializable
sealed interface ReaderMessage {

  /** The page has loaded and can open a book. */
  @Serializable
  @SerialName("ready")
  data object Ready : ReaderMessage

  @Serializable
  @SerialName("opened")
  data class Opened(
    val title: String? = null,
    val toc: List<TocEntry> = emptyList(),
  ) : ReaderMessage

  /** The saved position was (or, when [restored] is false, couldn't be) shown. */
  @Serializable
  @SerialName("restored")
  data class Restored(val restored: Boolean) : ReaderMessage

  /**
   * The visible position changed.
   *
   * @param location in the Audiobookshelf `ebookLocation` format for this book's type
   * @param progress how far through the book, 0..1
   * @param part the title of the contents entry [chapter] is nested in
   * @param pagesLeft pages after this one in the chapter, where the book reflows into pages
   * @param atEnd whether this is the book's last page
   * @param save whether the reader moved here (a page turn, a jump), rather than laying out the
   * position it opened at again
   */
  @Serializable
  @SerialName("relocate")
  data class Relocate(
    val location: String? = null,
    val progress: Float? = null,
    val part: String? = null,
    val chapter: String? = null,
    val chapterHref: String? = null,
    val pagesLeft: Int? = null,
    val atEnd: Boolean = false,
    val save: Boolean = false,
  ) : ReaderMessage

  /** A tap in the middle of the page, which turns no page. */
  @Serializable
  @SerialName("tap")
  data object Tap : ReaderMessage

  /** Something failed in the page; [fatal] when the book couldn't be opened at all. */
  @Serializable
  @SerialName("error")
  data class Error(val message: String, val fatal: Boolean = false) : ReaderMessage
}

/** An item of the table of contents; [fraction] is where it starts in the book, 0..1. */
@Serializable
data class TocEntry(
  val label: String,
  val href: String,
  val depth: Int,
  val fraction: Float? = null,
)

private val BridgeJson = Json {
  ignoreUnknownKeys = true
  encodeDefaults = true
}
