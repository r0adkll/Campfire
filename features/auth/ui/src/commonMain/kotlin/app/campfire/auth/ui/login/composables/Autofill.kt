package app.campfire.auth.ui.login.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Autofill(
  autofillTypes: List<AutofillType>,
  onFill: ((String) -> Unit),
  content: @Composable BoxScope.() -> Unit,
) {
  val autofill = LocalAutofill.current
  val autofillTree = LocalAutofillTree.current
  val autofillNode =
    remember(autofillTypes, onFill) {
      AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    }

  Box(
      modifier =
      Modifier.onFocusChanged {
          if (it.isFocused) {
              autofill?.requestAutofillForNode(autofillNode)
          } else {
              autofill?.cancelAutofillForNode(autofillNode)
          }
      }
          .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() },
      content = content,
  )

  DisposableEffect(autofillNode) {
    autofillTree.children[autofillNode.id] = autofillNode
    onDispose { autofillTree.children.remove(autofillNode.id) }
  }
}
