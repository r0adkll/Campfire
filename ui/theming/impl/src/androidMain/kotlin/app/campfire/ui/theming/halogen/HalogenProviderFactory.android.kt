package app.campfire.ui.theming.halogen

import halogen.HalogenLlmProvider

internal actual fun createNanoProvider(): HalogenLlmProvider? = DiagnosticGeminiNanoProvider()
