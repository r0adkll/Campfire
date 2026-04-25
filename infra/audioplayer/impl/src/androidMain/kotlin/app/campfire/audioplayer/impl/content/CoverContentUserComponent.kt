package app.campfire.audioplayer.impl.content

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface CoverContentUserComponent {
  val urlHydrator: UrlHydrator
}
