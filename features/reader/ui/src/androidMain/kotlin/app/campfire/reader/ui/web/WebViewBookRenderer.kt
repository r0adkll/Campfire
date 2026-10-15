// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import kotlinx.coroutines.runBlocking

/**
 * Hosts the reader page in a [WebView] at `https://appassets.androidplatform.net`, a secure
 * origin foliate-js needs for Web Crypto, with every request served from [ReaderContent] by a
 * [WebViewAssetLoader]. Nothing is loaded from the network.
 */
@ContributesBinding(AppScope::class, replaces = [UnsupportedBookRenderer::class])
@Inject
class WebViewBookRenderer(
  private val applicationInfo: ApplicationInfo,
) : BookRenderer {
  override val isSupported: Boolean get() = true

  @SuppressLint("SetJavaScriptEnabled")
  @Composable
  override fun Content(
    content: ReaderContent,
    bridge: ReaderBridge,
    modifier: Modifier,
  ) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    // Bumped when the renderer process dies, as the OS may reclaim it under memory pressure: a web
    // view whose renderer is gone can't be used again, so a fresh one opens the book again
    var renderer by remember { mutableIntStateOf(0) }

    key(renderer) {
      AndroidView(
        factory = { context ->
          if (applicationInfo.debugBuild) WebView.setWebContentsDebuggingEnabled(true)
          val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(ReaderContent.READER_PREFIX) { path -> content.respond(ReaderContent.READER_PREFIX + path) }
            .addPathHandler(ReaderContent.BOOK_PREFIX) { path -> content.respond(ReaderContent.BOOK_PREFIX + path) }
            .build()

          WebView(context).apply {
            // Without explicit MATCH_PARENT the web view sizes itself to its content, and the
            // page's 100% heights resolve to 0
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            addJavascriptInterface(MessageInterface(bridge), "CampfireAndroid")
            // foliate-js turns pages on its own swipe handling, which needs the whole gesture: an
            // ancestor claiming the horizontal drag cancels it after the first move
            setOnTouchListener { view, event ->
              if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                view.parent?.requestDisallowInterceptTouchEvent(true)
              }
              false
            }
            webViewClient = object : WebViewClient() {
              override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

              // The page never navigates away; links inside the book are handled by foliate-js
              override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.isForMainFrame

              override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                bark(LogPriority.WARN) { "Reader renderer gone (crashed: ${detail.didCrash()}); reopening" }
                webView = null
                renderer++
                return true
              }
            }
            loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}${ReaderContent.ENTRY_PATH}")
            webView = this
          }
        },
        onRelease = { it.destroy() },
        modifier = modifier,
      )
    }

    LaunchedEffect(webView) {
      val view = webView ?: return@LaunchedEffect
      for (script in bridge.scripts) {
        view.evaluateJavascript(script, null)
      }
    }
  }
}

private class MessageInterface(private val bridge: ReaderBridge) {
  @JavascriptInterface
  fun postMessage(json: String) = bridge.onMessage(json)
}

/** Called on the web view's background thread, so blocking on the resource read is fine. */
private fun ReaderContent.respond(path: String): WebResourceResponse? {
  return when (val resource = runBlocking { resolve(path) }) {
    is ReaderResource.Bytes -> WebResourceResponse(resource.mimeType, null, ByteArrayInputStream(resource.bytes))
    is ReaderResource.File -> WebResourceResponse(resource.mimeType, null, FileInputStream(resource.path))
    null -> null
  }
}
