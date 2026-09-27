// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.di.qualifier

import dev.zacsweers.metro.Qualifier
import kotlin.reflect.KClass

@Qualifier
annotation class ForScope(val scope: KClass<*>)
