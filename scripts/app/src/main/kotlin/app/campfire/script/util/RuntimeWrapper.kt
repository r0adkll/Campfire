package app.campfire.script.util

import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext

class ProcessResult<Output>(
  val code: Int,
  val output: Output,
) {
  val isSuccess: Boolean get() = code == 0
  val isFailure: Boolean get() = !isSuccess

  inline fun onSuccess(block: ProcessResult<Output>.() -> Unit): ProcessResult<Output> = apply {
    if (isSuccess) block()
  }

  inline fun onFailure(block: ProcessResult<Output>.() -> Unit): ProcessResult<Output> = apply {
    if (isFailure) block()
  }
}

suspend fun <Output> runtime(
  vararg commands: String,
  block: suspend ProcessScope.() -> Output,
): ProcessResult<Output> = withContext(Dispatchers.IO) {
  val process = Runtime.getRuntime().exec(commands)
  val output = ProcessScopeImpl(process).block()
  ProcessResult(process.waitFor(), output)
}

interface ProcessScope {
  val process: Process

  suspend fun onInput(block: suspend (String) -> Unit)
  suspend fun onError(block: suspend (String) -> Unit)
}

private class ProcessScopeImpl(
  override val process: Process,
) : ProcessScope {

  override suspend fun onInput(block: suspend (String) -> Unit) {
    process.inputStream
      .bufferedReader()
      .lineSequence()
      .asFlow()
      .collect { line -> block(line) }
  }

  override suspend fun onError(block: suspend (String) -> Unit) {
    process.errorStream
      .bufferedReader()
      .lineSequence()
      .asFlow()
      .collect { line -> block(line) }
  }
}

val Process.stdin: OutputStream get() = outputStream
val Process.stdout: InputStream get() = inputStream
val Process.stderr: InputStream get() = errorStream
