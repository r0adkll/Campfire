import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.metro)
  alias(libs.plugins.about.libraries)
}

metro {
  debug.set(true)
  reportsDestination.set(layout.buildDirectory.dir("metro/reports"))
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.app.common)

        implementation(libs.circuit.codegen.annotations)

        implementation(compose.components.resources)
      }
    }

    iosMain {
      commonMain {
        dependencies {
          implementation(projects.app.common)
        }
      }
    }
  }
}

ksp {
  arg("circuit.codegen.mode", "metro")
}

addKspDependencyForAllTargets(libs.circuit.codegen)

aboutLibraries {
  android.registerAndroidTasks = false
  export.prettyPrint = true
}
