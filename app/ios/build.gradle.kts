import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
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

        implementation(libs.kimchi.annotations)
        implementation(libs.kotlininject.runtime)

        implementation(compose.components.resources)
      }
    }
  }
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

addKspDependencyForAllTargets(libs.kotlininject.ksp)
addKspDependencyForAllTargets(libs.kimchi.compiler)

aboutLibraries {
  android.registerAndroidTasks = false
  export.prettyPrint = true
}
