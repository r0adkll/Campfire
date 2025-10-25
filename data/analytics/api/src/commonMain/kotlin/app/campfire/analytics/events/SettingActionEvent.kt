package app.campfire.analytics.events

@Suppress("FunctionName")
fun SettingActionEvent(
  obj: String,
  verb: Verb,
  noun: Any? = null,
) = ActionEvent(
  obj = obj,
  verb = verb,
  noun = noun,
  extras = mapOf(
    "source" to "settings",
  ),
)
