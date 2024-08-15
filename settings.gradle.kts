pluginManagement {
  includeBuild("gradle/build-logic")

  fun hasProperty(key: String): Boolean {
    return settings.providers.gradleProperty(key).get().toBoolean()
  }

  repositories {

    if (hasProperty("campfire.config.enableSnapshots")) {
      maven("https://oss.sonatype.org/content/repositories/snapshots") {
        name = "snapshots-maven-central"
//        mavenContent { snapshotsOnly() }
      }
    }

    if (hasProperty("campfire.config.enableMavenLocal")) {
      mavenLocal()
    }

    google()
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {

  fun hasProperty(key: String): Boolean {
    return settings.providers.gradleProperty(key).get().toBoolean()
  }

  repositories {
    if (hasProperty("campfire.config.enableSnapshots")) {
      maven("https://oss.sonatype.org/content/repositories/snapshots") {
        name = "snapshots-maven-central"
//        mavenContent { snapshotsOnly() }
      }
    }

    if (hasProperty("campfire.config.enableMavenLocal")) {
      mavenLocal {
        content { includeGroup("com.r0adkll.kimchi") }
      }
    }

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
  ":thirdparty:shimmer",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
