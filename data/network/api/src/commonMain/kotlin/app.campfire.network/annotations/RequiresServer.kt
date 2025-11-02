package app.campfire.network.annotations

/**
 * This annotation is used to denote api endpoints that require a minimum version of the server
 * to be requested.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class RequiresServer(val version: String)
