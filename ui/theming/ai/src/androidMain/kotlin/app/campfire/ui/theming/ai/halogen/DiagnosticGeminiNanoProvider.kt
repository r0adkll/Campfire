package app.campfire.ui.theming.ai.halogen

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import halogen.HalogenLlmAvailability
import halogen.HalogenLlmProvider
import halogen.provider.nano.GeminiNanoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wraps [GeminiNanoProvider] with diagnostic logging that exposes the underlying
 * ML Kit `FeatureStatus`, and proactively triggers the Nano model download when the
 * device reports `DOWNLOADABLE`.
 *
 * The library's provider silently catches exceptions and collapses every failure mode
 * into `UNAVAILABLE`, which makes it impossible to tell whether Nano is genuinely
 * unsupported on the device, the model is downloading, the AICore service is missing,
 * etc. This wrapper calls `Generation.getClient().checkStatus()` directly so the raw
 * status (and any exception) lands in logcat under tag "Halogen".
 *
 * It also kicks off the model download once on first DOWNLOADABLE check — Halogen's
 * library never triggers it, so without this the model would sit in DOWNLOADABLE state
 * forever and Nano would never come online.
 */
internal class DiagnosticGeminiNanoProvider(
  private val delegate: GeminiNanoProvider = GeminiNanoProvider(),
) : HalogenLlmProvider {

  private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Volatile
  private var downloadStarted: Boolean = false

  override suspend fun availability(): HalogenLlmAvailability {
    val rawStatus = withContext(Dispatchers.IO) {
      runCatching { Generation.getClient().checkStatus() }
        .onSuccess { status ->
          bark(tag = "Halogen") {
            "Gemini Nano FeatureStatus: $status " +
              "(0=UNAVAILABLE, 1=DOWNLOADABLE, 2=DOWNLOADING, 3=AVAILABLE)"
          }
        }
        .onFailure { e ->
          bark(tag = "Halogen", priority = LogPriority.ERROR) {
            "Gemini Nano checkStatus threw: ${e::class.simpleName}: ${e.message}"
          }
        }
        .getOrNull()
    }

    if (rawStatus == FeatureStatus.DOWNLOADABLE && !downloadStarted) {
      downloadStarted = true
      bark(tag = "Halogen") { "Triggering Gemini Nano model download…" }
      downloadScope.launch {
        runCatching {
          delegate.downloadModel().collect { progress ->
            bark(tag = "Halogen") { "Nano download progress: $progress" }
          }
          bark(tag = "Halogen") { "Nano download complete." }
        }.onFailure { e ->
          bark(tag = "Halogen", priority = LogPriority.ERROR) {
            "Nano download failed: ${e::class.simpleName}: ${e.message}"
          }
          // Allow another attempt on the next availability check.
          downloadStarted = false
        }
      }
    }

    return delegate.availability()
  }

  override suspend fun generate(prompt: String): String = delegate.generate(prompt)
}
