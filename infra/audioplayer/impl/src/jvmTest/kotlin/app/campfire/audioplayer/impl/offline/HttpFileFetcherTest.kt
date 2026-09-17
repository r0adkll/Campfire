// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AbsToken
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.test.runTest

class HttpFileFetcherTest {

  private val body = "0123456789".encodeToByteArray()
  private val dir: File = createTempDirectory("http-fetcher").toFile()
  private val part = File(dir, "1.mp3.part")

  private val requests = CopyOnWriteArrayList<Map<String, String?>>()
  private var handler: (HttpExchange) -> Unit = ::serveWithRanges

  private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
    createContext("/api/items/li_1/file/1/download") { exchange ->
      requests += listOf("Range", "Authorization", "X-Proxy").associateWith {
        exchange.requestHeaders.getFirst(it)
      }
      exchange.use { handler(it) }
    }
    start()
  }

  private val accountManager = FakeAccountManager()
  private val refresher = FakeTokenRefresher(accountManager)

  private val request = FetchRequest(
    url = "http://127.0.0.1:${server.address.port}/api/items/li_1/file/1/download",
    userId = "user-1",
    serverUrl = "http://127.0.0.1:${server.address.port}",
    expectedBytes = body.size.toLong(),
  )

  // The desktop engine, against a real socket, so ranges, timeouts, and streaming behave as shipped
  private val client = HttpClient(OkHttp) {
    install(HttpTimeout) { socketTimeoutMillis = 5_000 }
  }
  private val fetcher = HttpFileFetcher(client, accountManager, refresher)

  @AfterTest
  fun tearDown() {
    client.close()
    server.stop(0)
    dir.deleteRecursively()
  }

  private fun serveWithRanges(exchange: HttpExchange) {
    val range = exchange.requestHeaders.getFirst("Range")
    if (range == null) {
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.write(body)
      return
    }
    val start = range.removePrefix("bytes=").removeSuffix("-").toInt()
    if (start >= body.size) {
      exchange.responseHeaders.add("Content-Range", "bytes */${body.size}")
      exchange.sendResponseHeaders(416, -1)
      return
    }
    exchange.responseHeaders.add("Content-Range", "bytes $start-${body.size - 1}/${body.size}")
    exchange.sendResponseHeaders(206, (body.size - start).toLong())
    exchange.responseBody.write(body, start, body.size - start)
  }

  @Test
  fun `downloads a file with the account's credentials`() = runTest {
    accountManager.token = AbsToken("access", "refresh")
    accountManager.extraHeaders = mapOf("X-Proxy" to "secret")
    val progress = mutableListOf<Pair<Long, Long>>()

    fetcher.fetch(request, part) { bytes, total -> progress += bytes to total }

    assertThat(part.readText()).isEqualTo("0123456789")
    assertThat(progress.last()).isEqualTo(10L to 10L)
    assertThat(requests.single()).isEqualTo(
      mapOf(
        "Range" to null,
        "Authorization" to "Bearer access",
        "X-Proxy" to "secret",
      ),
    )
  }

  @Test
  fun `resumes a partial file with a range request`() = runTest {
    part.writeText("0123")

    fetcher.fetch(request, part) { _, _ -> }

    assertThat(part.readText()).isEqualTo("0123456789")
    assertThat(requests.single()["Range"]).isEqualTo("bytes=4-")
  }

  @Test
  fun `starts over when the server ignores the range`() = runTest {
    part.writeText("stale")
    handler = { exchange ->
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.write(body)
    }

    fetcher.fetch(request, part) { _, _ -> }

    assertThat(part.readText()).isEqualTo("0123456789")
  }

  @Test
  fun `a partial file already holding every byte is complete`() = runTest {
    part.writeText("0123456789")

    fetcher.fetch(request, part) { _, _ -> }

    assertThat(part.readText()).isEqualTo("0123456789")
  }

  @Test
  fun `an expired token is refreshed once and the request retried`() = runTest {
    accountManager.token = AbsToken("stale", "refresh")
    refresher.next = AbsToken("fresh", "refresh-2")
    handler = { exchange ->
      if (exchange.requestHeaders.getFirst("Authorization") == "Bearer fresh") {
        serveWithRanges(exchange)
      } else {
        exchange.sendResponseHeaders(401, -1)
      }
    }

    fetcher.fetch(request, part) { _, _ -> }

    assertThat(part.readText()).isEqualTo("0123456789")
    assertThat(refresher.refreshedWith).containsExactly("stale")
    assertThat(requests.map { it["Authorization"] }).containsExactly("Bearer stale", "Bearer fresh")
  }

  @Test
  fun `an unrefreshable token fails permanently`() = runTest {
    accountManager.token = AbsToken("stale", "refresh")
    handler = { exchange -> exchange.sendResponseHeaders(401, -1) }

    assertFailure { fetcher.fetch(request, part) { _, _ -> } }
      .isInstanceOf(PermanentFetchException::class)
  }

  @Test
  fun `a token still rejected after refreshing fails permanently`() = runTest {
    accountManager.token = AbsToken("stale", "refresh")
    refresher.next = AbsToken("fresh", "refresh-2")
    handler = { exchange -> exchange.sendResponseHeaders(401, -1) }

    assertFailure { fetcher.fetch(request, part) { _, _ -> } }
      .isInstanceOf(PermanentFetchException::class)
    assertThat(requests.map { it["Authorization"] }).containsExactly("Bearer stale", "Bearer fresh")
  }

  @Test
  fun `a user without download permission fails permanently`() = runTest {
    handler = { exchange -> exchange.sendResponseHeaders(403, -1) }

    assertFailure { fetcher.fetch(request, part) { _, _ -> } }
      .isInstanceOf(PermanentFetchException::class)
    assertThat(requests.single()["Authorization"]).isNull()
  }

  @Test
  fun `a server error can be retried`() = runTest {
    handler = { exchange -> exchange.sendResponseHeaders(503, -1) }

    val failure = assertFailure { fetcher.fetch(request, part) { _, _ -> } }
    failure.isInstanceOf(IOException::class)
    failure.transform { it is PermanentFetchException }.isEqualTo(false)
  }

  @Test
  fun `a stalled transfer is abandoned`() = runTest {
    handler = { exchange ->
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.write(body, 0, 2)
      exchange.responseBody.flush()
      Thread.sleep(10_000)
    }
    val stallingClient = HttpClient(OkHttp) {
      install(HttpTimeout) { socketTimeoutMillis = 300 }
    }
    val stalling = HttpFileFetcher(stallingClient, accountManager, refresher)
    val started = TimeSource.Monotonic.markNow()

    // A socket timeout is an IOException, so the queue retries it rather than failing for good
    assertFailure { stalling.fetch(request, part) { _, _ -> } }
      .transform { it is IOException && it !is PermanentFetchException }
      .isTrue()
    // Abandoned by the socket timeout, long before the server would have given up
    assertThat(started.elapsedNow()).isLessThan(5.seconds)
    stallingClient.close()
  }
}
