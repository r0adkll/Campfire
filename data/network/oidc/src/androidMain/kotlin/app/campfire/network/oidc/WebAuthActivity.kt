// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

internal const val EXTRA_KEY_URL = "url"
internal const val EXTRA_KEY_BROWSER_PACKAGE = "browser_package"

/** The scheme Audiobookshelf sends the app back to once the OpenID provider is done. */
private const val APP_REDIRECT_SCHEME = "campfireaudiobooks"

/**
 * Sends the user to their OpenID provider and returns where it sent them back to.
 *
 * Phones open the provider in a custom tab, and come back through the `campfireaudiobooks://oauth`
 * intent filter. Cars, and devices with no browser, sign in in a web view here instead: a car's
 * link handler shows links rather than opening them, so a custom tab would never come back.
 */
class WebAuthActivity : ComponentActivity() {

  private var webView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onResume() {
    super.onResume()
    // Signing in here: pausing (the car's driving restrictions, the keyboard) isn't coming back
    if (webView != null) return

    val url = intent.extras?.getString(EXTRA_KEY_URL)
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

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
          showWebView(url)
        } else {
          launchCustomTabIntent(
            url = url,
            preferredBrowserPackage = preferredBrowserPackage,
          )
        }
      }
    }
  }

  private fun launchCustomTabIntent(
    url: String,
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
      showWebView(url)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  /**
   * Signs in in a web view, with a Cancel button outside it: a car has no system back button to
   * rely on. Back goes back a page, and cancels from the first one.
   */
  private fun showWebView(url: String) {
    val webView = createWebView().also { webView = it }

    val cancel = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
      setText(android.R.string.cancel)
      // Big enough to hit in a car
      minHeight = (CANCEL_MIN_SIZE_DP * resources.displayMetrics.density).toInt()
      minWidth = minHeight * 2
      setTextSize(TypedValue.COMPLEX_UNIT_SP, CANCEL_TEXT_SIZE_SP)
      setOnClickListener { cancel() }
    }
    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      addView(cancel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.START })
      addView(webView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
    }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.systemBars() or
          WindowInsetsCompat.Type.displayCutout() or
          WindowInsetsCompat.Type.ime(),
      )
      view.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = insets.bottom)
      WindowInsetsCompat.CONSUMED
    }

    onBackPressedDispatcher.addCallback(this) {
      if (webView.canGoBack()) webView.goBack() else cancel()
    }

    // Every session is ephemeral, so reset the history and cookies
    CookieManager.getInstance().removeAllCookies(null)
    webView.clearHistory()
    webView.clearCache(true)

    setContentView(root)
    webView.loadUrl(url)
  }

  private fun createWebView(): WebView = WebView(this).apply {
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
        val requestedUrl = request?.url ?: return false
        return if (requestedUrl.isRedirectBack()) {
          setResult(RESULT_OK, Intent().setData(requestedUrl))
          finish()
          true
        } else {
          false
        }
      }
    }
  }

  /**
   * The server sending the user back to the app, the same URL a custom tab hands the app on a
   * phone. The provider's own redirect to the server's callback goes on loading, so the server
   * finishes its side of the flow first.
   */
  private fun Uri.isRedirectBack(): Boolean = scheme == APP_REDIRECT_SCHEME

  private fun cancel() {
    setResult(RESULT_CANCELED)
    finish()
  }

  private companion object {
    const val CANCEL_MIN_SIZE_DP = 64
    const val CANCEL_TEXT_SIZE_SP = 20f
  }

  override fun onDestroy() {
    webView?.destroy()
    webView = null
    super.onDestroy()
  }
}
