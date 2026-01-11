package app.campfire.network.oidc.di

import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.network.oidc.OpenIdAuthorization
import me.tatarka.inject.annotations.Provides

actual interface PlatformOpenIdAuthComponent {
  @Provides
  fun provideDesktopAuthorizationFlow(): AuthorizationFlow = object : AuthorizationFlow {
    override suspend fun getAuthorization(
      serverUrl: String,
      extraHeaders: Map<String, String>?,
    ): Result<OpenIdAuthorization> {
      return Result.failure(IllegalStateException("OAuth not supported on this platform"))
    }
  }
}
