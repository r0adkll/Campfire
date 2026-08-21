// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.image

import app.campfire.core.image.CoverUrls
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import coil3.Image
import coil3.PlatformContext
import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class CoverSizingInterceptorTest {

  private val interceptor = CoverSizingInterceptor()
  private val cover = "https://abs.example.com/api/items/li_1/cover?ts=5"

  @Test
  fun sizes_cover_request_from_resolved_width() = runTest {
    val chain = FakeChain(request(cover), Size(Dimension(360), Dimension(360)))
    interceptor.intercept(chain)
    assertThat(chain.proceeded?.data).isEqualTo("$cover&width=400")
  }

  @Test
  fun falls_back_to_height_when_width_is_undefined() = runTest {
    val chain = FakeChain(request(cover), Size(Dimension.Undefined, Dimension(700)))
    interceptor.intercept(chain)
    assertThat(chain.proceeded?.data).isEqualTo("$cover&width=800")
  }

  @Test
  fun uses_artwork_width_for_original_size_requests() = runTest {
    val chain = FakeChain(request(cover), Size.ORIGINAL)
    interceptor.intercept(chain)
    assertThat(chain.proceeded?.data).isEqualTo("$cover&width=${CoverUrls.ARTWORK_WIDTH}")
  }

  @Test
  fun asks_server_for_webp() = runTest {
    val chain = FakeChain(request(cover), Size(Dimension(200), Dimension(200)))
    interceptor.intercept(chain)
    assertThat(chain.proceeded?.httpHeaders?.get("Accept")).isEqualTo(CoverSizingInterceptor.ACCEPT_HEADER)
  }

  @Test
  fun leaves_non_server_urls_untouched() = runTest {
    val external = "https://cdn.example.com/art.png"
    val chain = FakeChain(request(external), Size(Dimension(360), Dimension(360)))
    interceptor.intercept(chain)
    assertThat(chain.proceeded).isNull()
    assertThat(chain.proceedCount).isEqualTo(1)
  }

  @Test
  fun leaves_non_string_data_untouched() = runTest {
    val chain = FakeChain(ImageRequest.Builder(PlatformContext.INSTANCE).data(42).build(), Size.ORIGINAL)
    interceptor.intercept(chain)
    assertThat(chain.proceeded).isNull()
    assertThat(chain.proceedCount).isEqualTo(1)
  }

  private fun request(url: String) = ImageRequest.Builder(PlatformContext.INSTANCE).data(url).build()

  /** Records the request handed to [proceed] after [withRequest]; null when proceeded with the original. */
  private class FakeChain(
    override val request: ImageRequest,
    override val size: Size,
    private val replaced: Boolean = false,
  ) : Interceptor.Chain {
    var proceeded: ImageRequest? = null
    var proceedCount = 0
    private var parent: FakeChain? = null

    override fun withRequest(request: ImageRequest): Interceptor.Chain =
      FakeChain(request, size, replaced = true).also { it.parent = this }

    override fun withSize(size: Size): Interceptor.Chain =
      FakeChain(request, size, replaced).also { it.parent = parent }

    override suspend fun proceed(): ImageResult {
      val root = generateSequence(this) { it.parent }.last()
      root.proceedCount++
      if (replaced) root.proceeded = request
      return SuccessResult(image = NoopImage, request = request)
    }
  }

  private object NoopImage : Image {
    override val size: Long = 0
    override val width: Int = 1
    override val height: Int = 1
    override val shareable: Boolean = true
    override fun draw(canvas: coil3.Canvas) = Unit
  }
}
