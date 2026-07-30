// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@OptIn(UnstableApi::class)
internal class CampfireRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

  override fun buildAudioRenderers(
    context: Context,
    extensionRendererMode: Int,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    audioSink: AudioSink,
    eventHandler: Handler,
    eventListener: AudioRendererEventListener,
    out: ArrayList<Renderer>,
  ) {
    out.add(
      MediaCodecAudioRenderer(
        context,
        getCodecAdapterFactory(),
        mediaCodecSelector,
        enableDecoderFallback,
        eventHandler,
        eventListener,
        audioSink,
        // Pass in null to LoudnessCodecController to disable
        // volume changes when listening to xhe-aac audio
        // See https://github.com/r0adkll/Campfire/issues/684
        null,
      ),
    )
  }
}
