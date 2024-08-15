dependencyResolutionManagement {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
      name = "snapshots-maven-central"
    }

    google()
    mavenCentral()

    // Prerelease versions of Compose Multiplatform
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }

  versionCatalogs {
    create("libs") {
      from(files("../libs.versions.toml"))
    }
  }
}

buildCache {
  val isCi = System.getenv().containsKey("CI")
  local {
    isEnabled = !isCi
  }
}

rootProject.name = "build-logic"
include(":convention")
