import kotlinx.kover.gradle.aggregation.settings.dsl.KoverSettingsExtension
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

pluginManagement {
  includeBuild("gradle/build-logic")

  fun hasProperty(key: String): Boolean {
    return settings.providers.gradleProperty(key).get().toBoolean()
  }

  repositories {

    if (hasProperty("campfire.config.enableSnapshots")) {
      maven("https://oss.sonatype.org/content/repositories/snapshots") {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
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
        mavenContent { snapshotsOnly() }
      }
    }

    if (hasProperty("campfire.config.enableMavenLocal")) {
      mavenLocal()
    }

    google()
    mavenCentral()

    // Prerelease versions of Compose Multiplatform
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

plugins {
  id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.3"
}

extensions.configure<KoverSettingsExtension> {
  enableCoverage()
  reports {
    verify {
      rule {
        bound {
          minValue = 50
          coverageUnits = CoverageUnit.LINE
        }
      }
    }
  }
}

val isCi = providers.environmentVariable("CI").isPresent
buildCache {
  local {
    isEnabled = !isCi
  }
}

rootProject.name = "Campfire"
include(
  ":app:android",
  ":app:common",
  ":app:desktop",
  ":app:ios",
)
include(":app:baselineprofile")
include(":core")
include(
  ":infra:audioplayer:api",
  ":infra:audioplayer:impl",
  ":infra:audioplayer:public-ui",
  ":infra:shake",
  ":infra:debug",
  ":infra:updates:api",
  ":infra:updates:impl",
  ":infra:tracing",
)
include(
  ":common:screens",
  ":common:compose",
)
include(
  ":data:account:api",
  ":data:account:impl",
  ":data:account:ui",
)
include(
  ":data:analytics:api",
  ":data:analytics:impl",
  ":data:analytics:mixpanel",
)
include(
  ":data:crashreporting:api",
  ":data:crashreporting:impl",
)
include(
  ":data:network:api",
  ":data:network:impl",
  ":data:db",
  ":data:mapping",
)
include(
  ":features:auth:api",
  ":features:auth:impl",
  ":features:auth:ui",
)
include(
  ":features:user:api",
  ":features:user:impl",
)
include(
  ":features:libraries:api",
  ":features:libraries:impl",
  ":features:libraries:ui",
)
include(
  ":features:home:api",
  ":features:home:impl",
  ":features:home:ui",
)
include(
  ":features:series:api",
  ":features:series:impl",
  ":features:series:ui",
)
include(
  ":features:collections:api",
  ":features:collections:impl",
  ":features:collections:ui",
)
include(
  ":features:author:api",
  ":features:author:impl",
  ":features:author:ui",
)
include(
  ":features:sessions:api",
  ":features:sessions:impl",
  ":features:sessions:ui",
)
include(
  ":features:search:api",
  ":features:search:impl",
  ":features:search:ui",
)
include(
  ":features:settings:api",
  ":features:settings:impl",
  ":features:settings:ui",
)
include(
  ":features:stats:api",
  ":features:stats:impl",
  ":features:stats:ui",
)
include(
  ":ui:appbar",
  ":ui:drawer",
  ":ui:attribution",
  ":ui:widgets:api",
  ":ui:widgets:impl",
)
include(":scripts:app")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
