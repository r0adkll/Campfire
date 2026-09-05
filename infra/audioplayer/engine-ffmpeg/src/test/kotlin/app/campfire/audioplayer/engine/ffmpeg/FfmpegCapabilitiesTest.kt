// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.ffmpeg

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlin.test.Test
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avfilter
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer

/**
 * Proves the bundled (LGPL) FFmpeg build carries everything Campfire needs before any engine
 * code depends on it: the audiobook/podcast codecs and containers Audiobookshelf serves, HTTPS
 * and HLS input, and the filters for pitch-preserving rate change and the equalizer.
 */
class FfmpegCapabilitiesTest {

  @Test
  fun `bundled ffmpeg has the decoders, demuxers, protocols, and filters Campfire needs`() {
    println("FFmpeg ${avutil.av_version_info().string} (avformat ${avformat.avformat_version()})")

    val missingDecoders = listOf("aac", "mp3", "flac", "opus", "vorbis", "alac", "pcm_s16le")
      .filter { avcodec.avcodec_find_decoder_by_name(it) == null }
    val missingDemuxers = listOf("mov,mp4,m4a,3gp,3g2,mj2", "mp3", "flac", "ogg", "hls", "wav", "matroska,webm")
      .filter { avformat.av_find_input_format(it) == null }
    val missingFilters = listOf(
      "atempo",
      "anequalizer",
      "superequalizer",
      "volume",
      "aformat",
      "aresample",
      "abuffer",
      "abuffersink",
    )
      .filter { avfilter.avfilter_get_by_name(it) == null }
    val protocols = inputProtocols()
    val missingProtocols = listOf("http", "https", "tls", "file").filter { it !in protocols }

    println("protocols: $protocols")
    assertThat(missingDecoders, "decoders").isEmpty()
    assertThat(missingDemuxers, "demuxers").isEmpty()
    assertThat(missingFilters, "filters").isEmpty()
    assertThat(missingProtocols, "protocols").isEmpty()
  }

  private fun inputProtocols(): List<String> {
    val names = mutableListOf<String>()
    val opaque = PointerPointer<BytePointer>(1L)
    while (true) {
      val name: BytePointer = avformat.avio_enum_protocols(opaque, 0) ?: break
      names += name.string
    }
    return names
  }
}
