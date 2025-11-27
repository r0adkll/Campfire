package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.campfire.core.extensions.fluentIf
import app.campfire.core.logging.bark
import app.campfire.core.model.Author
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.filter_bar_book_count
import campfire.common.compose.generated.resources.placeholder_person
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

data class AuthorSharedTransitionKey(
  val id: String,
  val type: ElementType,
) {
  enum class ElementType {
    Image,
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorCard(
  author: Author,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  ElevatedContentCard(
    modifier = modifier,
    onClick = onClick,
  ) {
    Box(
      modifier = modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .widthIn(max = CardMaxWidth)
        .clip(RoundedCornerShape(ThumbnailCornerSize)),
    ) {
      val placeHolderResource = remember {
        Res.drawable.placeholder_person
      }

      val painter = rememberAsyncImagePainter(
        model = author.imagePath,
        placeholder = painterResource(placeHolderResource),
        error = painterResource(placeHolderResource),
        onError = {
          bark(throwable = it.result.throwable) { "Author image loading error" }
        },
      )

      val imageState by painter.state.collectAsState()
      when (imageState) {
        is AsyncImagePainter.State.Loading -> ImageLoading()
        // Do nothing in the other states
        else -> Unit
      }

      Image(
        painter,
        contentDescription = author.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fluentIf<Modifier>(findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation) != null) {
            sharedElement(
              sharedContentState = rememberSharedContentState(
                AuthorSharedTransitionKey(
                  id = author.id,
                  type = AuthorSharedTransitionKey.ElementType.Image,
                ),
              ),
              animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
          }
          .fillMaxSize()
          .clip(MaterialTheme.shapes.largeIncreased),
      )
    }
    Column(
      Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = author.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
      )
      author.numBooks?.let {
        Text(
          text = pluralStringResource(Res.plurals.filter_bar_book_count, it, it),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun PlaceholderImage(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize(),
  ) {
    val placeHolderResource = remember {
      Res.drawable.placeholder_person
    }
    Image(
      painterResource(placeHolderResource),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
}
