package app.campfire.auth.ui.login.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.NetworkSettings
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import org.jetbrains.compose.ui.tooling.preview.Preview

data class NetworkSettingsModel(val settings: NetworkSettings?)

sealed interface NetworkSettingsResult {
  data object None : NetworkSettingsResult
  data class Success(
    val settings: NetworkSettings,
  ) : NetworkSettingsResult
}

suspend fun OverlayHost.showNetworkSettingsBottomSheet(
  currentSettings: NetworkSettings?,
): NetworkSettingsResult {
  return show(
    BottomSheetOverlay<NetworkSettingsModel, NetworkSettingsResult>(
      model = NetworkSettingsModel(currentSettings),
      onDismiss = { NetworkSettingsResult.None },
      skipPartiallyExpandedState = true,
    ) { model, navigator ->
      NetworkSettingsBottomSheet(
        currentSettings = model.settings,
        onDismiss = { navigator.finish(it) },
      )
    },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkSettingsBottomSheet(
  currentSettings: NetworkSettings?,
  onDismiss: (NetworkSettingsResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  val headers = remember {
    mutableStateMapOf(
      *(
        currentSettings?.extraHeaders
          ?.map { (k, v) -> k to v }
          ?.toTypedArray()
          ?: emptyArray<Pair<String, String>>()
        ),
    )
  }

  val hasChanges by remember {
    derivedStateOf {
      headers.toMap() != currentSettings?.extraHeaders
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
  ) {
    Text(
      text = "Headers",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    )

    headers.forEach { (name, value) ->
      HeaderValue(
        name = name,
        value = value,
        onDeleteClick = {
          headers.remove(name)
        },
      )
    }

    if (headers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(88.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No extra headers",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Spacer(Modifier.size(8.dp))

    Text(
      text = "Add header",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    )

    val newHeaderName = rememberTextFieldState()
    val newHeaderValue = rememberTextFieldState()

    Row(
      modifier = Modifier,
    ) {
      Spacer(Modifier.size(16.dp))

      Column(
        modifier = Modifier.weight(1f),
      ) {
        OutlinedTextField(
          state = newHeaderName,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Name") },
        )
        OutlinedTextField(
          state = newHeaderValue,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Value") },
        )
      }

      Spacer(Modifier.size(8.dp))

      FilledIconButton(
        shapes = IconButtonDefaults.shapes(),
        enabled = newHeaderName.text.isNotBlank() &&
          newHeaderValue.text.isNotBlank(),
        onClick = {
          headers[newHeaderName.text.toString()] = newHeaderValue.text.toString()
          newHeaderName.clearText()
          newHeaderValue.clearText()
        },
        modifier = Modifier.padding(top = 8.dp),
      ) {
        Icon(
          Icons.Rounded.Add,
          contentDescription = "Add Extra Header",
        )
      }

      Spacer(Modifier.size(8.dp))
    }

    Spacer(Modifier.size(16.dp))

    val acceptButtonSize = ButtonDefaults.MinHeight
    Button(
      enabled = hasChanges,
      onClick = {
        onDismiss(
          NetworkSettingsResult.Success(
            NetworkSettings(headers),
          ),
        )
      },
      shapes = ButtonDefaults.shapes(),
      contentPadding = ButtonDefaults.contentPaddingFor(acceptButtonSize),
      modifier = Modifier
        .heightIn(acceptButtonSize)
        .padding(horizontal = 16.dp)
        .align(Alignment.End),
    ) {
      Icon(
        Icons.Rounded.Save,
        contentDescription = "Save Settings",
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(acceptButtonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(acceptButtonSize)))
      Text(
        text = "Accept",
        style = ButtonDefaults.textStyleFor(acceptButtonSize),
      )
    }

    Spacer(Modifier.size(16.dp))
  }
}

private const val NAME_WEIGHT = 2f
private const val VALUE_WEIGHT = 3f

@Composable
private fun HeaderValue(
  name: String,
  value: String,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 8.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(Modifier.size(8.dp))

    Text(
      text = name,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.weight(NAME_WEIGHT),
      maxLines = 1,
      overflow = TextOverflow.MiddleEllipsis,
    )

    Spacer(Modifier.size(16.dp))

    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.weight(VALUE_WEIGHT),
      maxLines = 1,
      overflow = TextOverflow.MiddleEllipsis,
    )

    IconButton(
      onClick = onDeleteClick,
    ) {
      Icon(
        Icons.Rounded.Delete,
        contentDescription = "Delete Extra Header",
        tint = MaterialTheme.colorScheme.error,
      )
    }
  }
}

@Preview
@Composable
private fun NetworkSettingsBottomSheetPreview() {
  CampfireTheme {
    ModalBottomSheet(
      onDismissRequest = {},
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      NetworkSettingsBottomSheet(
        currentSettings = NetworkSettings(
          mapOf(
            "CF-Access-Client-Id" to "abcdefghijklmnopqrstuvwxyz",
            "CF-Access-Client-Secret" to "abcdefghijklmnopqrstuvwxyz",
          ),
        ),
        onDismiss = {},
      )
    }
  }
}
