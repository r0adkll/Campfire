// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.image

import app.campfire.core.model.LibraryItemId
import app.campfire.covers.api.OfflineCover
import app.campfire.covers.api.OfflineCovers
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isInstanceOf
import coil3.Image
import coil3.PlatformContext
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Size
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

class OfflineCoverInterceptorTest {

  private val kept = mutableMapOf<LibraryItemId, OfflineCover>()
  private val interceptor = OfflineCoverInterceptor(
    object : OfflineCovers {
      override suspend fun get(itemId: LibraryItemId) = kept[itemId]
    },
  )

  @Test
  fun keptCover_standsInForTheSameVersion_withoutAskingTheServer() = runTest {
    kept["li_1"] = OfflineCover(PATH, timestamp = 5)
    val chain = FakeChain(request("$SERVER/api/items/li_1/cover?ts=5"))

    interceptor.intercept(chain)

    assertThat(chain.proceeded).containsExactly(PATH.toPath())
  }

  @Test
  fun keptCover_standsInForAnUnversionedUrl() = runTest {
    kept["li_1"] = OfflineCover(PATH, timestamp = 5)
    val chain = FakeChain(request("$SERVER/api/items/li_1/cover"))

    interceptor.intercept(chain)

    assertThat(chain.proceeded).containsExactly(PATH.toPath())
  }

  @Test
  fun outdatedKeptCover_letsTheServerAnswer() = runTest {
    kept["li_1"] = OfflineCover(PATH, timestamp = 5)
    val url = "$SERVER/api/items/li_1/cover?ts=6"
    val chain = FakeChain(request(url))

    val result = interceptor.intercept(chain)

    assertThat(result).isInstanceOf<SuccessResult>()
    assertThat(chain.proceeded).containsExactly(url)
  }

  @Test
  fun outdatedKeptCover_standsInWhenTheServerCantBeReached() = runTest {
    kept["li_1"] = OfflineCover(PATH, timestamp = 5)
    val url = "$SERVER/api/items/li_1/cover?ts=6"
    val chain = FakeChain(request(url), failing = { it is String })

    val result = interceptor.intercept(chain)

    assertThat(result).isInstanceOf<SuccessResult>()
    assertThat(chain.proceeded).containsExactly(url, PATH.toPath())
  }

  @Test
  fun itemsWithoutAKeptCover_andOtherImages_goToTheServer() = runTest {
    kept["li_1"] = OfflineCover(PATH, timestamp = 5)
    val other = "$SERVER/api/items/li_2/cover?ts=5"
    val author = "$SERVER/api/authors/li_1/image"
    val chain = FakeChain(request(other))
    val authorChain = FakeChain(request(author))

    interceptor.intercept(chain)
    interceptor.intercept(authorChain)

    assertThat(chain.proceeded).containsExactly(other)
    assertThat(authorChain.proceeded).containsExactly(author)
  }

  private fun request(data: Any) = ImageRequest.Builder(PlatformContext.INSTANCE).data(data).build()

  /** Records the data of every request proceeded with; [failing] ones end in an error. */
  private class FakeChain(
    override val request: ImageRequest,
    private val failing: (Any) -> Boolean = { false },
    private val root: FakeChain? = null,
  ) : Interceptor.Chain {
    val proceeded = mutableListOf<Any>()

    override val size: Size = Size.ORIGINAL

    override fun withRequest(request: ImageRequest): Interceptor.Chain = FakeChain(request, failing, root ?: this)

    override fun withSize(size: Size): Interceptor.Chain = this

    override suspend fun proceed(): ImageResult {
      (root ?: this).proceeded += request.data
      return if (failing(request.data)) {
        ErrorResult(image = null, request = request, throwable = IllegalStateException("offline"))
      } else {
        SuccessResult(image = NoopImage, request = request)
      }
    }
  }

  private object NoopImage : Image {
    override val size: Long = 0
    override val width: Int = 1
    override val height: Int = 1
    override val shareable: Boolean = true
    override fun draw(canvas: coil3.Canvas) = Unit
  }

  private companion object {
    const val SERVER = "https://abs.example.com"
    const val PATH = "/data/covers/li_1.5"
  }
}
