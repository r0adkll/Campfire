// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.engine.ffmpeg.FfmpegPlaybackEngine
import app.campfire.audioplayer.engine.vlc.VlcPlaybackEngine
import app.campfire.audioplayer.impl.DesktopAudioPlayer
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.FileMetadata
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * End-to-end check of the desktop player against a real Audiobookshelf server: logs in with the
 * developer credentials from `~/.gradle/gradle.properties` (`campfire_server_url`,
 * `campfire_username`, `campfire_password`), picks a chaptered book, and streams it through the
 * real libvlc engine — resume inside a chapter, chapter seek, pause/resume, skip forward.
 *
 * Self-skips when the credentials or VLC are absent so CI is unaffected. Audio is muted.
 */
class DesktopAudioPlayerServerIntegrationTest {

  private val json = Json { ignoreUnknownKeys = true }
  private val http = HttpClient.newHttpClient()

  @Test
  fun `libvlc streams a chaptered book from the server with chapter navigation`() {
    try {
      VlcPlaybackEngine.Factory(SILENT_LIBVLC_ARGS).create().release()
    } catch (e: PlaybackEngineUnavailableException) {
      println("VLC not available, skipping: ${e.message}")
      return
    }
    streamFromServer(VlcPlaybackEngine.Factory(SILENT_LIBVLC_ARGS))
  }

  @Test
  fun `ffmpeg streams a chaptered book from the server with chapter navigation`() {
    streamFromServer { FfmpegPlaybackEngine().apply { volume = 0f } }
  }

  private fun streamFromServer(engineFactory: PlaybackEngine.Factory) {
    val credentials = credentials() ?: run {
      println("No campfire_* credentials in ~/.gradle/gradle.properties, skipping")
      return
    }

    Heartwood.grow(
      object : Heartwood.Bark {
        override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String) {
          println("[${priority.name}] ${tag ?: ""} ${message()}")
        }
      },
    )

    val token = login(credentials)
    val book = findChapteredBook(credentials.serverUrl, token)
    println("Streaming '${book.title}' (${book.tracks.size} tracks, ${book.chapters.size} chapters)")

    val settings = FakePlaybackSettings()
    val player = DesktopAudioPlayer(
      settings = settings,
      equalizerSettings = FakeEqualizerSettings(),
      sleepTimerManagerFactory = FakeSleepTimerManager().factory,
      engineFactory = engineFactory,
      accessTokenProvider = { token },
    )

    try {
      runBlocking {
        // Resume 3s into the second chapter
        val chapter = book.chapters[1]
        val resume = chapter.start.seconds + 3.seconds
        player.prepare(session(book.chapters, book.tracks, currentTime = resume), playImmediately = true) { }

        awaitUntil("playing past the resume point") {
          player.state.value == State.Playing && player.overallTime.value > resume
        }
        assertThat(player.currentMetadata.value.title).isEqualTo(chapter.title)
        assertThat(player.currentTime.value).isGreaterThanOrEqualTo(3.seconds)
        assertThat(player.currentTime.value).isLessThan(3.seconds + STREAM_SLACK)

        // Jump to the third chapter by id, as the chapter list in the UI does
        val next = book.chapters[2]
        player.seekTo(next.id)
        awaitUntil("playing inside ${next.title}") {
          player.state.value == State.Playing &&
            player.currentMetadata.value.title == next.title &&
            player.overallTime.value >= next.start.seconds
        }
        assertThat(player.currentTime.value).isLessThan(STREAM_SLACK)

        player.pause()
        awaitUntil("paused") { player.state.value == State.Paused }
        val pausedAt = player.overallTime.value

        player.seekForward()
        player.playPause()
        awaitUntil("resumed after skipping forward") {
          player.state.value == State.Playing &&
            player.overallTime.value > pausedAt + settings.forwardTimeMs.milliseconds
        }

        assertThat(player.error.value).isNull()
      }
    } finally {
      player.stop()
    }
  }

  private suspend fun awaitUntil(what: String, condition: () -> Boolean) {
    try {
      withTimeout(TIMEOUT) {
        while (!condition()) delay(100)
      }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
      error("Timed out waiting for: $what")
    }
  }

  // region Server access

  private data class Credentials(val serverUrl: String, val username: String, val password: String)

  private data class Book(val title: String, val chapters: List<Chapter>, val tracks: List<AudioTrack>)

  private fun credentials(): Credentials? {
    val file = File(System.getProperty("user.home"), ".gradle/gradle.properties")
    if (!file.exists()) return null
    val props = Properties().apply { file.reader().use { load(it) } }
    val server = props.getProperty("campfire_server_url")?.trimEnd('/') ?: return null
    val username = props.getProperty("campfire_username") ?: return null
    val password = props.getProperty("campfire_password") ?: return null
    return Credentials(server, username, password)
  }

  private fun login(credentials: Credentials): String {
    val body = """{"username":"${credentials.username}","password":"${credentials.password}"}"""
    val request = HttpRequest.newBuilder(URI("${credentials.serverUrl}/login"))
      .header("Content-Type", "application/json")
      .header("User-Agent", USER_AGENT)
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) { "Login failed: HTTP ${response.statusCode()}" }
    val user = json.parseToJsonElement(response.body()).jsonObject["user"]!!.jsonObject
    return user["accessToken"]?.jsonPrimitive?.contentOrNull
      ?: user["token"]?.jsonPrimitive?.contentOrNull
      ?: error("Login response carried no token")
  }

  private fun get(serverUrl: String, token: String, path: String): JsonObject {
    val request = HttpRequest.newBuilder(URI("$serverUrl$path"))
      .header("Authorization", "Bearer $token")
      .header("User-Agent", USER_AGENT)
      .GET()
      .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) { "GET $path failed: HTTP ${response.statusCode()}" }
    return json.parseToJsonElement(response.body()).jsonObject
  }

  /** First book in the first book library with at least three chapters and one audio track. */
  private fun findChapteredBook(serverUrl: String, token: String): Book {
    val libraries = get(serverUrl, token, "/api/libraries")["libraries"]!!.jsonArray
    val library = libraries.map { it.jsonObject }
      .first { it["mediaType"]?.jsonPrimitive?.contentOrNull == "book" }
    val libraryId = library["id"]!!.jsonPrimitive.contentOrNull
    val items = get(serverUrl, token, "/api/libraries/$libraryId/items?limit=40&sort=media.metadata.title")
      .getValue("results").jsonArray
    for (item in items) {
      val id = item.jsonObject["id"]!!.jsonPrimitive.contentOrNull
      val media = get(serverUrl, token, "/api/items/$id?expanded=1")["media"]!!.jsonObject
      val chapters = media["chapters"]?.jsonArray ?: continue
      val tracks = media["tracks"]?.jsonArray ?: continue
      if (chapters.size < 3 || tracks.isEmpty()) continue
      return Book(
        title = media["metadata"]!!.jsonObject["title"]!!.jsonPrimitive.contentOrNull ?: "?",
        chapters = chapters.map { c ->
          val o = c.jsonObject
          Chapter(
            id = o["id"]!!.jsonPrimitive.int,
            start = o["start"]!!.jsonPrimitive.double.toFloat(),
            end = o["end"]!!.jsonPrimitive.double.toFloat(),
            title = o["title"]!!.jsonPrimitive.contentOrNull ?: "",
          )
        },
        tracks = tracks.map { t ->
          val o = t.jsonObject
          val contentUrl = o["contentUrl"]!!.jsonPrimitive.contentOrNull!!
          AudioTrack(
            index = o["index"]!!.jsonPrimitive.int,
            startOffset = o["startOffset"]!!.jsonPrimitive.double.toFloat(),
            duration = o["duration"]!!.jsonPrimitive.double.toFloat(),
            title = o["title"]?.jsonPrimitive?.contentOrNull ?: "",
            contentUrl = serverUrl + contentUrl,
            mimeType = o["mimeType"]?.jsonPrimitive?.contentOrNull ?: "audio/mp4",
            codec = o["codec"]?.jsonPrimitive?.contentOrNull ?: "",
            metadata = FileMetadata(contentUrl, "", contentUrl, contentUrl, 0, 0, 0, 0),
            metaTags = null,
          )
        },
      )
    }
    error("No chaptered book found in library ${library["name"]?.jsonPrimitive?.contentOrNull}")
  }

  // endregion

  companion object {
    private const val USER_AGENT = "Campfire-Desktop-IntegrationTest/1.0"

    /** Decode and clock the stream normally, but discard the samples instead of playing them. */
    private val SILENT_LIBVLC_ARGS = listOf("--aout=dummy")
    private val TIMEOUT = 45.seconds

    /** How far past the requested position the first observed tick may land on a network stream. */
    private val STREAM_SLACK = 20.seconds
  }
}
