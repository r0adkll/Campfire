package app.campfire.audioplayer.impl.content

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

internal const val COVER_AUTHORITY_SUFFIX = ".covers"
internal const val PATH_ITEMS = "items"
internal const val PATH_AUTHORS = "authors"

fun coverContentUriForItem(context: Context, libraryItemId: String): Uri {
  return "content://${context.packageName}$COVER_AUTHORITY_SUFFIX/$PATH_ITEMS/$libraryItemId".toUri()
}

fun coverContentUriForAuthor(context: Context, authorId: String): Uri {
  return "content://${context.packageName}$COVER_AUTHORITY_SUFFIX/$PATH_AUTHORS/$authorId".toUri()
}
