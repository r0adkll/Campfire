// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.management.api.match.ImageSize
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class HttpCoverSizeReaderTest {

  private val requested = mutableListOf<String>()
  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun `a cover's size comes from the start of its file`() = runTest {
    val reader = reader { respond(Png500 + ByteArray(64 * 1024)) }

    val size = reader.sizeOf("https://m.media-amazon.com/images/I/cover.jpg")

    assertThat(size.getOrNull()).isEqualTo(ImageSize(500, 500))
    assertThat(requested).containsExactly("https://m.media-amazon.com/images/I/cover.jpg")
  }

  @Test
  fun `a server's cover is read as the server keeps it, not resized`() = runTest {
    val reader = reader { respond(Png500) }

    reader.sizeOf("https://abs.example.com/api/items/li_1/cover?ts=42")

    assertThat(requested).containsExactly("https://abs.example.com/api/items/li_1/cover?ts=42&raw=1")
  }

  @Test
  fun `a cover that won't load, or isn't an image, has no size`() = runTest {
    val missing = reader { respondError(HttpStatusCode.NotFound) }.sizeOf("https://cdn.example.com/a.jpg")
    val notImage = reader { respond("<html></html>") }.sizeOf("https://cdn.example.com/b.jpg")

    assertThat(missing.isFailure).isTrue()
    assertThat(notImage.isFailure).isTrue()
  }

  private fun reader(respond: suspend MockRequestHandleScope.() -> HttpResponseData) =
    HttpCoverSizeReader(
      client = HttpClient(
        MockEngine { request ->
          requested += request.url.toString()
          respond()
        },
      ),
      dispatchers = dispatchers,
    )

  private companion object {
    val Png500: ByteArray =
      byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x01, 0xF4.toByte(),
        0x00, 0x00, 0x01, 0xF4.toByte(),
        0x08, 0x02, 0x00, 0x00, 0x00,
      )
  }
}
