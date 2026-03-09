package app.campfire.core.app

data class ApplicationUrls(
  val privacyPolicy: String =
    "https://raw.githubusercontent.com/r0adkll/Campfire/refs/heads/main/docs/privacy_policy.md",
  val termsOfService: String =
    "https://raw.githubusercontent.com/r0adkll/Campfire/refs/heads/main/docs/terms_conditions.md",
  val githubDiscussion: String = "https://github.com/r0adkll/Campfire/discussions",
  val developerHomepage: String = "https://github.com/r0adkll",
)
