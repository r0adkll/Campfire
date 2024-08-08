pluginManagement {
  includeBuild("gradle/build-logic")
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()

    // Prerelease versions of Compose Multiplatform
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

val isCi = providers.environmentVariable("CI").isPresent
buildCache {
  local {
    isEnabled = !isCi
  }
}

rootProject.name = "Campfire"
include(":androidApp")
include(":desktopApp")
include(":shared")
include(":core")
include(
  ":common:screens",
  ":common:compose",
  ":common:settings",
)
include(
  ":data:account:api",
  ":data:account:impl",
  ":data:network:public",
  ":data:network:impl",
  ":data:db",
)
include(
  ":features:auth:api",
  ":features:auth:impl",
  ":features:auth:ui",
)
include(
  ":features:libraries:api",
  ":features:libraries:impl",
  ":features:libraries:ui",
)
include(
  ":di:kotlin-inject-merge",
  ":di:kotlin-inject-merge-annotations",
)
include(
  ":thirdparty:shimmer",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
