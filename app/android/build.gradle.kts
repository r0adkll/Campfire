@file:Suppress("UnstableApiUsage")

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor


plugins {
  id("app.campfire.android.application")
  id("app.campfire.kotlin.android")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.about.libraries)
  alias(libs.plugins.baselineprofile)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

ksp {
  arg("me.tatarka.inject.generateCompanionExtensions", "true")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
  }
}

android {
  namespace = "app.campfire.android"

  defaultConfig {
    applicationId = "app.campfire.android"
    versionCode = properties["CAMPFIRE_VERSIONCODE"]?.toString()?.toIntOrNull() ?: 999999999
    versionName = properties["CAMPFIRE_VERSIONNAME"]?.toString() ?: "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources.excludes += setOf(
      // Exclude AndroidX version files
      "META-INF/*.version",
      // Exclude consumer proguard files
      "META-INF/proguard/*",
      // Exclude the Firebase/Fabric/other random properties files
      "/*.properties",
      "fabric/*.properties",
      "META-INF/*.properties",
      // License files
      "LICENSE*",
      // Exclude Kotlin unused files
      "META-INF/**/previous-compilation-data.bin",
    )
  }

  signingConfigs {
    getByName("debug") {
      storeFile = rootProject.file("app/signing/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }

    if (rootProject.file("app/signing/campfire.keystore").exists()) {
      create("release") {
        storeFile = file("../signing/campfire.keystore")
        storePassword = properties["CAMPFIRE_KEYSTORE_PWD"]?.toString().orEmpty()
        keyAlias = "audiobooks"
        keyPassword = properties["CAMPFIRE_KEY_PWD"]?.toString().orEmpty()
      }
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs["debug"]
      versionNameSuffix = "-dev"
    }

    getByName("release") {
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }

    create("nonMinifiedRelease") {
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs["debug"]
    }
  }
}

aboutLibraries {
  android.registerAndroidTasks = false
  export.prettyPrint = true
}

dependencies {
  implementation(platform(libs.google.firebase.bom))
  implementation(libs.google.firebase.analytics)
  implementation(libs.google.firebase.crashlytics)

  implementation(projects.app.common)
  implementation(projects.common.screens)

  implementation(libs.about.libraries.core)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.browser)

  implementation(libs.circuit.runtime)
  implementation(libs.circuit.foundation)
  implementation(libs.androidx.profileinstaller)

  baselineProfile(projects.app.baselineprofile)

  debugImplementation(projects.infra.debug)

  ksp(libs.kimchi.compiler)
  ksp(libs.kotlininject.ksp)
}

// Temporary workaround for ktor and R8
// https://youtrack.jetbrains.com/issue/KTOR-8583/Space-characters-in-SimpleName-error-when-executing-R8-mergeExtDex-task-with-3.2.0
// TODO: Remove this in v3.2.1+
class FieldSkippingClassVisitor(
  apiVersion: Int,
  nextClassVisitor: ClassVisitor,
) : ClassVisitor(apiVersion, nextClassVisitor) {

  // Returning null from this method will cause the ClassVisitor to strip all fields from the class.
  override fun visitField(
    access: Int,
    name: String?,
    descriptor: String?,
    signature: String?,
    value: Any?,
  ): FieldVisitor? = null

  abstract class Factory : AsmClassVisitorFactory<Parameters> {

    private val excludedClasses
      get() = parameters.get().classes.get()

    override fun isInstrumentable(classData: ClassData): Boolean =
      classData.className in excludedClasses

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
      return FieldSkippingClassVisitor(
        apiVersion = instrumentationContext.apiVersion.get(),
        nextClassVisitor = nextClassVisitor,
      )
    }
  }

  abstract class Parameters : InstrumentationParameters {
    @get:Input
    abstract val classes: SetProperty<String>
  }
}

androidComponents {
  onVariants { variant ->
    variant.instrumentation.transformClassesWith(
      FieldSkippingClassVisitor.Factory::class.java,
      scope = InstrumentationScope.ALL,
    ) { params ->
      params.classes.add("io.ktor.client.plugins.Messages")
    }
  }
}
