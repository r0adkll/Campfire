// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider

/**
 * How streamed items are delivered from the server.
 *
 * [AUTO] lets the playback router pick: direct play normally, an HLS transcode session for
 * large single-file books whose progressive seeks perform poorly. [DIRECT_PLAY_ONLY] is
 * today's behavior, always. [PREFER_HLS] requests an HLS session for every streamed item —
 * for listeners on slow servers who want segment-based seeking everywhere. Downloads always
 * play locally regardless.
 */
enum class StreamingMethod(override val storageKey: String) : EnumSetting {
  AUTO("auto"),
  DIRECT_PLAY_ONLY("direct_play"),
  PREFER_HLS("prefer_hls"),
  ;

  companion object : EnumSettingProvider<StreamingMethod> {
    override fun fromStorageKey(key: String?): StreamingMethod {
      return entries.find { it.storageKey == key } ?: DIRECT_PLAY_ONLY
    }
  }
}
