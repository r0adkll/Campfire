package app.campfire.core.di.qualifier

import me.tatarka.inject.annotations.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Named(val name: String)
