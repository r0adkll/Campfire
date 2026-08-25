// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

kotlin {
  sourceSets {
    androidMain {
      dependencies {
        implementation(projects.infra.audioplayer.api)
        implementation(projects.infra.audioplayer.impl)
        implementation(projects.core)

        // `api` because MediaRouterCastController's supertypes (CastStateListener,
        // MediaRouter.Callback) must be visible to the kimchi-generated component in the
        // consuming app module. mediarouter is also declared explicitly rather than
        // relying on it arriving transitively through the cast framework.
        api(libs.androidx.mediarouter)
        api(libs.play.services.cast.framework)
        implementation(libs.androidx.lifecycle.process)
        implementation(libs.media3.cast)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
