package app.campfire.core

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

abstract class ActivityResultFlowLauncher<Input, Output>(
  private val contract: ActivityResultContract<Input, Output>,
) : ActivityResultCallback<Output>, ComponentActivityPlugin {

  private var resultFlow = MutableStateFlow<Output?>(null)

  private var launcher: ActivityResultLauncher<Input>? = null

  override fun onActivityResult(result: Output) {
    resultFlow.value = result
  }

  @MainThread
  override fun register(activity: ComponentActivity) {
    launcher = activity.registerForActivityResult(
      contract = contract,
      callback = this,
    )
  }

  @MainThread
  override fun unregister() {
    launcher?.unregister()
    launcher = null
  }

  suspend fun launch(input: Input, options: ActivityOptionsCompat? = null): Result<Output> {
    if (launcher == null) return Result.failure(IllegalStateException("Launcher not registered"))
    launcher!!.launch(input, options)

    try {
      val result = resultFlow.filterNotNull().first()
      return Result.success(result)
    } catch (e: Exception) {
      return Result.failure(e)
    } finally {
      resultFlow.value = null
    }
  }
}
