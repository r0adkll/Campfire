import kotlinx.kover.gradle.aggregation.settings.dsl.KoverSettingsExtension
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

pluginManagement {
  includeBuild("gradle/build-logic")

  fun hasProperty(key: String): Boolean {
    return settings.providers.gradleProperty(key).get().toBoolean()
  }

  repositories {

    if (hasProperty("campfire.config.enableSnapshots")) {
      maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "snapshots-maven-central"
        mavenContent {
          snapshotsOnly()
        }

        content {
          includeGroup("com.r0adkll.swatchbuckler")
          includeGroup("app.cash.sqldelight")
        }
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
      maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
        content {
          includeGroup("com.r0adkll.swatchbuckler")
          includeGroup("app.cash.sqldelight")
        }
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
  id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.7"
}

extensions.configure<KoverSettingsExtension> {
  enableCoverage()
  reports {
    excludedClasses.add("*.test.*")
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
  ":infra:audioplayer:test",
  ":infra:shake",
  ":infra:debug",
  ":infra:updates:api",
  ":infra:updates:impl",
  ":infra:whats-new:api",
  ":infra:whats-new:impl",
  ":infra:whats-new:test",
  ":infra:whats-new:ui",
  ":infra:tracing",
)
include(
  ":common:screens",
  ":common:compose",
  ":common:test",
)
include(
  ":data:account:api",
  ":data:account:impl",
  ":data:account:ui",
  ":data:account:test",
)
include(
  ":data:analytics:api",
  ":data:analytics:impl",
  ":data:analytics:mixpanel",
  ":data:analytics:test",
)
include(
  ":data:crashreporting:api",
  ":data:crashreporting:impl",
)
include(
  ":data:network:api",
  ":data:network:impl",
  ":data:network:oidc",
  ":data:network:test",
)
include(
  ":data:db:core",
  ":data:db:mapping",
  ":data:db:test",
)
include(
  ":features:auth:api",
  ":features:auth:impl",
  ":features:auth:ui",
)
include(
  ":features:user:api",
  ":features:user:impl",
  ":features:user:test",
)
include(
  ":features:libraries:api",
  ":features:libraries:impl",
  ":features:libraries:ui",
  ":features:libraries:test",
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
  ":features:series:test",
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
  ":features:sessions:test",
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
  ":features:settings:test",
)
include(
  ":features:stats:api",
  ":features:stats:impl",
  ":features:stats:ui",
)
include(
  ":features:filters:api",
  ":features:filters:impl",
  ":features:filters:test",
  ":features:filters:ui",
)
include(
  ":ui:appbar",
  ":ui:navigation",
  ":ui:attribution",
)
include(
  ":ui:widgets:api",
  ":ui:widgets:impl",
)
include(
  ":ui:theming:api",
  ":ui:theming:impl",
  ":ui:theming:ui",
  ":ui:theming:test",
)
include(":scripts:app")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
