// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.whatsnew.api

import kotlinx.coroutines.flow.Flow

interface WhatsNewRepository {

  suspend fun getChangelog(): Changelog

  fun observeShouldShowWhatsNew(): Flow<Boolean>
  suspend fun dismissWhatsNew()
}
