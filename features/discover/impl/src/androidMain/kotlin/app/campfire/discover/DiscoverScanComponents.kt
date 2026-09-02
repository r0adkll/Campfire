// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.UserScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesTo

/**
 * Accessors for [DiscoverScanWorker], which is constructed by WorkManager and
 * resolves its dependencies through
 * [app.campfire.core.di.ComponentHolder] on every run — the user graph is
 * rebuilt on session swaps, so nothing here may be cached across suspends.
 */
@ContributesTo(UserScope::class)
interface DiscoverScanUserComponent {
  val currentUserSession: UserSession
  val seriesScanner: SeriesScanner
  val discoverScanStateStore: DiscoverScanStateStore
  val bookInfoRegistry: BookInfoRegistry
}

@ContributesTo(AppScope::class)
interface DiscoverScanAppComponent {
  val activityIntentProvider: ActivityIntentProvider
  val appLifecycleObserver: AppLifecycleObserver
}
