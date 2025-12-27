package app.campfire.network.oidc.di

import android.app.Application
import app.campfire.core.ComponentActivityPlugin
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.oidc.AndroidAuthorizationFlow
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.network.oidc.StartActivityForResultFlowLauncher
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides

actual interface PlatformOpenIdAuthComponent {

  @Provides
  fun provideAndroidAuthorizationFlow(
    application: Application,
    api: AuthAudioBookShelfApi,
    launcher: StartActivityForResultFlowLauncher,
  ): AuthorizationFlow = AndroidAuthorizationFlow(
    application = application,
    authApi = api,
    launcher = launcher,
  )

  @Provides
  @IntoSet
  fun provideStartActivityForResultFlowLauncher(
    launcher: StartActivityForResultFlowLauncher,
  ): ComponentActivityPlugin = launcher
}
