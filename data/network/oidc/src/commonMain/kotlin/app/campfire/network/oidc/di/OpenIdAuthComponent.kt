// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc.di

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
interface OpenIdAuthComponent : PlatformOpenIdAuthComponent

expect interface PlatformOpenIdAuthComponent
