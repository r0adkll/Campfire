package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.campfire.common.compose.extensions.fromHexCodeOrNull
import app.campfire.common.compose.extensions.toHexString
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColorPicker(
  color: Color,
  onColorChange: (Color) -> Unit,
  title: String,
  modifier: Modifier = Modifier,
  description: String? = null,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
  contentPadding: PaddingValues = PaddingValues(16.dp),
  colorSize: Dp = 56.dp,
  shadowElevation: Dp = 1.dp,
) {
  var showColorPickerDialog by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    contentColor = contentColor,
    shadowElevation = shadowElevation,
    onClick = {
      showColorPickerDialog = true
    },
  ) {
    Row(
      modifier = Modifier
        .padding(contentPadding),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier
          .size(colorSize)
          .clip(CircleShape)
          .background(color)
          .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
      )

      Spacer(Modifier.size(16.dp))

      Column(
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = title,
          style = if (description == null) {
            MaterialTheme.typography.titleLargeEmphasized
          } else {
            MaterialTheme.typography.titleMediumEmphasized
          },
        )
        description?.let { desc ->
          Text(
            text = desc,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }

  if (showColorPickerDialog) {
    ColorPickerDialog(
      initialColor = color,
      onColorChange = onColorChange,
      onDismissRequest = {
        showColorPickerDialog = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPickerDialog(
  initialColor: Color?,
  onColorChange: (Color) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorController = rememberColorPickerController()
  val hexColorTextFieldState = rememberTextFieldState(
    initialText = initialColor?.toHexString(includePrefix = false) ?: "",
  )

  LaunchedEffect(hexColorTextFieldState.text) {
    hexColorTextFieldState.text.toString()
      .takeIf { it.length == 6 || it.length == 8 }
      ?.let { if (it.length == 6) "ff$it" else it }
      ?.fromHexCodeOrNull()
      ?.let { color ->
        if (color != colorController.selectedColor.value) {
          colorController.selectByColor(color, false)
        }
      }
  }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false,
    ),
    confirmButton = {
      TextButton(
        onClick = {
          onColorChange(colorController.selectedColor.value)
          onDismissRequest()
        },
      ) {
        Text("Select")
      }
    },
    title = {
      Text(
        text = "Choose a color",
      )
    },
    text = {
      Column {
        HsvColorPicker(
          controller = colorController,
          initialColor = initialColor,
          onColorChanged = {
            if (it.fromUser) {
              hexColorTextFieldState.edit {
                replace(0, length, it.hexCode.drop(2))
              }
            }
          },
          modifier = Modifier
            .padding(16.dp)
            .height(350.dp),
        )

        Spacer(Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
          Box(
            modifier = Modifier
              .padding(top = 8.dp)
              .clip(MaterialTheme.shapes.large)
              .background(colorController.selectedColor.value)
              .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
              .size(56.dp),
          )

          Spacer(Modifier.size(16.dp))

          OutlinedTextField(
            state = hexColorTextFieldState,
            prefix = { Text("#") },
            label = { Text("Hex code") },
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = InputTransformation.maxLength(6),
            textStyle = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier,
          )
        }
      }
    },
    modifier = modifier
      .padding(horizontal = 24.dp),
  )
}
