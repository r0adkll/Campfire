package app.campfire.network.plugins

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.util.AttributeKey
import io.ktor.util.appendAll

class SuspendingDefaultRequestHeaders private constructor(
  private val block: SuspendingDefaultRequestHeadersBuilder.() -> Unit,
) {

  companion object Plugin : HttpClientPlugin<SuspendingDefaultRequestHeadersBuilder, SuspendingDefaultRequestHeaders> {
    override val key: AttributeKey<SuspendingDefaultRequestHeaders> = AttributeKey("SuspendingDefaultRequest")

    override fun prepare(block: SuspendingDefaultRequestHeadersBuilder.() -> Unit): SuspendingDefaultRequestHeaders =
      SuspendingDefaultRequestHeaders(block)

    override fun install(
      plugin: SuspendingDefaultRequestHeaders,
      scope: HttpClient,
    ) {
      scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
        val defaultRequest = SuspendingDefaultRequestHeadersBuilder().apply {
          headers.appendAll(this@intercept.context.headers)
          val userHeaders = headers.build()

          // Allow the suspending builder to populate
          plugin.block(this)

          // Apply the suspending builder
          suspendingBuilder()

          // KTOR-6946 User's headers should have higher priority
          userHeaders.entries().forEach { (key, oldValues) ->
            val newValues = headers.getAll(key)
            if (newValues == null) {
              headers.appendAll(key, oldValues)
              return@forEach
            }

            if (newValues == oldValues || key == HttpHeaders.Cookie) return@forEach

            headers.remove(key)
            headers.appendAll(key, oldValues)
            headers.appendMissing(key, newValues)
          }
        }

        context.headers.clear()
        context.headers.appendAll(defaultRequest.headers.build())
      }
    }
  }

  class SuspendingDefaultRequestHeadersBuilder internal constructor() : HttpMessageBuilder {
    override val headers: HeadersBuilder = HeadersBuilder()
    internal var suspendingBuilder: suspend SuspendingDefaultRequestHeadersBuilder.() -> Unit = {}
  }
}

fun HttpClientConfig<*>.suspendingDefaultHeaders(
  block: suspend SuspendingDefaultRequestHeaders.SuspendingDefaultRequestHeadersBuilder.() -> Unit,
) {
  install(SuspendingDefaultRequestHeaders) {
    suspendingBuilder = block
  }
}
