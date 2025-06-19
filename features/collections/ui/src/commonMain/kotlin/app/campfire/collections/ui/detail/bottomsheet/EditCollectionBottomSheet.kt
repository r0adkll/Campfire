package app.campfire.collections.ui.detail.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.compose.di.rememberComponent
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.Collection
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.edit_collection_bottomsheet_action_create
import campfire.features.collections.ui.generated.resources.edit_collection_bottomsheet_action_creating
import campfire.features.collections.ui.generated.resources.edit_collection_bottomsheet_input_description_label
import campfire.features.collections.ui.generated.resources.edit_collection_bottomsheet_input_title_label
import campfire.features.collections.ui.generated.resources.edit_collection_bottomsheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface EditCollectionResult {
  data object None : EditCollectionResult
  data object Edited : EditCollectionResult
}

@ContributesTo(UserScope::class)
interface EditCollectionBottomSheetComponent {
  val collectionsRepository: CollectionsRepository
}

suspend fun OverlayHost.showEditCollectionBottomSheet(
  collection: Collection,
): EditCollectionResult {
  return show(
    BottomSheetOverlay<Collection, EditCollectionResult>(
      model = collection,
      onDismiss = { EditCollectionResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { c, overlayNavigator ->
      SheetScaffold(
        title = { Text(stringResource(Res.string.edit_collection_bottomsheet_title)) },
      ) {
        EditCollectionBottomSheet(
          collection = c,
          onCollectionEdited = {
            overlayNavigator.finish(EditCollectionResult.Edited)
          },
        )
        Spacer(Modifier.height(16.dp))

        Spacer(
          Modifier
            .navigationBarsPadding()
            .imePadding(),
        )
      }
    },
  )
}

@Composable
internal fun SheetScaffold(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
  ) {
    Box(
      Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
        ),
      ) {
        title()
      }
    }

    content()
  }
}

@Composable
private fun EditCollectionBottomSheet(
  collection: Collection,
  onCollectionEdited: () -> Unit,
  modifier: Modifier = Modifier,
  component: EditCollectionBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()
  var isCreating by remember { mutableStateOf(false) }

  var name by remember { mutableStateOf(TextFieldValue(collection.name)) }
  var description by remember { mutableStateOf(TextFieldValue(collection.description ?: "")) }
  val isValid = name.text.isNotBlank()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
      ),
  ) {
    OutlinedTextField(
      enabled = !isCreating,
      value = name,
      onValueChange = { name = it },
      label = { Text(stringResource(Res.string.edit_collection_bottomsheet_input_title_label)) },
      modifier = Modifier
        .fillMaxWidth(),
    )

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
      enabled = !isCreating,
      value = description,
      onValueChange = { description = it },
      label = { Text(stringResource(Res.string.edit_collection_bottomsheet_input_description_label)) },
      modifier = Modifier
        .fillMaxWidth(),
    )

    Spacer(Modifier.height(24.dp))

    Button(
      onClick = {
        isCreating = true
        scope.launch {
          try {
            component.collectionsRepository
              .updateCollection(
                collectionId = collection.id,
                name = name.text.trim().takeIf { it.isNotBlank() },
                description = description.text.trim().takeIf { it.isNotBlank() },
              )
            onCollectionEdited()
          } catch (e: Throwable) {
            bark(throwable = e) { "Failed to edit collection" }
          } finally {
            isCreating = false
          }
        }
      },
      enabled = isValid && !isCreating,
      contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (isCreating) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
      } else {
        Icon(Icons.Rounded.Save, contentDescription = null)
      }
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      if (isCreating) {
        Text(stringResource(Res.string.edit_collection_bottomsheet_action_creating))
      } else {
        Text(stringResource(Res.string.edit_collection_bottomsheet_action_create))
      }
    }
  }
}
