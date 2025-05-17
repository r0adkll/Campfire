import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.metro)
  alias(libs.plugins.about.libraries)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.app.common)

//        implementation(libs.kimchi.annotations)
//        implementation(libs.kotlininject.runtime)
        implementation(libs.circuit.codegen.annotations)

        implementation(compose.components.resources)
      }
    }
  }
}

ksp {
  // arg("me.tatarka.inject.generateCompanionExtensions", "true")
  arg("circuit.codegen.mode", "metro")
}

// addKspDependencyForAllTargets(libs.kotlininject.ksp)
// addKspDependencyForAllTargets(libs.kimchi.compiler)
addKspDependencyForAllTargets(libs.circuit.codegen)

aboutLibraries {
  registerAndroidTasks = false
  prettyPrint = true
}
