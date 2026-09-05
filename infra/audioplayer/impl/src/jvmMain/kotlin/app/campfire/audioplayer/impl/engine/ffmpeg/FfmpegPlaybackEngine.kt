// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine.ffmpeg

import app.campfire.audioplayer.impl.engine.EngineState
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.audioplayer.impl.engine.PlaybackEngineEvent
import app.campfire.audioplayer.impl.engine.PlaybackEngineException
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.core.logging.Cork
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avfilter.AVFilterContext
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVIOInterruptCB
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avfilter
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.swresample
import org.bytedeco.ffmpeg.swresample.SwrContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer

/**
 * [PlaybackEngine] over FFmpeg (bytedeco presets) and Java Sound.
 *
 * Each opened item gets a worker thread that demuxes, decodes, resamples to 16-bit PCM, runs the
 * optional filter graph (pitch-preserving tempo, equalizer), and writes to a `SourceDataLine`.
 * Engine methods post commands to that thread and return immediately; the worker applies them
 * between frames and reports back through [events]. Blocking network reads are cut short by
 * FFmpeg's interrupt callback when the item is stopped.
 *
 * Position is derived from what has actually been handed to the audio line: the media time of the
 * first frame after the last (re)start of the filter graph, plus the output samples produced since
 * scaled by the tempo, minus what is still queued in the line's buffer.
 */
class FfmpegPlaybackEngine : PlaybackEngine {

  private val _events = MutableSharedFlow<PlaybackEngineEvent>(
    extraBufferCapacity = EVENT_BUFFER,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  override val events: Flow<PlaybackEngineEvent> = _events.asSharedFlow()

  @Volatile
  private var gain = 1f

  @Volatile
  private var rate = 1f

  @Volatile
  private var equalizer: EqualizerConfig? = null

  private var worker: Worker? = null

  override var volume: Float
    get() = gain
    set(value) {
      gain = value.coerceIn(0f, 1f)
    }

  override fun open(item: MediaItem, startPosition: Duration, playWhenReady: Boolean) {
    worker?.shutdown()
    dbark { "open(${item.id}, start=$startPosition, playWhenReady=$playWhenReady)" }
    worker = Worker(item.uri, startPosition, playWhenReady).also { it.start() }
  }

  override fun play() {
    worker?.post(Command.Play)
  }

  override fun pause() {
    worker?.post(Command.Pause)
  }

  override fun stop() {
    worker?.shutdown()
    worker = null
    _events.tryEmit(PlaybackEngineEvent.StateChanged(EngineState.Idle))
  }

  override fun seekTo(position: Duration) {
    worker?.post(Command.Seek(position))
  }

  override fun setRate(rate: Float) {
    this.rate = rate
    worker?.post(Command.Reconfigure)
  }

  override fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) {
    equalizer = EqualizerConfig(enabled, preampDb, bandGainsDb)
    worker?.post(Command.Reconfigure)
  }

  override fun release() {
    stop()
  }

  private sealed interface Command {
    data object Play : Command
    data object Pause : Command
    data class Seek(val position: Duration) : Command
    data object Reconfigure : Command
    data object Stop : Command
  }

  private class StopRequested : RuntimeException()

  private inner class Worker(
    private val url: String,
    private val startPosition: Duration,
    playWhenReady: Boolean,
  ) : Thread("campfire-ffmpeg-${WORKER_IDS.incrementAndGet()}") {

    private val commands = LinkedBlockingQueue<Command>()

    @Volatile
    private var interrupted = false

    private var paused = !playWhenReady
    private var lineStarted = false

    // FFmpeg state
    private var format: AVFormatContext? = null
    private var codec: AVCodecContext? = null
    private var swr: SwrContext? = null
    private var swrConfigured = false
    private var nextPtsUs = 0L
    private var graph: AVFilterGraph? = null
    private var graphSource: AVFilterContext? = null
    private var graphSink: AVFilterContext? = null
    private var graphTempo = 1f
    private var rebuildGraph = true
    private var audioStreamIndex = -1
    private lateinit var streamTimeBase: AVRational
    private val outLayout = AVChannelLayout()
    private var outChannels = 2
    private var outSampleRate = 48_000

    private val packet: AVPacket = avcodec.av_packet_alloc()
    private val decoded: AVFrame = avutil.av_frame_alloc()
    private val converted: AVFrame = avutil.av_frame_alloc()
    private val filtered: AVFrame = avutil.av_frame_alloc()

    // Kept referenced: FFmpeg calls it from inside blocking reads
    private val interruptCallback = object : AVIOInterruptCB.Callback_Pointer() {
      override fun call(opaque: Pointer?): Int = if (interrupted) 1 else 0
    }

    // Output
    private var line: SourceDataLine? = null
    private var pcm = ByteArray(0)

    // Position accounting
    private var graphBaseUs: Long? = null
    private var outputSamplesSinceBase = 0L
    private var dropBeforeUs = Long.MIN_VALUE
    private var lastPositionEmitUs: Long? = null
    private var lastEmittedPosition = Duration.ZERO

    init {
      isDaemon = true
    }

    fun post(command: Command) {
      commands.offer(command)
    }

    /** Stops the worker and waits briefly for it to release FFmpeg and the audio line. */
    fun shutdown() {
      interrupted = true
      commands.offer(Command.Stop)
      join(SHUTDOWN_JOIN_MILLIS)
    }

    override fun run() {
      emit(PlaybackEngineEvent.StateChanged(EngineState.Opening))
      try {
        openInput()
        openDecoder()
        openOutput()
        if (startPosition > Duration.ZERO) seek(startPosition, emitPosition = false)
        if (paused) emit(PlaybackEngineEvent.StateChanged(EngineState.Paused))
        loop()
      } catch (_: StopRequested) {
        // Shutting down
      } catch (e: PlaybackEngineException) {
        if (!interrupted) fail(e)
      } catch (t: Throwable) {
        if (!interrupted) fail(PlaybackEngineException("FFmpeg playback failed", t))
      } finally {
        cleanup()
      }
    }

    private fun fail(e: PlaybackEngineException) {
      ebark(e) { "Playback failed for ${url.substringBefore('?')}" }
      emit(PlaybackEngineEvent.Error(e))
    }

    // region Setup

    private fun openInput() {
      avformat.avformat_network_init()
      val context = avformat.avformat_alloc_context() ?: throw PlaybackEngineException("avformat_alloc_context failed")
      context.interrupt_callback().callback(interruptCallback)
      format = context

      val options = AVDictionary(null)
      avutil.av_dict_set(options, "user_agent", USER_AGENT, 0)
      avutil.av_dict_set(options, "reconnect", "1", 0)
      avutil.av_dict_set(options, "reconnect_streamed", "1", 0)
      avutil.av_dict_set(options, "reconnect_delay_max", "5", 0)
      avutil.av_dict_set(options, "rw_timeout", NETWORK_TIMEOUT_US, 0)
      try {
        check(avformat.avformat_open_input(context, url, null, options), "avformat_open_input")
      } finally {
        avutil.av_dict_free(options)
      }
      check(avformat.avformat_find_stream_info(context, null as AVDictionary?), "avformat_find_stream_info")

      val durationUs = context.duration()
      if (durationUs > 0) emit(PlaybackEngineEvent.DurationChanged(durationUs.microseconds))
    }

    private fun openDecoder() {
      val context = format!!
      val decoder = AVCodec()
      val index = avformat.av_find_best_stream(context, avutil.AVMEDIA_TYPE_AUDIO, -1, -1, decoder, 0)
      if (index < 0) throw PlaybackEngineException("No audio stream: ${errorString(index)}")
      audioStreamIndex = index
      val stream = context.streams(index)
      streamTimeBase = stream.time_base()

      val codecContext = avcodec.avcodec_alloc_context3(decoder)
        ?: throw PlaybackEngineException("avcodec_alloc_context3 failed")
      codec = codecContext
      check(avcodec.avcodec_parameters_to_context(codecContext, stream.codecpar()), "avcodec_parameters_to_context")
      codecContext.pkt_timebase(streamTimeBase)
      check(avcodec.avcodec_open2(codecContext, decoder, null as AVDictionary?), "avcodec_open2")

      outChannels = codecContext.ch_layout().nb_channels().coerceIn(1, 2)
      outSampleRate = codecContext.sample_rate()
      avutil.av_channel_layout_default(outLayout, outChannels)

      // Configured lazily from the first decoded frame: the decoder's advertised layout and the
      // frames' actual layout can differ in ways swr_convert_frame rejects as "input changed"
      swr = swresample.swr_alloc() ?: throw PlaybackEngineException("swr_alloc failed")
      dbark {
        val channels = codecContext.ch_layout().nb_channels()
        "decoder ${decoder.name().string}: ${codecContext.sample_rate()} Hz, $channels ch"
      }
    }

    private fun openOutput() {
      val audioFormat = AudioFormat(outSampleRate.toFloat(), 16, outChannels, true, false)
      val bufferBytes = (outSampleRate * outChannels * 2 * LINE_BUFFER_MILLIS / 1000)
      val dataLine = AudioSystem.getSourceDataLine(audioFormat)
      dataLine.open(audioFormat, bufferBytes)
      line = dataLine
    }

    // endregion

    // region Playback loop

    private fun loop() {
      while (!interrupted) {
        val command = if (paused) commands.take() else commands.poll()
        if (command != null) handle(command)
        if (paused || interrupted) continue

        val ret = avformat.av_read_frame(format, packet)
        when {
          ret == avutil.AVERROR_EOF -> {
            finish()
            return
          }
          ret < 0 -> {
            if (interrupted) throw StopRequested()
            throw PlaybackEngineException("Read failed: ${errorString(ret)}")
          }
        }
        try {
          if (packet.stream_index() == audioStreamIndex) {
            check(avcodec.avcodec_send_packet(codec, packet), "avcodec_send_packet")
            drainDecoder()
          }
        } finally {
          avcodec.av_packet_unref(packet)
        }
      }
      throw StopRequested()
    }

    private fun handle(command: Command) {
      when (command) {
        Command.Play -> {
          if (paused) {
            paused = false
            if (lineStarted) line?.start()
            emit(PlaybackEngineEvent.StateChanged(EngineState.Playing))
          }
        }
        Command.Pause -> {
          if (!paused) {
            paused = true
            line?.stop()
            emit(PlaybackEngineEvent.StateChanged(EngineState.Paused))
          }
        }
        is Command.Seek -> seek(command.position, emitPosition = true)
        Command.Reconfigure -> rebuildGraph = true
        Command.Stop -> throw StopRequested()
      }
    }

    private fun drainDecoder() {
      while (true) {
        val ret = avcodec.avcodec_receive_frame(codec, decoded)
        if (isAgain(ret) || ret == avutil.AVERROR_EOF) return
        check(ret, "avcodec_receive_frame")
        try {
          handleDecoded(decoded)
        } finally {
          avutil.av_frame_unref(decoded)
        }
      }
    }

    private fun handleDecoded(frame: AVFrame) {
      val timestamp = frame.best_effort_timestamp()
      val ptsUs = if (timestamp == avutil.AV_NOPTS_VALUE) {
        nextPtsUs
      } else {
        avutil.av_rescale_q(timestamp, streamTimeBase, MICROSECONDS)
      }
      nextPtsUs = ptsUs + frame.nb_samples() * 1_000_000L / frame.sample_rate().coerceAtLeast(1)
      if (ptsUs < dropBeforeUs) return

      // swr_init promotes an unspecified-order layout to the native default, then the per-frame
      // check compares the still-unspecified frame layout against it and reports "input changed";
      // give the frame the same default layout up front
      if (frame.ch_layout().order() == avutil.AV_CHANNEL_ORDER_UNSPEC) {
        avutil.av_channel_layout_default(frame.ch_layout(), frame.ch_layout().nb_channels())
      }

      converted.format(avutil.AV_SAMPLE_FMT_S16)
      converted.sample_rate(outSampleRate)
      avutil.av_channel_layout_copy(converted.ch_layout(), outLayout)
      try {
        if (!swrConfigured) configureResampler(frame)
        var ret = swresample.swr_convert_frame(swr, converted, frame)
        if (ret == avutil.AVERROR_INPUT_CHANGED || ret == avutil.AVERROR_OUTPUT_CHANGED) {
          configureResampler(frame)
          ret = swresample.swr_convert_frame(swr, converted, frame)
        }
        check(ret, "swr_convert_frame")
        if (converted.nb_samples() == 0) return
        if (rebuildGraph) configureGraph()
        if (graphBaseUs == null) {
          graphBaseUs = ptsUs
          outputSamplesSinceBase = 0L
        }
        val source = graphSource
        if (source == null) {
          writeOut(converted)
        } else {
          check(avfilter.av_buffersrc_add_frame(source, converted), "av_buffersrc_add_frame")
          drainGraph()
        }
      } finally {
        avutil.av_frame_unref(converted)
      }
    }

    private fun configureResampler(input: AVFrame) {
      check(swresample.swr_config_frame(swr, converted, input), "swr_config_frame")
      check(swresample.swr_init(swr), "swr_init")
      swrConfigured = true
    }

    private fun drainGraph() {
      val sink = graphSink ?: return
      while (true) {
        val ret = avfilter.av_buffersink_get_frame(sink, filtered)
        if (isAgain(ret) || ret == avutil.AVERROR_EOF) return
        check(ret, "av_buffersink_get_frame")
        try {
          writeOut(filtered)
        } finally {
          avutil.av_frame_unref(filtered)
        }
      }
    }

    private fun writeOut(frame: AVFrame) {
      val dataLine = line ?: return
      val bytes = frame.nb_samples() * outChannels * 2
      if (pcm.size < bytes) pcm = ByteArray(bytes)
      frame.data(0).get(pcm, 0, bytes)
      applyGain(pcm, bytes, gain)

      if (!lineStarted) {
        lineStarted = true
        if (!paused) {
          dataLine.start()
          emit(PlaybackEngineEvent.StateChanged(EngineState.Playing))
        }
      }
      // Never block inside the line: write only what fits and poll, so a stop request (or a
      // stalled audio device) can't wedge the worker inside Java Sound
      var offset = 0
      while (offset < bytes) {
        if (interrupted) throw StopRequested()
        val room = dataLine.available()
        if (room <= 0) {
          sleep(WRITE_POLL_MILLIS)
          continue
        }
        offset += dataLine.write(pcm, offset, minOf(room, bytes - offset))
      }
      outputSamplesSinceBase += frame.nb_samples()
      maybeEmitPosition()
    }

    private fun finish() {
      // Flush the decoder and the graph, then let the line play out
      avcodec.avcodec_send_packet(codec, null)
      drainDecoder()
      graphSource?.let { source ->
        avfilter.av_buffersrc_add_frame(source, null)
        drainGraph()
      }
      if (!interrupted) {
        line?.drain()
        emitPosition(force = true)
        emit(PlaybackEngineEvent.StateChanged(EngineState.Ended))
      }
    }

    // endregion

    // region Seeking and filters

    private fun seek(target: Duration, emitPosition: Boolean) {
      val targetUs = target.inWholeMicroseconds.coerceAtLeast(0L)
      val ret = avformat.av_seek_frame(format, -1, targetUs, avformat.AVSEEK_FLAG_BACKWARD)
      if (ret < 0) {
        wbark { "Seek to $target failed: ${errorString(ret)}" }
        return
      }
      avcodec.avcodec_flush_buffers(codec)
      line?.flush()
      // Frames before the target are decoded and dropped so the resume point is exact
      dropBeforeUs = targetUs
      graphBaseUs = null
      outputSamplesSinceBase = 0L
      rebuildGraph = true
      if (emitPosition) {
        lastEmittedPosition = target
        lastPositionEmitUs = System.nanoTime() / 1_000
        emit(PlaybackEngineEvent.PositionChanged(target))
      }
    }

    private fun configureGraph() {
      rebuildGraph = false
      // Media time continues from the next frame fed; samples written so far stay accounted at
      // the old tempo (the small overlap in the line buffer is not worth tracking)
      graphBaseUs = null
      freeGraph()

      val description = FilterChain.build(rate, equalizer, outSampleRate, outChannels)
      graphTempo = rate.coerceIn(0.5f, 4f)
      if (description == null) {
        graphTempo = 1f
        return
      }
      dbark { "filter graph: $description" }

      val newGraph = avfilter.avfilter_graph_alloc() ?: throw PlaybackEngineException("avfilter_graph_alloc failed")
      graph = newGraph
      val source = AVFilterContext()
      val sink = AVFilterContext()
      val sourceArgs = "time_base=1/$outSampleRate:sample_rate=$outSampleRate:sample_fmt=s16:" +
        "channel_layout=${FilterChain.layoutName(outChannels)}"
      check(
        avfilter.avfilter_graph_create_filter(
          source,
          avfilter.avfilter_get_by_name("abuffer"),
          "in",
          sourceArgs,
          null,
          newGraph,
        ),
        "abuffer",
      )
      check(
        avfilter.avfilter_graph_create_filter(
          sink,
          avfilter.avfilter_get_by_name("abuffersink"),
          "out",
          null,
          null,
          newGraph,
        ),
        "abuffersink",
      )

      val outputs = avfilter.avfilter_inout_alloc()
      outputs.name(avutil.av_strdup(BytePointer("in")))
      outputs.filter_ctx(source)
      outputs.pad_idx(0)
      outputs.next(null)
      val inputs = avfilter.avfilter_inout_alloc()
      inputs.name(avutil.av_strdup(BytePointer("out")))
      inputs.filter_ctx(sink)
      inputs.pad_idx(0)
      inputs.next(null)
      check(avfilter.avfilter_graph_parse_ptr(newGraph, description, inputs, outputs, null), "avfilter_graph_parse_ptr")
      check(avfilter.avfilter_graph_config(newGraph, null), "avfilter_graph_config")

      graphSource = source
      graphSink = sink
    }

    private fun freeGraph() {
      graph?.let { avfilter.avfilter_graph_free(it) }
      graph = null
      graphSource = null
      graphSink = null
    }

    // endregion

    // region Position

    private fun positionUs(): Long? {
      val base = graphBaseUs ?: return null
      val dataLine = line ?: return null
      val frameBytes = outChannels * 2
      val pendingSamples = ((dataLine.bufferSize - dataLine.available()) / frameBytes).coerceAtLeast(0)
      val playedSamples = (outputSamplesSinceBase - pendingSamples).coerceAtLeast(0L)
      return base + (playedSamples * graphTempo.toDouble() / outSampleRate * 1_000_000).toLong()
    }

    private fun maybeEmitPosition() {
      val last = lastPositionEmitUs
      if (last != null && System.nanoTime() / 1_000 - last < POSITION_INTERVAL_US) return
      emitPosition(force = false)
    }

    private fun emitPosition(force: Boolean) {
      val positionUs = positionUs() ?: return
      val position = positionUs.microseconds
      if (!force && position == lastEmittedPosition) return
      lastEmittedPosition = position
      lastPositionEmitUs = System.nanoTime() / 1_000
      emit(PlaybackEngineEvent.PositionChanged(position))
    }

    // endregion

    private fun cleanup() {
      runCatching {
        line?.let {
          it.stop()
          it.flush()
          it.close()
        }
      }
      line = null
      freeGraph()
      swr?.let { swresample.swr_free(it) }
      swr = null
      codec?.let { avcodec.avcodec_free_context(it) }
      codec = null
      format?.let { avformat.avformat_close_input(it) }
      format = null
      avcodec.av_packet_free(packet)
      avutil.av_frame_free(decoded)
      avutil.av_frame_free(converted)
      avutil.av_frame_free(filtered)
      avutil.av_channel_layout_uninit(outLayout)
    }

    private fun check(ret: Int, what: String) {
      if (ret < 0) {
        if (interrupted) throw StopRequested()
        throw PlaybackEngineException("$what failed: ${errorString(ret)}")
      }
    }
  }

  private fun emit(event: PlaybackEngineEvent) {
    _events.tryEmit(event)
  }

  companion object : Cork {
    override val tag: String = "FfmpegPlaybackEngine"
    override val enabled: Boolean = true

    private const val EVENT_BUFFER = 256
    private const val USER_AGENT = "Campfire-Desktop"
    private const val NETWORK_TIMEOUT_US = "20000000"
    private const val LINE_BUFFER_MILLIS = 300
    private const val WRITE_POLL_MILLIS = 5L
    private const val POSITION_INTERVAL_US = 250_000L
    private const val SHUTDOWN_JOIN_MILLIS = 1_000L
    private val WORKER_IDS = AtomicInteger()
    private val MICROSECONDS: AVRational = avutil.av_make_q(1, avutil.AV_TIME_BASE)

    // EAGAIN differs by platform (35 on Darwin, 11 elsewhere) and bytedeco exposes no helper
    private fun isAgain(ret: Int): Boolean = ret == -35 || ret == -11

    private fun errorString(ret: Int): String {
      val buffer = ByteArray(ERROR_BUFFER)
      avutil.av_strerror(ret, buffer, ERROR_BUFFER.toLong())
      val end = buffer.indexOf(0).let { if (it < 0) buffer.size else it }
      return String(buffer, 0, end, Charsets.UTF_8)
    }

    private const val ERROR_BUFFER = 256

    /** In-place 16-bit little-endian gain, saturating. */
    private fun applyGain(pcm: ByteArray, bytes: Int, gain: Float) {
      if (gain == 1f) return
      var i = 0
      while (i + 1 < bytes) {
        val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xff)).toShort().toInt()
        val scaled = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        pcm[i] = (scaled and 0xff).toByte()
        pcm[i + 1] = ((scaled shr 8) and 0xff).toByte()
        i += 2
      }
    }
  }
}
