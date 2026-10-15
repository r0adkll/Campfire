// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingMappedIfSafe
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorFileDoesNotExist
import platform.Foundation.NSURLRequest
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIColor
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * Hosts the reader page in a [WKWebView] under the `campfire-reader` scheme, with every request
 * served from [ReaderContent] by a [WKURLSchemeHandlerProtocol]. Nothing is loaded from the
 * network.
 */
@ContributesBinding(AppScope::class, replaces = [UnsupportedBookRenderer::class])
@Inject
class WebKitBookRenderer(
  private val applicationInfo: ApplicationInfo,
) : BookRenderer {
  override val isSupported: Boolean get() = true

  @Composable
  override fun Content(
    content: ReaderContent,
    bridge: ReaderBridge,
    modifier: Modifier,
  ) {
    val scope = rememberCoroutineScope()
    val schemeHandler = remember(content) { ReaderSchemeHandler(content, scope) }
    val messageHandler = remember(bridge) { ReaderMessageHandler(bridge) }
    // Held here: a web view keeps only a weak reference to its navigation delegate
    val processDelegate = remember { ReaderNavigationDelegate() }
    var webView by remember { mutableStateOf<WKWebView?>(null) }

    UIKitView(
      factory = {
        val configuration = WKWebViewConfiguration().apply {
          setURLSchemeHandler(schemeHandler, forURLScheme = SCHEME)
          userContentController.addScriptMessageHandler(messageHandler, name = MESSAGE_HANDLER)
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
          setOpaque(false)
          navigationDelegate = processDelegate
          backgroundColor = UIColor.clearColor
          scrollView.backgroundColor = UIColor.clearColor
          scrollView.scrollEnabled = false
          scrollView.bounces = false
          if (applicationInfo.debugBuild && respondsToSelector(NSSelectorFromString("setInspectable:"))) {
            setInspectable(true)
          }
          loadRequest(NSURLRequest(NSURL(string = "$SCHEME://app${ReaderContent.ENTRY_PATH}")))
          webView = this
        }
      },
      onRelease = { view ->
        view.configuration.userContentController.removeScriptMessageHandlerForName(MESSAGE_HANDLER)
      },
      properties = UIKitInteropProperties(
        isInteractive = true,
        isNativeAccessibilityEnabled = true,
      ),
      modifier = modifier,
    )

    LaunchedEffect(webView) {
      val view = webView ?: return@LaunchedEffect
      for (script in bridge.scripts) {
        view.evaluateJavaScript(script, completionHandler = null)
      }
    }
  }
}

private const val SCHEME = "campfire-reader"
private const val MESSAGE_HANDLER = "campfire"

private class ReaderMessageHandler(
  private val bridge: ReaderBridge,
) : NSObject(), WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
  ) {
    (didReceiveScriptMessage.body as? String)?.let(bridge::onMessage)
  }
}

/**
 * Reloads the page when WebKit's content process is gone, as iOS may end it under memory
 * pressure; the page reopens the book where the reader is.
 */
private class ReaderNavigationDelegate : NSObject(), WKNavigationDelegateProtocol {
  override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
    bark(LogPriority.WARN) { "Reader content process terminated; reloading" }
    webView.reload()
  }
}

/** Serves the reader's requests on the main thread; a task WebKit stopped is never answered. */
private class ReaderSchemeHandler(
  private val content: ReaderContent,
  private val scope: CoroutineScope,
) : NSObject(), WKURLSchemeHandlerProtocol {
  private val stopped = mutableSetOf<WKURLSchemeTaskProtocol>()

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
    val task = startURLSchemeTask
    val url = task.request.URL
    scope.launch {
      val resource = url?.path?.let { content.resolve(it) }
      if (stopped.remove(task)) return@launch

      val data = when (resource) {
        is ReaderResource.Bytes -> resource.bytes.toNSData()
        is ReaderResource.File -> NSData.dataWithContentsOfFile(resource.path, NSDataReadingMappedIfSafe, null)
        null -> null
      }
      if (url == null || resource == null || data == null) {
        task.didFailWithError(NSError.errorWithDomain(NSURLErrorDomain, NSURLErrorFileDoesNotExist, null))
        return@launch
      }

      val response = NSHTTPURLResponse(
        uRL = url,
        statusCode = 200,
        HTTPVersion = "HTTP/1.1",
        headerFields = mapOf<Any?, Any?>(
          "Content-Type" to resource.mimeType,
          "Content-Length" to data.length.toString(),
        ),
      )
      task.didReceiveResponse(response)
      task.didReceiveData(data)
      task.didFinish()
    }
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
    stopped += stopURLSchemeTask
  }
}

private fun ByteArray.toNSData(): NSData = if (isEmpty()) {
  NSData()
} else {
  usePinned { NSData.dataWithBytes(it.addressOf(0), size.convert()) }
}
