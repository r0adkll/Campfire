// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.content

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface CoverContentUserComponent {
  val urlHydrator: UrlHydrator
}
