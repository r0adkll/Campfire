// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.reflect

import kotlin.reflect.KClass

fun Any.instanceOf(type: KClass<*>): Boolean = type.isInstance(this)
