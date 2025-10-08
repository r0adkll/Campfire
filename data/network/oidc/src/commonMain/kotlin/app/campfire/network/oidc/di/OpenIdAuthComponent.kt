package app.campfire.network.oidc.di

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(AppScope::class)
interface OpenIdAuthComponent : PlatformOpenIdAuthComponent

expect interface PlatformOpenIdAuthComponent
