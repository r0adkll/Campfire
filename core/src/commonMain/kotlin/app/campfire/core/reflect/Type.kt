package app.campfire.core.reflect

import kotlin.reflect.KClass

fun Any.instanceOf(type: KClass<*>): Boolean = type.isInstance(this)
