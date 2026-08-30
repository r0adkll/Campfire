// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.settings

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

/**
 * Component to be implemented by platform configurations to provide the
 * encrypted settings store for Hardcover credentials.
 */
expect interface PlatformHardcoverSettingsComponent

@ContributesTo(AppScope::class)
interface HardcoverSettingsComponent : PlatformHardcoverSettingsComponent
