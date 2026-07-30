// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai.halogen

import halogen.HalogenLlmProvider

internal actual fun createNanoProvider(): HalogenLlmProvider? = DiagnosticGeminiNanoProvider()
