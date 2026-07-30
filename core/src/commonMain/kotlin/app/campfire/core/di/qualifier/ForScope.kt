// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.di.qualifier

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Qualifier

@Qualifier
annotation class ForScope(val scope: KClass<*>)
