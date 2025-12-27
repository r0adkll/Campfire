package app.campfire.network.oidc

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

internal const val EXTRA_KEY_REDIRECTURL = "redirecturl"
internal const val EXTRA_KEY_URL = "url"
internal const val EXTRA_KEY_BROWSER_PACKAGE = "browser_package"

class WebAuthActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onResume() {
    super.onResume()
    val url = intent.extras?.getString(EXTRA_KEY_URL)
    val redirectUrl = intent.extras?.getString(EXTRA_KEY_REDIRECTURL)
    val preferredBrowserPackage = intent.extras?.getString(EXTRA_KEY_BROWSER_PACKAGE)

    if (intent?.data != null) {
      // we're called by custom tab
      // create new intent for result to mitigate intent redirection vulnerability
      setResult(RESULT_OK, Intent().setData(intent?.data))
      finish()
    } else if (url == null) {
      // called by custom tab but no intent.data
      setResult(RESULT_CANCELED)
      finish()
    } else {
      // check if launch tab request is legit
      if (packageName == applicationContext.packageName) {
        // login requested by app
        // do not navigate to the login page again in this activity instance
        intent.removeExtra(EXTRA_KEY_URL)

        launchCustomTabIntent(
          url = url,
          redirectUrl = redirectUrl,
          preferredBrowserPackage = preferredBrowserPackage,
        )
      }
    }
  }

  private fun launchCustomTabIntent(
    url: String,
    redirectUrl: String?,
    preferredBrowserPackage: String?,
    ephemeralSession: Boolean = true,
  ) {
    val builder = CustomTabsIntent.Builder()

    if (preferredBrowserPackage != null) {
      // Enable ephemeral browsing if supported
      if (CustomTabsClient.isEphemeralBrowsingSupported(this, preferredBrowserPackage)) {
        builder.setEphemeralBrowsingEnabled(ephemeralSession)
      }
    }

    val intent = builder.build()
    preferredBrowserPackage?.let {
      intent.intent.setPackage(it)
    }
    try {
      intent.launchUrl(this, url.toUri())
    } catch (_: ActivityNotFoundException) {
      showWebView(url, redirectUrl)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  companion object {
    var createWebView: ComponentActivity.(redirectUrl: String?) -> WebView = { redirectUrl ->
      WebView(this).apply {
        settings.apply {
          javaScriptEnabled = true
          javaScriptCanOpenWindowsAutomatically = false
          setSupportMultipleWindows(false)
          safeBrowsingEnabled = false
        }

        webChromeClient = WebChromeClient()

        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val requestedUrl = request?.url
            return if (requestedUrl != null && redirectUrl != null && requestedUrl.toString().startsWith(redirectUrl)) {
              intent.data = request.url
              setResult(RESULT_OK, intent)
              finish()
              true
            } else {
              false
            }
          }
        }
      }
    }

    var showWebView: ComponentActivity.(url: String, redirectUrl: String?) -> Unit = { url, redirectUrl ->
      val webView = createWebView(this, redirectUrl)
      ViewCompat.setOnApplyWindowInsetsListener(webView) { view, windowInsets ->
        val insets = windowInsets.getInsets(
          WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
          topMargin = insets.top
          leftMargin = insets.left
          bottomMargin = insets.bottom
          rightMargin = insets.right
        }
        WindowInsetsCompat.CONSUMED
      }

      // Every session is ephemeral, so reset the history and cookies
      CookieManager.getInstance().removeAllCookies(null)
      webView.clearHistory()
      webView.clearCache(true)

      // Set content and show
      setContentView(webView)
      webView.loadUrl(url)
    }
  }
}
