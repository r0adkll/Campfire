package app.campfire.network.oidc

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.campfire.network.oidc.customtab.getCustomTabProviders
import io.ktor.http.Url

internal class ActivityWebAuthFlow(
  private val context: Context,
  private val launcher: StartActivityForResultFlowLauncher,
) : WebAuthFlow {

  override suspend fun startWebFlow(
    requestUrl: Url,
    redirectUri: String,
  ): WebAuthFlowResult {
    val customTabsProviders = context.getCustomTabProviders().map { it.activityInfo.packageName }
    val preferredBrowserPackage = if (customTabsProviders.isNotEmpty()) {
      customTabsProviders.firstOrNull()
    } else {
      null
    }

    val intent = prepareIntent(
      requestUrl = requestUrl.toString(),
      redirectUri = redirectUri,
      preferredBrowserPackage = preferredBrowserPackage,
    )
    return launcher.launch(intent)
      .fold(
        onSuccess = {
          when (it.resultCode) {
            Activity.RESULT_OK -> WebAuthFlowResult.Success(it.data?.data?.let { Url(it.toString()) })
            else -> WebAuthFlowResult.Cancelled
          }
        },
        onFailure = {
          WebAuthFlowResult.Cancelled
        },
      )
  }

  private fun prepareIntent(
    requestUrl: String,
    redirectUri: String,
    preferredBrowserPackage: String?,
  ): Intent {
    return Intent(context, WebAuthActivity::class.java).apply {
      putExtra(EXTRA_KEY_URL, requestUrl)
      putExtra(EXTRA_KEY_REDIRECTURL, redirectUri)
      preferredBrowserPackage?.let {
        putExtra(EXTRA_KEY_BROWSER_PACKAGE, it)
      }
    }
  }
}
