// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.di.qualifier

import me.tatarka.inject.annotations.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Named(val name: String)
