// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.mixpanel.di

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(AppScope::class)
interface MixPanelComponent : PlatformMixPanelComponent

expect interface PlatformMixPanelComponent
