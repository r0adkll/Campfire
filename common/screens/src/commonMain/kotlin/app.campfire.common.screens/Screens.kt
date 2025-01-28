package app.campfire.common.screens

import app.campfire.core.model.AuthorId
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.SeriesId
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen

//region App Screens

/**
 * This is a dummy screen to fill a blank detail side content since the backstack/navigator requires a
 * non-empty state to be initialized. This will just render a blank screen but will be used to automatically
 * show/hide the side detail content pain
 */
@Parcelize
data object RootScreen : BaseScreen(name = "Root")

@Parcelize
data class EmptyScreen(
  val message: String,
) : StaticScreen

@Parcelize
data object WelcomeScreen : BaseScreen(name = "Welcome()") {
  override val presentation: Presentation
    get() = Presentation(hideBottomNav = true)
}

@Parcelize
data class LoginScreen(
  val isAddingAccount: Boolean = false,
) : BaseScreen(name = "Login()") {
  override val arguments = mapOf(
    "isAddingAccount" to isAddingAccount,
  )

  override val presentation: Presentation
    get() = Presentation(hideBottomNav = !isAddingAccount)
}

@Parcelize
data object HomeScreen : BaseScreen(name = "Home()")

@Parcelize
data object DrawerScreen : StaticScreen

@Parcelize
data object LibraryScreen : BaseScreen(name = "Library()")

@Parcelize
data class LibraryItemScreen(
  val libraryItemId: LibraryItemId,
) : DetailScreen(name = "LibraryItem()")

@Parcelize
data object SeriesScreen : BaseScreen(name = "Series()")

@Parcelize
data class SeriesDetailScreen(
  val seriesId: SeriesId,
  val seriesName: String,
) : DetailScreen(name = "SeriesDetail()")

@Parcelize
data object CollectionsScreen : BaseScreen(name = "Collections()")

@Parcelize
data class CollectionDetailScreen(
  val collectionId: CollectionId,
  val collectionName: String,
) : DetailScreen(name = "CollectionDetail()")

@Parcelize
data object AuthorsScreen : BaseScreen(name = "Authors()")

@Parcelize
data class AuthorDetailScreen(
  val authorId: AuthorId,
  val authorName: String,
) : DetailScreen(name = "AuthorDetailScreen()")

@Parcelize
data object StatisticsScreen : BaseScreen(name = "Statistics()")

@Parcelize
data object StorageScreen : BaseScreen(name = "Storage()")

@Parcelize
data class SettingsScreen(
  val page: Page = Page.Root,
) : BaseScreen(name = "Settings($page)") {

  override val arguments get() = mapOf("page" to page)

  enum class Page {
    Root,
    Account,
    Appearance,
    Playback,
    Sleep,
    About,
  }
}

@Parcelize
data object AttributionScreen : BaseScreen(name = "Attributions()")

//endregion

//region Utility Screens

@Parcelize
data class UrlScreen(val url: String) : BaseScreen(name = "UrlScreen()") {
  override val arguments get() = mapOf("url" to url)
}

//endregion

/**
 * Screens that implement this will display in full screen on phones,
 * or in the supporting pane on larger devices like desktop, tablets and foldables
 */
abstract class DetailScreen(name: String) : BaseScreen(name) {
//  override val presentation: Presentation
//    get() = Presentation.Fullscreen
}

/**
 * The Root screen class that all other screen definitions will use as the underlying screen
 * data type
 */
abstract class BaseScreen(val name: String) : Screen {
  open val arguments: Map<String, *>? = null
  open val presentation: Presentation = Presentation()
}

/**
 * The type of presentation that a screen is. Dictating whether or not to hide the bottom nav (if available)
 * or whether this screen can be rendered as a detail screen
 */
data class Presentation(
  val hideBottomNav: Boolean = false,
  val isDetailScreen: Boolean = false,
) {
  companion object {
    val Fullscreen: Presentation get() = Presentation(hideBottomNav = true)
  }
}
