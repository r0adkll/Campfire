package app.campfire.network.oidc

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import app.campfire.core.ActivityResultFlowLauncher
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class StartActivityForResultFlowLauncher : ActivityResultFlowLauncher<Intent, ActivityResult>(
  ActivityResultContracts.StartActivityForResult(),
)
