package app.campfire.core.di

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Scope

/**
 * kotlin-inject scope marker for use with merge-scope classes
 */
@Scope
annotation class SingleIn(val mergeScope: KClass<*>)
