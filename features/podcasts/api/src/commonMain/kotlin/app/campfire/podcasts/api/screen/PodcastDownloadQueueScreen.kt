// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object PodcastDownloadQueueScreen : BaseScreen(name = "PodcastDownloadQueue")
