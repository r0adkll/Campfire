package app.campfire.filters

import app.campfire.core.filter.ContentFilter
import com.slack.circuit.overlay.OverlayHost
import kotlin.reflect.KClass

interface ContentFilterUi {

  val libraryItemFilterCategories: AllowedFilterCategories

  val seriesFilterCategories: AllowedFilterCategories

  suspend fun showContentFilterBottomSheet(
    overlayHost: OverlayHost,
    current: ContentFilter? = null,
    allowedCategories: AllowedFilterCategories? = null,
  ): ContentFilterResult
}

sealed interface ContentFilterResult {
  data object None : ContentFilterResult
  data class Selected(val filter: ContentFilter?) : ContentFilterResult
}

fun interface AllowedFilterCategories {
  fun isAllowed(clazz: KClass<*>): Boolean
}
