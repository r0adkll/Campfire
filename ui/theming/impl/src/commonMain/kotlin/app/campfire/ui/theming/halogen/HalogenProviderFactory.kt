package app.campfire.ui.theming.halogen

import halogen.HalogenLlmProvider

/**
 * Build the platform's on-device LLM provider for Halogen, or `null` if the platform
 * doesn't support on-device GenAI.
 *
 * - **Android**: returns [DiagnosticGeminiNanoProvider] (Gemini Nano via ML Kit).
 * - **iOS / Desktop**: returns `null` — the AI Theme Builder feature is hidden.
 */
internal expect fun createNanoProvider(): HalogenLlmProvider?
