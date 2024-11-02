package app.campfire.core.di.qualifier

import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Qualifier

@Qualifier
annotation class ForScope(val scope: KClass<*>)
